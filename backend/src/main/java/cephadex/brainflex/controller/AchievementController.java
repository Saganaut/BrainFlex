/**
 * REST endpoints for the achievement catalog and per-user "earned" lists.
 *
 *   GET /api/achievements                      public catalog; hidden rows masked.
 *   GET /api/users/me/achievements             caller's earned + locked list with progress.
 *   GET /api/users/{userId}/achievements       public profile view — earned + non-hidden only.
 *
 * Progress for locked achievements is computed inline against the cheap
 * sources we already maintain (User.stats, history counts, deck counts).
 * Triggers without a cheap counter (STREAK, PERFECT_GAME, FAVORITES_RECEIVED
 * on the personal endpoint) report 0 progress so the UI can render a neutral
 * progress bar rather than fabricate a value.
 */
package cephadex.brainflex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.AchievementResponse;
import cephadex.brainflex.dto.UserAchievementResponse;
import cephadex.brainflex.dto.UserAchievementsPage;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.model.user.Achievement;
import cephadex.brainflex.model.user.PlayerStats;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.model.user.UserAchievement;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GameHistoryRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AchievementService;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final DeckRepository deckRepository;

    public AchievementController(
            AchievementService achievementService,
            UserService userService,
            UserRepository userRepository,
            GameHistoryRepository gameHistoryRepository,
            DeckRepository deckRepository) {
        this.achievementService = achievementService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.gameHistoryRepository = gameHistoryRepository;
        this.deckRepository = deckRepository;
    }

    /**
     * Public catalog. If the caller is authenticated we mask hidden rows they
     * haven't earned; for anonymous viewers everything hidden is masked.
     */
    @GetMapping("/achievements")
    public List<AchievementResponse> listCatalog(Authentication authentication) {
        List<Achievement> catalog = achievementService.listCatalog();
        Set<String> earnedIds = userService.resolveRegisteredUser(authentication)
                .map(user -> earnedAchievementIds(user.getId()))
                .orElse(Set.of());
        return catalog.stream()
                .map(a -> (a.isHidden() && !earnedIds.contains(a.getId()))
                        ? AchievementResponse.masked(a)
                        : AchievementResponse.revealed(a))
                .toList();
    }

    /**
     * Caller's own list. Includes locked entries with progress so the catalog
     * page can render "7 / 10 games played" bars.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/users/me/achievements")
    public UserAchievementsPage listMyAchievements(Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Registered account required"));
        return buildUserPage(caller, /* publicView = */ false);
    }

    /**
     * Public profile view. Anonymous-friendly; returns only the target user's
     * earned + non-hidden achievements (the rest is private).
     */
    @GetMapping("/users/{userId}/achievements")
    public UserAchievementsPage listAchievementsForUser(@PathVariable String userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildUserPage(target, /* publicView = */ true);
    }

    // -------- helpers --------

    private UserAchievementsPage buildUserPage(User target, boolean publicView) {
        List<Achievement> catalog = achievementService.listCatalog();
        List<UserAchievement> earnedRows = achievementService.listEarnedByUser(target.getId());
        Map<String, UserAchievement> earnedById = new HashMap<>();
        for (UserAchievement row : earnedRows)
            earnedById.put(row.getAchievementId(), row);

        // Pre-compute progress only for triggers the catalog actually uses.
        // Avoids 20 redundant Mongo round-trips on a catalog with 4 unique triggers.
        Set<AchievementTrigger> activeTriggers = new HashSet<>();
        for (Achievement a : catalog)
            activeTriggers.add(a.getTrigger());
        Map<AchievementTrigger, Integer> progressByTrigger = computeProgressByTrigger(target, activeTriggers);

        List<UserAchievementResponse> items = new ArrayList<>(catalog.size());
        int earnedCount = 0;
        for (Achievement a : catalog) {
            boolean earned = earnedById.containsKey(a.getId());
            if (earned)
                earnedCount++;
            if (publicView && (!earned || a.isHidden()))
                continue;

            UserAchievement row = earnedById.get(a.getId());
            boolean mask = a.isHidden() && !earned;
            int progress = progressByTrigger.getOrDefault(a.getTrigger(), 0);
            items.add(new UserAchievementResponse(
                    a.getId(),
                    mask ? "???" : a.getName(),
                    mask ? "Hidden achievement — keep playing to reveal." : a.getDescription(),
                    mask ? null : a.getIconUrl(),
                    a.getCategory(),
                    a.getTrigger(),
                    a.getThreshold(),
                    a.getRewardPoints(),
                    a.isHidden(),
                    a.getDisplayOrder(),
                    earned,
                    row == null ? null : row.getEarnedAt(),
                    row == null ? null : row.getEarnedInInteractiveSessionId(),
                    row == null ? null : row.getEarnedInDeckId(),
                    progress));
        }
        return new UserAchievementsPage(items, earnedCount, catalog.size());
    }

    private Set<String> earnedAchievementIds(String userId) {
        Set<String> ids = new HashSet<>();
        for (UserAchievement row : achievementService.listEarnedByUser(userId)) {
            ids.add(row.getAchievementId());
        }
        return ids;
    }

    /**
     * Computes the cheap, readily-available progress value for each trigger
     * type. Triggers without an indexed counter (STREAK, PERFECT_GAME) are
     * left out; the caller defaults missing entries to 0.
     *
     * FAVORITES_RECEIVED uses the maximum favoriteCount across the user's
     * decks, mirroring the per-deck trigger semantics on the write side
     * — i.e. "have a deck with N favorites" rather than the cross-deck sum.
     */
    private Map<AchievementTrigger, Integer> computeProgressByTrigger(
            User user, Set<AchievementTrigger> wanted) {
        Map<AchievementTrigger, Integer> result = new HashMap<>();
        if (wanted.isEmpty())
            return result;
        String userId = user.getId();
        PlayerStats stats = user.getStats() == null ? new PlayerStats() : user.getStats();

        if (wanted.contains(AchievementTrigger.FIRST_GAME)
                || wanted.contains(AchievementTrigger.GAMES_PLAYED)) {
            int games = clampToInt(gameHistoryRepository.countByUserId(userId));
            if (wanted.contains(AchievementTrigger.FIRST_GAME)) {
                result.put(AchievementTrigger.FIRST_GAME, games > 0 ? 1 : 0);
            }
            if (wanted.contains(AchievementTrigger.GAMES_PLAYED)) {
                result.put(AchievementTrigger.GAMES_PLAYED, games);
            }
        }
        if (wanted.contains(AchievementTrigger.HOST_GAMES)) {
            result.put(AchievementTrigger.HOST_GAMES,
                    clampToInt(gameHistoryRepository.countByUserIdAndWasHostTrue(userId)));
        }
        if (wanted.contains(AchievementTrigger.TOTAL_POINTS)) {
            result.put(AchievementTrigger.TOTAL_POINTS, stats.getTotalPoints());
        }
        if (wanted.contains(AchievementTrigger.HIGH_SCORE)) {
            result.put(AchievementTrigger.HIGH_SCORE, stats.getHighScore());
        }
        if (wanted.contains(AchievementTrigger.DECKS_CREATED)) {
            result.put(AchievementTrigger.DECKS_CREATED,
                    clampToInt(deckRepository.countByCreatorUserId(userId)));
        }
        if (wanted.contains(AchievementTrigger.DECKS_PUBLISHED)) {
            result.put(AchievementTrigger.DECKS_PUBLISHED,
                    clampToInt(deckRepository.countByCreatorUserIdAndPublishStatus(
                            userId, PublishStatus.PUBLISHED)));
        }
        if (wanted.contains(AchievementTrigger.FAVORITES_RECEIVED)) {
            int max = 0;
            for (Deck deck : deckRepository.findByCreatorUserId(userId)) {
                if (deck.getFavoriteCount() > max)
                    max = deck.getFavoriteCount();
            }
            result.put(AchievementTrigger.FAVORITES_RECEIVED, max);
        }
        // STREAK and PERFECT_GAME intentionally skipped — see method javadoc.
        return result;
    }

    private static int clampToInt(long value) {
        return (int) Math.min(Math.max(value, 0L), Integer.MAX_VALUE);
    }
}
