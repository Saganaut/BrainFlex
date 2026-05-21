/**
 * Controller-slice tests for {@link AchievementController}.
 *
 * Services and repositories are mocked with {@code @MockitoBean}; the
 * underlying award logic is covered separately in
 * {@link cephadex.brainflex.service.AchievementServiceTest}. These tests
 * pin down request shapes and the three view-model rules that don't live in
 * the service:
 *   - hidden achievements are masked when the viewer hasn't earned them,
 *     revealed when they have
 *   - the public-profile endpoint drops locked + hidden rows entirely
 *   - the personal endpoint reports cheap progress (totalPoints, highScore,
 *     game counts, deck counts) so the catalog page can render progress bars
 */
package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.Achievement;
import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserAchievement;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GameHistoryRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AchievementService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class AchievementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AchievementService achievementService;
    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private GameHistoryRepository gameHistoryRepository;
    @MockitoBean private DeckRepository deckRepository;

    private User caller;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        PlayerStats stats = new PlayerStats();
        stats.setTotalPoints(1_200);
        stats.setHighScore(800);
        caller.setStats(stats);
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));
    }

    @Test
    void listCatalog_RevealsAllNonHidden_AndMasksHidden_ForAnonymous() throws Exception {
        Achievement firstGame = achievement("first-game", "First Steps",
                "Finish your first game.", AchievementTrigger.FIRST_GAME, 1, false, 10);
        Achievement perfect = achievement("perfect-5", "Flawless",
                "Answer every question right in a 5+ question game.",
                AchievementTrigger.PERFECT_GAME, 5, true, 130);
        when(achievementService.listCatalog()).thenReturn(List.of(firstGame, perfect));
        // No authenticated caller in this scenario.
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("first-game"))
                .andExpect(jsonPath("$[0].name").value("First Steps"))
                .andExpect(jsonPath("$[1].id").value("perfect-5"))
                .andExpect(jsonPath("$[1].name").value("???"))
                .andExpect(jsonPath("$[1].description")
                        .value("Hidden achievement — keep playing to reveal."));
    }

    @Test
    void listCatalog_AuthenticatedCaller_RevealsTheirEarnedHidden() throws Exception {
        Achievement perfect = achievement("perfect-5", "Flawless",
                "Answer every question right in a 5+ question game.",
                AchievementTrigger.PERFECT_GAME, 5, true, 130);
        when(achievementService.listCatalog()).thenReturn(List.of(perfect));
        UserAchievement earned = userAchievement("perfect-5");
        when(achievementService.listEarnedByUser("user-1")).thenReturn(List.of(earned));

        mockMvc.perform(get("/api/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Flawless"));
    }

    @Test
    void listMyAchievements_ReportsProgressAgainstCheapCounters() throws Exception {
        Achievement points1k = achievement("points-1k", "Rising Star",
                "Earn 1,000 lifetime points.", AchievementTrigger.TOTAL_POINTS, 1_000, false, 40);
        Achievement decks10 = achievement("decks-10", "Prolific Author",
                "Create 10 decks.", AchievementTrigger.DECKS_CREATED, 10, false, 160);
        when(achievementService.listCatalog()).thenReturn(List.of(points1k, decks10));
        when(achievementService.listEarnedByUser("user-1")).thenReturn(List.of(
                userAchievement("points-1k")));  // earned the first one
        when(deckRepository.countByCreatorUserId("user-1")).thenReturn(3L);

        mockMvc.perform(get("/api/users/me/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earnedCount").value(1))
                .andExpect(jsonPath("$.totalCount").value(2))
                // earned row carries its earnedAt
                .andExpect(jsonPath("$.items[0].id").value("points-1k"))
                .andExpect(jsonPath("$.items[0].earned").value(true))
                .andExpect(jsonPath("$.items[0].currentProgress").value(1_200))
                // locked row reports current progress from deckRepository.countByCreatorUserId
                .andExpect(jsonPath("$.items[1].id").value("decks-10"))
                .andExpect(jsonPath("$.items[1].earned").value(false))
                .andExpect(jsonPath("$.items[1].currentProgress").value(3));
    }

    @Test
    void listMyAchievements_MasksHiddenLockedRows() throws Exception {
        Achievement perfect = achievement("perfect-5", "Flawless",
                "Answer every question right in a 5+ question game.",
                AchievementTrigger.PERFECT_GAME, 5, true, 130);
        when(achievementService.listCatalog()).thenReturn(List.of(perfect));
        when(achievementService.listEarnedByUser("user-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/users/me/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("???"));
    }

    @Test
    void listAchievementsForUser_PublicView_OmitsLockedAndHiddenEntirely() throws Exception {
        Achievement firstGame = achievement("first-game", "First Steps",
                "Finish your first game.", AchievementTrigger.FIRST_GAME, 1, false, 10);
        Achievement locked = achievement("decks-10", "Prolific Author",
                "Create 10 decks.", AchievementTrigger.DECKS_CREATED, 10, false, 160);
        Achievement earnedHidden = achievement("perfect-5", "Flawless",
                "Answer every question right in a 5+ question game.",
                AchievementTrigger.PERFECT_GAME, 5, true, 130);
        when(achievementService.listCatalog()).thenReturn(
                List.of(firstGame, locked, earnedHidden));
        // Target user has earned first-game (public) AND perfect-5 (hidden);
        // hidden earned rows are still scrubbed from the public profile.
        User target = new User();
        target.setId("u-other");
        target.setStats(new PlayerStats());
        when(userRepository.findById("u-other")).thenReturn(Optional.of(target));
        when(achievementService.listEarnedByUser("u-other")).thenReturn(List.of(
                userAchievement("first-game"),
                userAchievement("perfect-5")));

        mockMvc.perform(get("/api/users/u-other/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("first-game"))
                // totalCount is still the full catalog so the profile can show "1 of 3"
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.earnedCount").value(2));
    }

    @Test
    void listAchievementsForUser_UnknownUser_404() throws Exception {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/ghost/achievements"))
                .andExpect(status().isNotFound());
    }

    // ---- fixtures ----

    private static Achievement achievement(String id, String name, String description,
            AchievementTrigger trigger, int threshold, boolean hidden, int displayOrder) {
        Achievement a = new Achievement();
        a.setId(id);
        a.setName(name);
        a.setDescription(description);
        a.setTrigger(trigger);
        a.setThreshold(threshold);
        a.setHidden(hidden);
        a.setDisplayOrder(displayOrder);
        return a;
    }

    private static UserAchievement userAchievement(String achievementId) {
        UserAchievement row = new UserAchievement();
        row.setId(achievementId + "-row");
        row.setUserId("user-1");
        row.setAchievementId(achievementId);
        row.setEarnedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        return row;
    }
}
