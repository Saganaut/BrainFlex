/**
 * Spring Data repository for {@link UserAchievement} rows.
 *
 * The {@code (userId, achievementId)} compound unique index lets a second
 * {@code insert} for the same pair throw {@code DuplicateKeyException} that
 * {@link cephadex.brainflex.service.AchievementService} converts into an
 * idempotent no-op.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.user.UserAchievement;

public interface UserAchievementRepository extends MongoRepository<UserAchievement, String> {

    List<UserAchievement> findAllByUserId(String userId);

    boolean existsByUserIdAndAchievementId(String userId, String achievementId);
}
