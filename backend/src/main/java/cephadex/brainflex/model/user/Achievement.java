/**
 * Catalog entry for a single badge a user can earn through gameplay or
 * creator activity. Catalog rows live in the {@code achievements} collection
 * and are seeded by {@link cephadex.brainflex.config.SampleDataSeeder}; the
 * per-user "I earned this" rows live separately in {@link UserAchievement}.
 *
 * The id is a stable slug (e.g. {@code "first-game"}, {@code "scoring-10k"})
 * so seed re-runs and frontend deep-links don't break when collections are
 * rebuilt. {@code threshold} semantics depend on {@link cephadex.brainflex.model.enums.AchievementTrigger}
 * — see the enum's javadoc.
 *
 * {@code hidden=true} keeps the catalog endpoint from leaking the
 * description / icon until a user earns it — the frontend renders a generic
 * "???" card in its place.
 */
package cephadex.brainflex.model.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.AchievementTrigger;
import lombok.Data;
import cephadex.brainflex.model.shared.Auditable;

@Data
@Document(collection = "achievements")
public class Achievement extends Auditable {

    @Id
    private String id;

    private String name;
    private String description;
    private String iconUrl;
    private String category;

    @Indexed
    private AchievementTrigger trigger;

    /** Numeric threshold the trigger compares against — see AchievementTrigger. */
    private int threshold;

    /** Bonus added to {@code User.stats.totalPoints} when this is first earned. */
    private int rewardPoints;

    /** Hidden achievements only reveal name/description after unlock. */
    private boolean hidden;

    private int displayOrder;
}
