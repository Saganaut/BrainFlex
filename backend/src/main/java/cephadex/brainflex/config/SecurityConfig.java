package cephadex.brainflex.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api/users/leaderboard/**").permitAll()
                        .requestMatchers("/api/users/check-username").permitAll()
                        .requestMatchers("/**/api-docs").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
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

            boolean userExists = userRepository.findByGoogleId(googleId).isPresent();

            if (userExists) {
                getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/dashboard");
            } else {
                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");
                String picture = oAuth2User.getAttribute("picture");
                String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/register")
                        .queryParam("googleId", googleId)
                        .queryParam("email", email)
                        .queryParam("name", name)
                        .queryParam("picture", picture)
                        .build().toUriString();

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
        }
    }
}
