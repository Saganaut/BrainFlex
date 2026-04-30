package cephadex.brainflex.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
// @RequiredArgsConstructor
public class UserController {

        private final UserRepository userRepository;

        public UserController(UserRepository userRepository) {
                this.userRepository = userRepository;
        }

        /**
         * PUBLIC LEADERBOARD
         * Returns a paginated list of users sorted by total points.
         */
        @GetMapping("/leaderboard")
        public List<UserDTO.Public> getLeaderboard(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                PageRequest pageRequest = PageRequest.of(page, size, Sort.by("stats.totalPoints").descending());
                Page<User> userPage = userRepository.findAll(pageRequest);

                return userPage.getContent().stream()
                                .map(user -> new UserDTO.Public(
                                                user.getId(),
                                                user.getUserName(),
                                                user.getIsGuest(),
                                                user.getPictureUrl(),
                                                user.getStats()))
                                .toList();
        }

        /**
         * PRIVATE USER INFO
         * Returns a a user info
         */
        @GetMapping("/{id}")
        public ResponseEntity<UserDTO.Private> getUserProfile(@PathVariable String id) {
                return userRepository.findById(id)
                                .map(user -> ResponseEntity.ok(new UserDTO.Private(
                                                user.getId(),
                                                user.getEmail(),
                                                user.getName(),
                                                user.getUserName(),
                                                user.getIsGuest(),
                                                user.getGoogleId(),
                                                user.getPictureUrl(),
                                                user.getStats(),
                                                user.getLastLogin(),
                                                user.getCreatedAt())))
                                .orElse(ResponseEntity.notFound().build());
        }

}
