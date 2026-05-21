/**
 * Spring Data repository for the {@link Achievement} catalog.
 *
 * Queries are by {@link AchievementTrigger} so the service can resolve "all
 * Achievements whose trigger matches this event" in a single indexed read.
 * The catalog is small and stable enough (~15–20 rows) that a full read on
 * boot for caching would also be cheap, but keeping it as a per-call query
 * keeps live edits via the admin tool visible without a restart.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Achievement;
import cephadex.brainflex.model.enums.AchievementTrigger;

public interface AchievementRepository extends MongoRepository<Achievement, String> {

    List<Achievement> findAllByTrigger(AchievementTrigger trigger);
}
