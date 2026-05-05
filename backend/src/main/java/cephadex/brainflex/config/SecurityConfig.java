package cephadex.brainflex.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import cephadex.brainflex.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public GrantedAuthoritiesMapper oauthUserAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            mapped.add(new SimpleGrantedAuthority("ROLE_USER"));
            return mapped;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/**/api-docs").permitAll()
                        .requestMatchers("/api/users/leaderboard/**").permitAll()
                        .requestMatchers("/api/users/check-username").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/me", "/api/auth/guest").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/content-packs/**").permitAll()
                        // Guests and registered users can join/leave/cancel
                        .requestMatchers(HttpMethod.POST, "/api/games/*/join").hasAnyRole("GUEST", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/games/**").hasAnyRole("GUEST", "USER")
                        // Creating games is registered-only
                        .requestMatchers(HttpMethod.POST, "/api/games").hasRole("USER")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        }))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(oauthUserAuthoritiesMapper()))
                        .successHandler(new OAuth2SuccessHandler(userRepository)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        private final UserRepository userRepository;

        public OAuth2SuccessHandler(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException {

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String googleId = oAuth2User.getAttribute("sub");
            String sessionReturnUrl = null;
            String guestId = null;
            if (request.getSession(false) != null) {
                sessionReturnUrl = (String) request.getSession(false).getAttribute("returnUrl");
                guestId = (String) request.getSession(false).getAttribute("guestId");
                request.getSession(false).removeAttribute("returnUrl");
                request.getSession(false).removeAttribute("guestId");
            }

            boolean userExists = userRepository.findByGoogleId(googleId)
                    .filter(u -> !Boolean.TRUE.equals(u.getIsClosed()))
                    .isPresent();

            if (userExists) {
                String redirectUrl = sessionReturnUrl != null ? sessionReturnUrl : "http://localhost:5173/";
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            if (guestId != null && !guestId.isBlank()) {
                userRepository.findById(guestId).ifPresent(guestUser -> {
                    if (guestUser.getIsGuest()) {
                        guestUser.setIsGuest(false);
                        guestUser.setGoogleId(googleId);
                        guestUser.setEmail(oAuth2User.getAttribute("email"));
                        guestUser.setName(oAuth2User.getAttribute("name"));
                        guestUser.setPictureUrl(oAuth2User.getAttribute("picture"));
                        userRepository.save(guestUser);
                    }
                });
                String redirectUrl = sessionReturnUrl != null ? sessionReturnUrl : "http://localhost:5173/";
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/register")
                    .queryParam("googleId", googleId)
                    .queryParam("email", email)
                    .queryParam("name", name)
                    .queryParam("picture", picture)
                    .build().toUriString();
            if (sessionReturnUrl != null) {
                targetUrl += "&returnUrl=" + URLEncoder.encode(sessionReturnUrl, StandardCharsets.UTF_8);
            }
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
