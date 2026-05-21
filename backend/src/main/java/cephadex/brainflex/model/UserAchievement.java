/**
 * Per-user "I earned this badge" row. The compound unique index on
 * {@code (userId, achievementId)} is the idempotency guarantee:
 * {@link cephadex.brainflex.service.AchievementService} catches the resulting
 * {@code DuplicateKeyException} so a repeated trigger evaluation is a no-op,
 * the catalog seed can be re-run, and the host can replay {@code recordFinish}
 * without double-awarding anything.
 *
 * {@code earnedInInteractiveSessionId} and {@code earnedInDeckId} are
 * optional provenance hooks — useful when the same achievement can be earned
 * from multiple paths and we want to link the "Achievement unlocked!" toast
 * back to the round it happened in.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "user_achievements")
@CompoundIndex(name = "user_achievement_unique_idx",
        def = "{'userId': 1, 'achievementId': 1}",
        unique = true)
public class UserAchievement {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String achievementId;

    private LocalDateTime earnedAt = LocalDateTime.now();

    /** Nullable — only set when the trigger fired from a finished session. */
    private String earnedInInteractiveSessionId;

    /** Nullable — only set when the trigger fired from a deck-scoped event. */
    private String earnedInDeckId;
}
