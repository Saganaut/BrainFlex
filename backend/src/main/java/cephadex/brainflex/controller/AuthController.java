package cephadex.brainflex.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.UserService;
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

            return userRepository.findByGoogleId(authentication.getName())
                    .<ResponseEntity<UserDTO>>map(user -> ResponseEntity.ok(new UserDTO.RegisteredUser(user)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<UserDTO>build());
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
}