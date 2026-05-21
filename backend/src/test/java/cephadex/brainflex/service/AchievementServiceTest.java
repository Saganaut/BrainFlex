/**
 * Unit tests for {@link AchievementService}.
 *
 * Behaviors pinned down:
 *   - awards every catalog row whose threshold is at or below currentValue
 *   - never re-awards an already-earned row (existsByUserIdAndAchievementId guard)
 *   - swallows DuplicateKeyException on insert (race with another writer)
 *   - skips guests outright
 *   - on award, bumps User.stats.totalPoints by the achievement's rewardPoints
 *   - returns the newly-awarded list so callers can surface them in-line
 *   - never throws on repository failures (fire-and-forget contract)
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import cephadex.brainflex.model.Achievement;
import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserAchievement;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.repository.AchievementRepository;
import cephadex.brainflex.repository.UserAchievementRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private AchievementService achievementService;

    @Test
    void evaluate_AwardsAchievementWhenThresholdMet_AndAddsRewardPoints() {
        User user = registeredUser("u-1", 100);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        Achievement points1k = achievement("points-1k", AchievementTrigger.TOTAL_POINTS, 1_000, 50);
        when(achievementRepository.findAllByTrigger(AchievementTrigger.TOTAL_POINTS))
                .thenReturn(List.of(points1k));
        when(userAchievementRepository.existsByUserIdAndAchievementId("u-1", "points-1k"))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.TOTAL_POINTS, 1_200, "session-1", null);

        assertEquals(1, earned.size());
        assertEquals("points-1k", earned.get(0).getId());
        ArgumentCaptor<UserAchievement> rowCaptor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository).insert(rowCaptor.capture());
        assertEquals("session-1", rowCaptor.getValue().getEarnedInInteractiveSessionId());
        // 100 base + 50 reward = 150 saved on user.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(150, userCaptor.getValue().getStats().getTotalPoints());
    }

    @Test
    void evaluate_BelowThreshold_AwardsNothing_AndLeavesTotalPointsAlone() {
        User user = registeredUser("u-1", 100);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        Achievement points1k = achievement("points-1k", AchievementTrigger.TOTAL_POINTS, 1_000, 50);
        when(achievementRepository.findAllByTrigger(AchievementTrigger.TOTAL_POINTS))
                .thenReturn(List.of(points1k));

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.TOTAL_POINTS, 500);

        assertTrue(earned.isEmpty(), "no row should be earned below threshold");
        verify(userAchievementRepository, never()).insert(any(UserAchievement.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void evaluate_AlreadyEarned_NoOps() {
        User user = registeredUser("u-1", 100);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        Achievement firstGame = achievement("first-game", AchievementTrigger.FIRST_GAME, 1, 50);
        when(achievementRepository.findAllByTrigger(AchievementTrigger.FIRST_GAME))
                .thenReturn(List.of(firstGame));
        when(userAchievementRepository.existsByUserIdAndAchievementId("u-1", "first-game"))
                .thenReturn(true);

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.FIRST_GAME, 1);

        assertTrue(earned.isEmpty(), "already-earned achievements are skipped");
        verify(userAchievementRepository, never()).insert(any(UserAchievement.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void evaluate_DuplicateKeyOnInsert_IsSwallowed_AndDoesNotBumpPoints() {
        // Caller "knew" no row existed but a concurrent writer beat them to it.
        User user = registeredUser("u-1", 100);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        Achievement firstGame = achievement("first-game", AchievementTrigger.FIRST_GAME, 1, 50);
        when(achievementRepository.findAllByTrigger(AchievementTrigger.FIRST_GAME))
                .thenReturn(List.of(firstGame));
        when(userAchievementRepository.existsByUserIdAndAchievementId("u-1", "first-game"))
                .thenReturn(false);
        when(userAchievementRepository.insert(any(UserAchievement.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.FIRST_GAME, 1);

        assertTrue(earned.isEmpty(), "duplicate-key races contribute nothing to the returned list");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void evaluate_GuestUser_IsSkipped() {
        User guest = new User();
        guest.setId("g-1");
        guest.setIsGuest(true);
        when(userRepository.findById("g-1")).thenReturn(Optional.of(guest));

        List<Achievement> earned = achievementService.evaluate(
                "g-1", AchievementTrigger.FIRST_GAME, 1);

        assertTrue(earned.isEmpty());
        verify(achievementRepository, never()).findAllByTrigger(any());
        verify(userAchievementRepository, never()).insert(any(UserAchievement.class));
    }

    @Test
    void evaluate_UnknownUserId_NoOps() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        List<Achievement> earned = achievementService.evaluate(
                "ghost", AchievementTrigger.FIRST_GAME, 1);

        assertTrue(earned.isEmpty());
        verify(achievementRepository, never()).findAllByTrigger(any());
    }

    @Test
    void evaluate_ExceptionFromRepository_IsSwallowed() {
        // A broken repository call should never escape — the service is
        // fire-and-forget by contract. We assert the returned list is empty
        // rather than that no throw happened (a leaked exception would fail
        // the test outright).
        when(userRepository.findById("u-1")).thenThrow(new RuntimeException("DB down"));

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.FIRST_GAME, 1);

        assertTrue(earned.isEmpty());
    }

    @Test
    void evaluate_MultipleAchievementsCrossingThreshold_AwardsAll_AndSumsRewardPoints() {
        User user = registeredUser("u-1", 0);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        // Three thresholds at 10, 100, 1000 — currentValue=150 should cross
        // the first two but not the third.
        Achievement a10 = achievement("a-10", AchievementTrigger.GAMES_PLAYED, 10, 25);
        Achievement a100 = achievement("a-100", AchievementTrigger.GAMES_PLAYED, 100, 75);
        Achievement a1000 = achievement("a-1000", AchievementTrigger.GAMES_PLAYED, 1_000, 500);
        when(achievementRepository.findAllByTrigger(AchievementTrigger.GAMES_PLAYED))
                .thenReturn(List.of(a10, a100, a1000));
        when(userAchievementRepository.existsByUserIdAndAchievementId(eq("u-1"), any()))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Achievement> earned = achievementService.evaluate(
                "u-1", AchievementTrigger.GAMES_PLAYED, 150);

        assertEquals(2, earned.size(), "both <=150 thresholds should award");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(100, userCaptor.getValue().getStats().getTotalPoints(),
                "totalPoints should sum 25 + 75 in a single save");
    }

    @Test
    void evaluate_NullOrBlankInputs_NoOp() {
        assertTrue(achievementService.evaluate(null, AchievementTrigger.FIRST_GAME, 1).isEmpty());
        assertTrue(achievementService.evaluate("", AchievementTrigger.FIRST_GAME, 1).isEmpty());
        assertTrue(achievementService.evaluate("u-1", null, 1).isEmpty());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void listCatalog_OrdersByDisplayOrder() {
        Achievement late = achievement("late", AchievementTrigger.FIRST_GAME, 1, 0);
        late.setDisplayOrder(100);
        Achievement early = achievement("early", AchievementTrigger.FIRST_GAME, 1, 0);
        early.setDisplayOrder(10);
        when(achievementRepository.findAll()).thenReturn(List.of(late, early));

        List<Achievement> ordered = achievementService.listCatalog();

        assertEquals("early", ordered.get(0).getId());
        assertEquals("late", ordered.get(1).getId());
    }

    // ---- fixtures ----

    private static User registeredUser(String id, int totalPoints) {
        User u = new User();
        u.setId(id);
        u.setIsGuest(false);
        PlayerStats stats = new PlayerStats();
        stats.setTotalPoints(totalPoints);
        u.setStats(stats);
        return u;
    }

    private static Achievement achievement(String id, AchievementTrigger trigger,
            int threshold, int rewardPoints) {
        Achievement a = new Achievement();
        a.setId(id);
        a.setName(id);
        a.setTrigger(trigger);
        a.setThreshold(threshold);
        a.setRewardPoints(rewardPoints);
        return a;
    }
}
