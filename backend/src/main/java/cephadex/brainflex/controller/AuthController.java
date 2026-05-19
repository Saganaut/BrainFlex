package cephadex.brainflex.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AuthoritiesService;
import cephadex.brainflex.service.UserImageHydrator;
import cephadex.brainflex.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final AuthoritiesService authoritiesService;
    private final UserImageHydrator userImageHydrator;

    public AuthController(UserRepository userRepository, UserService userService,
            SecurityContextRepository securityContextRepository,
            AuthoritiesService authoritiesService,
            UserImageHydrator userImageHydrator) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
        this.authoritiesService = authoritiesService;
        this.userImageHydrator = userImageHydrator;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            boolean isGuest = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
            boolean isRegistered = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

            if (isGuest) {
                String id = authentication.getName().substring(6);
                return userRepository.findById(id)
                        .map(user -> ResponseEntity.ok((UserDTO) new UserDTO.GuestUser(user, userImageHydrator.pictureImageOf(user))))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserDTO>build());
            } else if (isRegistered) {
                return userRepository.findByGoogleId(authentication.getName())
                        .map(user -> ResponseEntity.ok((UserDTO) new UserDTO.RegisteredUser(user, userImageHydrator.pictureImageOf(user))))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserDTO>build());
            }
        }

        // Visitor — no session, not even a guest. Return 204 so the frontend's
        // useCurrentUser maps it to state="visitor" via its data==null branch
        // instead of treating the caller as a fake "id=0" guest.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO.RegisteredUser> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<UserDTO.RegisteredUser>build();
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User registered = userService.register(oAuth2User, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDTO.RegisteredUser(registered, userImageHydrator.pictureImageOf(registered)));
    }

    @GetMapping("/login")
    public void login(
            @RequestParam(required = false) String returnUrl,
            @RequestParam(required = false) String guestId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (returnUrl != null) {
            request.getSession(true).setAttribute("returnUrl", returnUrl);
        }
        if (guestId != null && !guestId.isBlank()) {
            request.getSession(true).setAttribute("guestId", guestId);
        }
        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/guest")
    public ResponseEntity<UserDTO.GuestUser> guestLogin(
            @RequestBody UserDTO.GuestLoginRequest request,
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
                .body(new UserDTO.GuestUser(user, userImageHydrator.pictureImageOf(user)));
    }
}
