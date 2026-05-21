/**
 * Per-user achievement row combining catalog metadata with the viewer's
 * earned-state. Used by both {@code GET /api/users/me/achievements} (the
 * caller's own list, including progress toward locked ones) and
 * {@code GET /api/users/{userId}/achievements} (the public profile view,
 * which only includes earned + non-hidden rows).
 *
 * {@code currentProgress} is best-effort for triggers whose progress is
 * cheap to compute (counts on indexed fields, the user's denormalized
 * highScore / totalPoints); triggers that would need a full history scan
 * (STREAK, PERFECT_GAME) return 0 so the frontend renders a neutral bar
 * instead of pretending we know the value.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.enums.AchievementTrigger;

public record UserAchievementDTO(
        String id,
        String name,
        String description,
        String iconUrl,
        String category,
        AchievementTrigger trigger,
        int threshold,
        int rewardPoints,
        boolean hidden,
        int displayOrder,
        boolean earned,
        LocalDateTime earnedAt,
        String earnedInInteractiveSessionId,
        String earnedInDeckId,
        int currentProgress) {
}
