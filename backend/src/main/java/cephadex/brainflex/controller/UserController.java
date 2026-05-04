package cephadex.brainflex.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.UpdateProfileRequest;
import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

        private final UserRepository userRepository;
        private final UserService userService;

        public UserController(UserRepository userRepository, UserService userService) {
                this.userRepository = userRepository;
                this.userService = userService;
        }

        /**
         * PUBLIC LEADERBOARD
         * Returns a paginated list of users sorted by total points.
         */
        @GetMapping("/leaderboard")
        public List<UserDTO.GuestUser> getLeaderboard(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                PageRequest pageRequest = PageRequest.of(page, size, Sort.by("stats.totalPoints").descending());
                Page<User> userPage = userRepository.findAll(pageRequest);

                return userPage.getContent().stream()
                                .map(user -> new UserDTO.GuestUser(
                                                user.getId(),
                                                user.getUserName(),
                                                user.getIsGuest(),
                                                user.getPictureUrl(),
                                                user.getStats()))
                                .toList();
        }

        @GetMapping("/check-username")
        public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
                return ResponseEntity.ok(Map.of("available", userService.isUsernameAvailable(username)));
        }

        /**
         * PRIVATE USER INFO
         * Returns a a user info
         */
        @GetMapping("/{id}")
        public ResponseEntity<UserDTO.RegisteredUser> getUserProfile(@PathVariable String id) {
                return userRepository.findById(id)
                                .map(user -> ResponseEntity.ok(new UserDTO.RegisteredUser(user)))
                                .orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/me")
        public ResponseEntity<UserDTO.RegisteredUser> updateProfile(
                        @RequestBody UpdateProfileRequest request,
                        Authentication authentication) {
                return resolveRegisteredUser(authentication)
                                .map(user -> ResponseEntity.ok(
                                                new UserDTO.RegisteredUser(userService.updateProfile(user, request))))
                                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        @PostMapping("/me/close")
        public ResponseEntity<Void> closeAccount(Authentication authentication) {
                return resolveRegisteredUser(authentication)
                                .map(user -> {
                                        userService.closeAccount(user);
                                        return new ResponseEntity<Void>(HttpStatus.OK);
                                })
                                .orElseGet(() -> new ResponseEntity<Void>(HttpStatus.FORBIDDEN));
        }

        private Optional<User> resolveRegisteredUser(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()
                                || "anonymousUser".equals(authentication.getName())
                                || authentication.getName().startsWith("guest:")) {
                        return Optional.empty();
                }
                return userRepository.findByGoogleId(authentication.getName());
        }

}
