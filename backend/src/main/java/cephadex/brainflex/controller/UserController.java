package cephadex.brainflex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.deck.DeckResponse;
import cephadex.brainflex.dto.shared.Page;
import cephadex.brainflex.dto.user.UpdateProfileRequest;
import cephadex.brainflex.dto.user.UserResponse;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckFavorite;
import cephadex.brainflex.model.user.NotificationPrefs;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserImageHydrator;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

        private final UserRepository userRepository;
        private final UserService userService;
        private final DeckFavoriteService deckFavoriteService;
        private final DeckRepository deckRepository;
        private final DeckImageHydrationService deckImageHydrationService;
        private final DeckTagHydrationService deckTagHydrationService;
        private final UserImageHydrator userImageHydrator;

        public UserController(
                        UserRepository userRepository,
                        UserService userService,
                        DeckFavoriteService deckFavoriteService,
                        DeckRepository deckRepository,
                        DeckImageHydrationService deckImageHydrationService,
                        DeckTagHydrationService deckTagHydrationService,
                        UserImageHydrator userImageHydrator) {
                this.userRepository = userRepository;
                this.userService = userService;
                this.deckFavoriteService = deckFavoriteService;
                this.deckRepository = deckRepository;
                this.deckImageHydrationService = deckImageHydrationService;
                this.deckTagHydrationService = deckTagHydrationService;
                this.userImageHydrator = userImageHydrator;
        }

        /**
         * PUBLIC LEADERBOARD
         * Returns a paginated list of users sorted by total points.
         */
        @GetMapping("/leaderboard")
        public List<UserResponse.GuestUser> getLeaderboard(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                PageRequest pageRequest = PageRequest.of(page, size, Sort.by("stats.totalPoints").descending());
                org.springframework.data.domain.Page<User> userPage = userRepository.findAll(pageRequest);

                return userPage.getContent().stream()
                                .map(user -> new UserResponse.GuestUser(user, userImageHydrator.pictureImageOf(user)))
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
        public ResponseEntity<UserResponse.RegisteredUser> getUserProfile(@PathVariable String id) {
                return userRepository.findById(id)
                                .map(user -> ResponseEntity.ok(new UserResponse.RegisteredUser(user,
                                                userImageHydrator.pictureImageOf(user))))
                                .orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/me")
        public ResponseEntity<UserResponse.RegisteredUser> updateProfile(
                        @RequestBody UpdateProfileRequest request,
                        Authentication authentication) {
                return userService.resolveRegisteredUser(authentication)
                                .map(user -> {
                                        User updated = userService.updateProfile(user, request);
                                        return ResponseEntity.ok(
                                                        new UserResponse.RegisteredUser(updated,
                                                                        userImageHydrator.pictureImageOf(updated)));
                                })
                                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        /**
         * Returns the caller's {@link NotificationPrefs}, materialising the
         * spec defaults on the fly when the user record predates the
         * profile-backfill migration. The returned object is suitable to PUT
         * back as-is from the settings page.
         */
        @PreAuthorize("hasRole('USER')")
        @GetMapping("/me/notification-prefs")
        public ResponseEntity<NotificationPrefs> getNotificationPrefs(Authentication authentication) {
                User caller = userService.resolveRegisteredUser(authentication)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Registered account required"));
                NotificationPrefs prefs = caller.getNotificationPrefs();
                if (prefs == null) {
                        prefs = NotificationPrefs.withDefaults();
                        prefs.setMarketingEmail(caller.isNewsletter());
                }
                return ResponseEntity.ok(prefs);
        }

        /**
         * Replaces the caller's {@link NotificationPrefs}. The settings page
         * always sends the full object, so a PUT (not PATCH) maps cleanly. Null
         * or empty maps inside the body are accepted: the read-side helpers
         * ({@code inAppEnabled}/{@code emailEnabled}) fall back to spec
         * defaults per kind.
         */
        @PreAuthorize("hasRole('USER')")
        @PutMapping("/me/notification-prefs")
        public ResponseEntity<NotificationPrefs> updateNotificationPrefs(
                        @RequestBody NotificationPrefs prefs,
                        Authentication authentication) {
                User caller = userService.resolveRegisteredUser(authentication)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Registered account required"));
                if (prefs == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "NotificationPrefs body is required");
                }
                caller.setNotificationPrefs(prefs);
                userRepository.save(caller);
                return ResponseEntity.ok(prefs);
        }

        @PostMapping("/me/close")
        public ResponseEntity<Void> closeAccount(Authentication authentication) {
                return userService.resolveRegisteredUser(authentication)
                                .map(user -> {
                                        userService.closeAccount(user);
                                        return new ResponseEntity<Void>(HttpStatus.OK);
                                })
                                .orElseGet(() -> new ResponseEntity<Void>(HttpStatus.FORBIDDEN));
        }

        /**
         * Paginated favorites list for the authenticated caller, ordered
         * by {@code favoritedAt DESC}. Returns full {@link DeckResponse}s so the
         * grid can render cards without a second lookup; deleted source
         * decks are dropped from the page (with the total reflecting the
         * raw join-row count — close enough until chunk 18 introduces a
         * background cleanup task).
         */
        @PreAuthorize("hasRole('USER')")
        @GetMapping("/me/favorites")
        public Page<DeckResponse> listMyFavorites(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Authentication authentication) {
                User caller = userService.resolveRegisteredUser(authentication)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN, "Registered account required"));
                int safePage = Math.max(0, page);
                int safeSize = Math.max(1, Math.min(50, size));
                PageRequest pageRequest = PageRequest.of(
                                safePage, safeSize, Sort.by("favoritedAt").descending());
                org.springframework.data.domain.Page<DeckFavorite> rows = deckFavoriteService
                                .listForUser(caller.getId(), pageRequest);

                List<String> deckIds = new ArrayList<>(rows.getNumberOfElements());
                for (DeckFavorite row : rows.getContent())
                        deckIds.add(row.getDeckId());

                Map<String, Deck> byId = new HashMap<>();
                for (Deck deck : deckRepository.findAllById(deckIds))
                        byId.put(deck.getId(), deck);

                List<Deck> ordered = new ArrayList<>(deckIds.size());
                for (String deckId : deckIds) {
                        Deck deck = byId.get(deckId);
                        if (deck != null)
                                ordered.add(deck);
                }

                for (Deck deck : ordered)
                        deckImageHydrationService.hydrate(deck);
                deckTagHydrationService.hydrate(ordered);

                List<DeckResponse> items = ordered.stream()
                                .map(d -> new DeckResponse(d, true))
                                .toList();
                boolean hasMore = (long) (safePage + 1) * safeSize < rows.getTotalElements();
                return new Page<>(items, safePage, safeSize, rows.getTotalElements(), hasMore);
        }

}
