package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
import cephadex.brainflex.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            String name = authentication.getName();
            if (name.startsWith("guest:")) {
                String id = name.substring(6);
                return userRepository.findById(id)
                        .map(user -> ResponseEntity.ok((UserDTO) new UserDTO.GuestUser(user)))
                        .orElseGet(() -> ResponseEntity
                                .<UserDTO>ok(new UserDTO.GuestUser("0", "Guest", true, null, null)));
            } else {
                return userRepository.findByGoogleId(name)
                        .map(user -> ResponseEntity.ok((UserDTO) new UserDTO.RegisteredUser(user)))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserDTO>build());
            }
        }

        return ResponseEntity.<UserDTO>ok(new UserDTO.GuestUser("0", "Guest", true, null, null));
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDTO.RegisteredUser(userService.register(oAuth2User, request)));
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
            HttpServletRequest httpRequest) {
        User user = userService.createGuest(request.username());
        String authName = "guest:" + user.getId();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authName, null, Collections.emptyList()));
        httpRequest.getSession(true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDTO.GuestUser(user));
    }
}