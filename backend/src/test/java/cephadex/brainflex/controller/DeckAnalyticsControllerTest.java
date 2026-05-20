/**
 * Slice test for {@code GET /api/decks/{id}/analytics} on
 * {@link DeckController}.
 *
 * Mocks {@link DeckAnalyticsService} + {@link AuthorizationService} +
 * {@link UserService}/{@code UserRepository}. The full per-kind bucketing
 * + math is covered in
 * {@link cephadex.brainflex.service.DeckAnalyticsServiceTest}; this file
 * pins down the controller's auth wiring and empty-state behavior:
 *   - owner / EDITOR  → 200 with the rollup
 *   - never-played    → 200 with a zero-default {@link DeckAnalytics}
 *   - non-editor      → 403 (the AuthorizationService throws)
 *   - missing deck    → 404 (same path)
 */
package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckAnalytics;
import cephadex.brainflex.model.ElementStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.AuthorizationService;
import cephadex.brainflex.service.DeckAnalyticsService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class DeckAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DeckAnalyticsService deckAnalyticsService;
    @MockitoBean private AuthorizationService authorizationService;
    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;

    private User caller;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        caller.setIsGuest(false);
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));
    }

    @Test
    void getDeckAnalytics_OwnerOrEditor_ReturnsRollup() throws Exception {
        Deck deck = new Deck();
        deck.setId("deck-1");
        when(authorizationService.requireDeckEditable(eq("deck-1"), eq(caller))).thenReturn(deck);

        DeckAnalytics analytics = new DeckAnalytics();
        analytics.setDeckId("deck-1");
        analytics.setTotalPlays(3);
        analytics.setTotalPlayers(12);
        analytics.setAverageScore(85.5);
        analytics.setAverageAccuracy(0.72);
        analytics.setAverageDurationMs(420_000L);
        analytics.setLastPlayedAt(LocalDateTime.of(2026, 5, 19, 10, 0));
        ElementStats stats = new ElementStats();
        stats.setPresentedCount(3);
        stats.setAnsweredCount(11);
        stats.setCorrectCount(8);
        analytics.getPerElement().put("mcq-1", stats);
        when(deckAnalyticsService.findByDeckId("deck-1")).thenReturn(analytics);

        mockMvc.perform(get("/api/decks/deck-1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value("deck-1"))
                .andExpect(jsonPath("$.totalPlays").value(3))
                .andExpect(jsonPath("$.totalPlayers").value(12))
                .andExpect(jsonPath("$.averageScore").value(85.5))
                .andExpect(jsonPath("$.perElement.mcq-1.presentedCount").value(3))
                .andExpect(jsonPath("$.perElement.mcq-1.correctCount").value(8));
    }

    @Test
    void getDeckAnalytics_NeverPlayed_ReturnsEmptyRollup() throws Exception {
        Deck deck = new Deck();
        deck.setId("deck-7");
        when(authorizationService.requireDeckEditable(eq("deck-7"), eq(caller))).thenReturn(deck);
        when(deckAnalyticsService.findByDeckId("deck-7")).thenReturn(null);

        mockMvc.perform(get("/api/decks/deck-7/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value("deck-7"))
                .andExpect(jsonPath("$.totalPlays").value(0))
                .andExpect(jsonPath("$.totalPlayers").value(0))
                .andExpect(jsonPath("$.perElement").isMap());
    }

    @Test
    void getDeckAnalytics_NotEditable_Returns403() throws Exception {
        when(authorizationService.requireDeckEditable(eq("deck-2"), eq(caller)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no access"));

        mockMvc.perform(get("/api/decks/deck-2/analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDeckAnalytics_DeckMissing_Returns404() throws Exception {
        when(authorizationService.requireDeckEditable(eq("missing"), eq(caller)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        mockMvc.perform(get("/api/decks/missing/analytics"))
                .andExpect(status().isNotFound());
    }
}
