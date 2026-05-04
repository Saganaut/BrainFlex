package cephadex.brainflex.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("✅ SecurityConfig has been initialized by Spring!");
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api/users/leaderboard/**").permitAll()
                        .requestMatchers("/api/users/check-username").permitAll()
                        .requestMatchers("/**/api-docs").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me").permitAll()
                        .requestMatchers("/api/auth/guest").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        }))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new OAuth2SuccessHandler(userRepository)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        }));

        return http.build();
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
                // User user = userRepository.findByGoogleId(googleId).get();
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
