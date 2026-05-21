package cephadex.brainflex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Test-profile security replacement for the production {@code SecurityConfig}.
 *
 * The production config is gated by {@code @Profile("!test")}, so test contexts
 * run without its filter chain. {@code @EnableMethodSecurity} here is what
 * gives {@code @PreAuthorize} bite at the controller level — chunk 20 moved
 * several admin-only endpoints from in-body {@code adminProperties.isAdmin(...)}
 * checks to declarative {@code hasRole('ADMIN')} gates, and the tests need that
 * gate to fire to assert the 403 path.
 *
 * The role hierarchy mirrors the production one so {@code hasRole('USER')}
 * test mocks (the default of {@code @WithMockUser}) continue to satisfy
 * {@code hasRole('USER')} gates everywhere else.
 */
@Configuration
@EnableMethodSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MODERATOR
                ROLE_MODERATOR > ROLE_USER_PREMIUM
                ROLE_USER_PREMIUM > ROLE_USER_BASIC
                ROLE_USER_BASIC > ROLE_USER_FREE
                ROLE_USER_FREE > ROLE_USER
                ROLE_ORG_OWNER > ROLE_ORG_MEMBER
                """);
    }

    // SecurityConfig is excluded in tests; provide this bean so AuthController can inject it.
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
