package cephadex.brainflex.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AuthoritiesService;
import cephadex.brainflex.web.MdcLoggingFilter;
import cephadex.brainflex.service.OAuthProviderService;
import cephadex.brainflex.service.OAuthProviderService.ProviderProfile;
import cephadex.brainflex.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
@Profile("!test")
public class SecurityConfig {

    private final UserRepository userRepository;
    private final AuthoritiesService authoritiesService;
    private final OAuthProviderService oAuthProviderService;
    private final OrganizationService organizationService;

    public SecurityConfig(UserRepository userRepository,
            AuthoritiesService authoritiesService,
            OAuthProviderService oAuthProviderService,
            OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.authoritiesService = authoritiesService;
        this.oAuthProviderService = oAuthProviderService;
        this.organizationService = organizationService;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Role implications applied to both web and method security. Picked up
     * automatically by @EnableMethodSecurity in Spring Security 6.3+, so
     * @PreAuthorize("hasRole('USER')") accepts any USER_* role and
     * @PreAuthorize("hasRole('ORG_MEMBER')") also accepts ORG_OWNER.
     *
     * ADMIN implies MODERATOR implies the rest of the user ladder, so any
     * admin can satisfy a hasRole('MODERATOR') or hasRole('USER') check.
     */
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

    /**
     * Maps the raw OAuth2 authorities into the full BrainFlex authority set by
     * delegating to AuthoritiesService when the user is already registered.
     * For brand-new users the User record does not exist yet (registration
     * happens after the OAuth redirect), so we fall back to a plain ROLE_USER
     * and the tier/org roles are filled in on the next login.
     *
     * The {@link OAuth2AuthenticationToken} that carries the registration id
     * hasn't been built yet at this stage — Spring is still assembling
     * authorities from raw user-info attributes. We probe each provider's
     * id attribute ({@code sub} for Google/Microsoft OIDC, {@code id} for
     * Discord) and then scan every provider column for a match.
     */
    @Bean
    public GrantedAuthoritiesMapper oauthUserAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            String providerId = null;
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OAuth2UserAuthority oauthAuth) {
                    var attrs = oauthAuth.getAttributes();
                    Object sub = attrs.get("sub");
                    Object discordId = attrs.get("id");
                    Object candidate = sub != null ? sub : discordId;
                    if (candidate != null) {
                        providerId = candidate.toString();
                        break;
                    }
                }
            }
            if (providerId != null) {
                oAuthProviderService.findByAnyProviderId(providerId)
                        .filter(u -> !u.isClosed())
                        .ifPresent(u -> mapped.addAll(authoritiesService.authoritiesFor(u)));
            }
            mapped.add(new SimpleGrantedAuthority(AuthoritiesService.ROLE_USER));
            return mapped;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
                // Runs after authorization so the authenticated principal is
                // resolved — it enriches the MDC with traceId + userId for every
                // request that reaches a controller. See MdcLoggingFilter.
                .addFilterAfter(new MdcLoggingFilter(), AuthorizationFilter.class)
                // Per-endpoint role rules live with the controller methods as
                // @PreAuthorize annotations. SecurityConfig only decides what
                // is public vs. what requires *any* authentication.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health",
                                "/api/public/**",
                                "/swagger-ui/**", "/**/api-docs",
                                "/oauth2/**", "/ws/**")
                        .permitAll()
                        .requestMatchers("/api/auth/login",
                                "/api/auth/me",
                                "/api/auth/guest")
                        .permitAll()
                        // Invite token is the credential; endpoint is public.
                        .requestMatchers(HttpMethod.POST, "/api/invites/*/redeem").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/leaderboard/**",
                                "/api/users/check-username",
                                "/api/users/*/achievements",
                                "/api/interactive-sessions/**",
                                "/api/decks/**",
                                "/api/collections/*",
                                "/api/achievements")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        }))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(oauthUserAuthoritiesMapper()))
                        .successHandler(
                                new OAuth2SuccessHandler(userRepository, oAuthProviderService, organizationService)))
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
        // Let the browser read back the request-correlation id the MdcLoggingFilter
        // echoes, so the frontend can show/log the traceId it shares with the server.
        configuration.setExposedHeaders(List.of(MdcLoggingFilter.REQUEST_ID_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        private final UserRepository userRepository;
        private final OAuthProviderService oAuthProviderService;
        private final OrganizationService organizationService;

        public OAuth2SuccessHandler(UserRepository userRepository,
                OAuthProviderService oAuthProviderService,
                OrganizationService organizationService) {
            this.userRepository = userRepository;
            this.oAuthProviderService = oAuthProviderService;
            this.organizationService = organizationService;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException {

            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            ProviderProfile profile = oAuthProviderService.profileOf(token);

            String sessionReturnUrl = null;
            String guestId = null;
            if (request.getSession(false) != null) {
                sessionReturnUrl = (String) request.getSession(false).getAttribute("returnUrl");
                guestId = (String) request.getSession(false).getAttribute("guestId");
                request.getSession(false).removeAttribute("returnUrl");
                request.getSession(false).removeAttribute("guestId");
            }

            var existingOpt = oAuthProviderService
                    .findByProviderId(profile.provider(), profile.providerId())
                    .filter(u -> !u.isClosed());
            // Lazy backfill so users created before emailVerifiedAt landed pick it up
            // on their next OAuth login. Reaching this branch means the IdP accepted
            // the credentials — for Google/Microsoft (OIDC) that's a verified-email
            // signal; Discord's OAuth-only flow only succeeds with a confirmed email
            // on the account, which is good enough for our purposes.
            existingOpt.ifPresent(u -> {
                if (u.getEmailVerifiedAt() == null) {
                    u.setEmailVerifiedAt(Instant.now());
                    userRepository.save(u);
                }
                // Chunk 20 — every login is a fresh chance for a domain-claimed
                // org to pick the user up. The service is idempotent so users
                // already in every match pay only a single indexed query.
                organizationService.autoJoinByEmailDomain(u);
            });

            if (existingOpt.isPresent()) {
                String redirectUrl = sessionReturnUrl != null ? sessionReturnUrl : "http://localhost:5173/";
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            if (guestId != null && !guestId.isBlank()) {
                userRepository.findById(guestId).ifPresent(guestUser -> {
                    if (guestUser.isGuest()) {
                        guestUser.setGuest(false);
                        oAuthProviderService.setProviderIdOn(guestUser, profile);
                        guestUser.setEmail(profile.email());
                        guestUser.setName(profile.name());
                        guestUser.setPictureUrl(profile.picture());
                        guestUser.setEmailVerifiedAt(Instant.now());
                        userRepository.save(guestUser);
                        // Chunk 20 — same auto-join sweep as the existing-user
                        // branch above, so a guest converting to a registered
                        // account lands in their email-domain orgs on the spot.
                        organizationService.autoJoinByEmailDomain(guestUser);
                    }
                });
                String redirectUrl = sessionReturnUrl != null ? sessionReturnUrl : "http://localhost:5173/";
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/register")
                    .queryParam("provider", profile.provider())
                    .queryParam("providerId", profile.providerId());
            if (profile.email() != null)
                builder.queryParam("email", profile.email());
            if (profile.name() != null)
                builder.queryParam("name", profile.name());
            if (profile.picture() != null)
                builder.queryParam("picture", profile.picture());

            String targetUrl = builder.build().toUriString();
            if (sessionReturnUrl != null) {
                targetUrl += "&returnUrl=" + URLEncoder.encode(sessionReturnUrl, StandardCharsets.UTF_8);
            }
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
