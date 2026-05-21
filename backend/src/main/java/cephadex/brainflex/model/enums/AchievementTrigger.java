/**
 * The events that can award an {@link cephadex.brainflex.model.Achievement}.
 *
 * Each value defines what {@code currentValue} represents when
 * {@code AchievementService.evaluate(userId, trigger, currentValue, ...)} is
 * called, and therefore what {@code Achievement.threshold} is compared against:
 *
 *   FIRST_GAME            currentValue=1 on any finished game; threshold=1.
 *   GAMES_PLAYED          currentValue=lifetime finished-games count for the player.
 *   TOTAL_POINTS          currentValue=lifetime PlayerStats.totalPoints.
 *   HIGH_SCORE            currentValue=this game's finalScore.
 *   STREAK                currentValue=this game's longestStreak.
 *   PERFECT_GAME          currentValue=this game's totalQuestions; only fires when
 *                         the player's accuracy == 1.0 (filter applied at call site).
 *   HOST_GAMES            currentValue=lifetime games hosted (wasHost=true rows).
 *   DECKS_CREATED         currentValue=count of decks owned by the user.
 *   DECKS_PUBLISHED       currentValue=count of owned decks with PublishStatus.PUBLISHED.
 *   FAVORITES_RECEIVED    currentValue=sum of favoriteCount across decks the user owns.
 *
 * {@code REACTIONS_SENT} and {@code WORD_CLOUD_SUBMITTED} from the chunk-17 doc
 * are deferred — neither has a cheap counter on the user document yet, and
 * aggregating across every GameHistoryEntry / element submission on every
 * game-end is more cost than this fire-and-forget path can swallow. Add them
 * back once a lifetime counter lands on User.stats.
 */
package cephadex.brainflex.model.enums;

public enum AchievementTrigger {
    FIRST_GAME,
    GAMES_PLAYED,
    TOTAL_POINTS,
    HIGH_SCORE,
    STREAK,
    PERFECT_GAME,
    HOST_GAMES,
    DECKS_CREATED,
    DECKS_PUBLISHED,
    FAVORITES_RECEIVED
}
