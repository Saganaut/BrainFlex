/**
 * Wrapper around {@link UserAchievementDTO} list with summary counts so the
 * profile header can show "12 / 20 earned" without a second request.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record UserAchievementsPage(
        List<UserAchievementDTO> items,
        int earnedCount,
        int totalCount) {
}
