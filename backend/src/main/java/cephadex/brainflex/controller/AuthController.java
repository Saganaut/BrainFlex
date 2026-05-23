package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.user.RegisterRequest;
import cephadex.brainflex.dto.user.UserResponse;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AuthoritiesService;
import cephadex.brainflex.service.DeckCollaboratorService;
import cephadex.brainflex.service.OAuthProviderService;
import cephadex.brainflex.service.UserImageHydrator;
import cephadex.brainflex.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Allowlist of OAuth registration ids accepted by {@code /api/auth/login}.
     * Anything else is rejected before we hand off to Spring's OAuth2 filter
     * so a typo (or attacker-supplied) provider name can't drive a redirect to
     * an arbitrary {@code /oauth2/authorization/...} path.
     */
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of(
            OAuthProviderService.GOOGLE,
            OAuthProviderService.DISCORD,
            OAuthProviderService.MICROSOFT);

    private final UserRepository userRepository;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final AuthoritiesService authoritiesService;
    private final UserImageHydrator userImageHydrator;
    private final DeckCollaboratorService deckCollaboratorService;
    private final OAuthProviderService oAuthProviderService;

    public AuthController(UserRepository userRepository, UserService userService,
            SecurityContextRepository securityContextRepository,
            AuthoritiesService authoritiesService,
            UserImageHydrator userImageHydrator,
            DeckCollaboratorService deckCollaboratorService,
            OAuthProviderService oAuthProviderService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
        this.authoritiesService = authoritiesService;
        this.userImageHydrator = userImageHydrator;
        this.deckCollaboratorService = deckCollaboratorService;
        this.oAuthProviderService = oAuthProviderService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            boolean isGuest = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
            boolean isRegistered = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

            if (isGuest) {
                String id = authentication.getName().substring(6);
                return userRepository.findById(id)
                        .map(user -> ResponseEntity.ok((UserResponse) new UserResponse.GuestUser(user,
                                userImageHydrator.pictureImageOf(user))))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserResponse>build());
            } else if (isRegistered) {
                return oAuthProviderService.findByOAuthAuthentication(authentication)
                        .map(user -> ResponseEntity.ok((UserResponse) new UserResponse.RegisteredUser(user,
                                userImageHydrator.pictureImageOf(user))))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserResponse>build());
            }
        }

        // Visitor — no session, not even a guest. Return 204 so the frontend's
        // useCurrentUser maps it to state="visitor" via its data==null branch
        // instead of treating the caller as a fake "id=0" guest.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse.RegisteredUser> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {

        if (!(authentication instanceof OAuth2AuthenticationToken token) || !token.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<UserResponse.RegisteredUser>build();
        }

        User registered = userService.register(token, request);
        // Promote any pending email-based deck-collaborator invites for this
        // address — invites sent to "alice@example.com" before Alice signed up
        // resolve to her userId now.
        deckCollaboratorService.claimPendingInvitesFor(registered);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse.RegisteredUser(registered, userImageHydrator.pictureImageOf(registered)));
    }

    @GetMapping("/login")
    public void login(
            @RequestParam(required = false) String returnUrl,
            @RequestParam(required = false) String guestId,
            @RequestParam(required = false, defaultValue = OAuthProviderService.GOOGLE) String provider,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String registrationId = SUPPORTED_PROVIDERS.contains(provider) ? provider : OAuthProviderService.GOOGLE;
        if (returnUrl != null) {
            request.getSession(true).setAttribute("returnUrl", returnUrl);
        }
        if (guestId != null && !guestId.isBlank()) {
            request.getSession(true).setAttribute("guestId", guestId);
        }
        response.sendRedirect("/oauth2/authorization/" + registrationId);
    }

    @PostMapping("/guest")
    public ResponseEntity<UserResponse.GuestUser> guestLogin(
            @RequestBody UserResponse.GuestLoginRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = userService.createGuest(request.username());
        String authName = "guest:" + user.getId();
        var guestAuth = new UsernamePasswordAuthenticationToken(
                authName, null, authoritiesService.authoritiesFor(user));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(guestAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse.GuestUser(user, userImageHydrator.pictureImageOf(user)));
    }
}
