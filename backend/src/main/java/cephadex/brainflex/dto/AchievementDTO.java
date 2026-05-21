/**
 * Public catalog projection of {@link cephadex.brainflex.model.Achievement}.
 *
 * Hidden achievements that the viewer hasn't earned are masked to "???" so
 * the surprise survives the catalog browser; once the user earns one, the
 * personal endpoint surfaces the real name/description/icon.
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.Achievement;
import cephadex.brainflex.model.enums.AchievementTrigger;

public record AchievementDTO(
        String id,
        String name,
        String description,
        String iconUrl,
        String category,
        AchievementTrigger trigger,
        int threshold,
        int rewardPoints,
        boolean hidden,
        int displayOrder) {

    /**
     * Reveal the row in full — used when the viewer has earned it (or the
     * achievement isn't hidden in the first place).
     */
    public static AchievementDTO revealed(Achievement a) {
        return new AchievementDTO(
                a.getId(), a.getName(), a.getDescription(), a.getIconUrl(),
                a.getCategory(), a.getTrigger(), a.getThreshold(),
                a.getRewardPoints(), a.isHidden(), a.getDisplayOrder());
    }

    /**
     * Mask name/description/icon for unrevealed hidden achievements. Threshold
     * and trigger are left visible because the frontend still needs to render
     * a progress bar against them (showing "??? / 100" rather than literally
     * hiding the existence of the achievement).
     */
    public static AchievementDTO masked(Achievement a) {
        return new AchievementDTO(
                a.getId(), "???", "Hidden achievement — keep playing to reveal.",
                null, a.getCategory(), a.getTrigger(), a.getThreshold(),
                a.getRewardPoints(), a.isHidden(), a.getDisplayOrder());
    }
}
