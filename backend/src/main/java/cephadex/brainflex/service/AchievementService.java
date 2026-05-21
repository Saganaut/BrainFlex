/**
 * Awards {@link cephadex.brainflex.model.Achievement} badges to a user when a
 * trigger event happens.
 *
 * Designed for fire-and-forget call sites: every public method swallows its
 * own exceptions and logs, so a broken achievement evaluation can never break
 * the underlying user action (finishing a game, publishing a deck, etc.).
 * Idempotency is structural — the {@code (userId, achievementId)} unique
 * index on {@link cephadex.brainflex.model.UserAchievement} guarantees no
 * duplicate awards even under retries.
 *
 * Guests are intentionally excluded: their accounts are ephemeral, the
 * leaderboard ignores them, and giving them an achievements drawer would
 * mislead users about whether their progress is being persisted.
 *
 * The {@code evaluate} return value is the list of newly-earned achievements
 * for this call so the game-end surface can show "Achievement unlocked"
 * cards in-line; an empty list is the common case.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Achievement;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserAchievement;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.repository.AchievementRepository;
import cephadex.brainflex.repository.UserAchievementRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    public AchievementService(
            AchievementRepository achievementRepository,
            UserAchievementRepository userAchievementRepository,
            UserRepository userRepository,
            ApplicationEventPublisher events) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.events = events;
    }

    /**
     * Awards every Achievement of {@code trigger} whose {@code threshold} is at
     * or below {@code currentValue} and that the user hasn't already earned.
     *
     * Returns the list of achievements newly granted in this call (empty when
     * nothing crossed a threshold this time, which is the common case). Never
     * throws — broken DB calls or missing user docs are logged and swallowed.
     *
     * {@code interactiveSessionId} / {@code deckId} are optional provenance
     * fields stored on the resulting UserAchievement row; pass {@code null}
     * when the trigger isn't session- or deck-scoped.
     */
    public List<Achievement> evaluate(
            String userId,
            AchievementTrigger trigger,
            int currentValue,
            String interactiveSessionId,
            String deckId) {
        if (userId == null || userId.isBlank() || trigger == null) {
            return Collections.emptyList();
        }
        try {
            // Skip guests — ephemeral accounts that never appear on the
            // leaderboard shouldn't accrue achievements either.
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || Boolean.TRUE.equals(userOpt.get().getIsGuest())) {
                return Collections.emptyList();
            }
            User user = userOpt.get();

            List<Achievement> candidates = achievementRepository.findAllByTrigger(trigger);
            if (candidates.isEmpty()) return Collections.emptyList();

            List<Achievement> earned = new ArrayList<>();
            int totalReward = 0;
            for (Achievement achievement : candidates) {
                if (achievement.getThreshold() > currentValue) continue;
                if (userAchievementRepository.existsByUserIdAndAchievementId(
                        userId, achievement.getId())) {
                    continue;
                }
                if (tryInsertEarnedRow(userId, achievement, interactiveSessionId, deckId)) {
                    earned.add(achievement);
                    totalReward += Math.max(0, achievement.getRewardPoints());
                    events.publishEvent(new NotificationEvents.AchievementEarnedEvent(
                            userId, achievement.getId()));
                }
            }

            if (totalReward > 0) {
                user.getStats().setTotalPoints(user.getStats().getTotalPoints() + totalReward);
                userRepository.save(user);
            }
            return earned;
        } catch (Exception e) {
            log.warn("AchievementService.evaluate failed for user {} trigger {}: {}",
                    userId, trigger, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Convenience overload for triggers with no session / deck provenance. */
    public List<Achievement> evaluate(String userId, AchievementTrigger trigger, int currentValue) {
        return evaluate(userId, trigger, currentValue, null, null);
    }

    /** Full catalog — public, ordered by displayOrder. */
    public List<Achievement> listCatalog() {
        // Defensive copy — Spring Data may hand back an immutable view, and
        // the next line sorts in place.
        List<Achievement> all = new ArrayList<>(achievementRepository.findAll());
        all.sort((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
        return all;
    }

    /** Every UserAchievement row for the user, newest-first. */
    public List<UserAchievement> listEarnedByUser(String userId) {
        List<UserAchievement> earned = new ArrayList<>(userAchievementRepository.findAllByUserId(userId));
        earned.sort((a, b) -> {
            if (a.getEarnedAt() == null && b.getEarnedAt() == null) return 0;
            if (a.getEarnedAt() == null) return 1;
            if (b.getEarnedAt() == null) return -1;
            return b.getEarnedAt().compareTo(a.getEarnedAt());
        });
        return earned;
    }

    private boolean tryInsertEarnedRow(
            String userId,
            Achievement achievement,
            String interactiveSessionId,
            String deckId) {
        UserAchievement row = new UserAchievement();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setAchievementId(achievement.getId());
        row.setEarnedInInteractiveSessionId(interactiveSessionId);
        row.setEarnedInDeckId(deckId);
        try {
            userAchievementRepository.insert(row);
            return true;
        } catch (DuplicateKeyException ignored) {
            // Raced with another writer — the row exists, just no-op.
            return false;
        }
    }
}
