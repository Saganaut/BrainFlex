/**
 * Smoke tests for the three GET endpoints on {@link GameHistoryController}.
 *
 * The {@link cephadex.brainflex.service.GameHistoryService} is mocked via
 * {@code @MockitoBean} so this stays a fast controller-slice test — the
 * underlying write + counter behavior is covered in GameHistoryServiceTest.
 */
package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.GameHistoryEntry;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.GameHistoryService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class GameHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GameHistoryService gameHistoryService;
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
    void listMyHistory_ReturnsPageWithDtoFields() throws Exception {
        GameHistoryEntry entry = sampleEntry("user-1", "session-7", "deck-1", "LOTR", 80, 1);
        Page<GameHistoryEntry> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);
        when(gameHistoryService.listForUser(eq("user-1"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/me/history").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].interactiveSessionId").value("session-7"))
                .andExpect(jsonPath("$.items[0].deckName").value("LOTR"))
                .andExpect(jsonPath("$.items[0].finalScore").value(80))
                .andExpect(jsonPath("$.items[0].placement").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void listMyHistory_WhenNotRegistered_Returns403() throws Exception {
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUserHistory_ReturnsPageForRegisteredTarget() throws Exception {
        User target = new User();
        target.setId("user-2");
        target.setIsGuest(false);
        target.setIsClosed(false);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(target));

        Page<GameHistoryEntry> page = new PageImpl<>(
                List.of(sampleEntry("user-2", "session-3", "deck-1", "LOTR", 50, 2)),
                PageRequest.of(0, 20), 1);
        when(gameHistoryService.listForUser(eq("user-2"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/user-2/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].finalScore").value(50));
    }

    @Test
    void listUserHistory_WhenTargetIsGuest_Returns404() throws Exception {
        User guest = new User();
        guest.setId("guest-7");
        guest.setIsGuest(true);
        when(userRepository.findById("guest-7")).thenReturn(Optional.of(guest));

        mockMvc.perform(get("/api/users/guest-7/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listUserHistory_WhenTargetClosed_Returns404() throws Exception {
        User closed = new User();
        closed.setId("user-9");
        closed.setIsGuest(false);
        closed.setIsClosed(true);
        when(userRepository.findById("user-9")).thenReturn(Optional.of(closed));

        mockMvc.perform(get("/api/users/user-9/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listUserHistory_WhenTargetMissing_Returns404() throws Exception {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/users/ghost/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMyHistoryForDeck_FiltersToOneDeck() throws Exception {
        Page<GameHistoryEntry> page = new PageImpl<>(
                List.of(sampleEntry("user-1", "session-1", "deck-1", "LOTR", 100, 1)),
                PageRequest.of(0, 20), 1);
        when(gameHistoryService.listForUserAndDeck(eq("user-1"), eq("deck-1"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/decks/deck-1/history/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].deckId").value("deck-1"))
                .andExpect(jsonPath("$.items[0].finalScore").value(100));

        verify(gameHistoryService).listForUserAndDeck(eq("user-1"), eq("deck-1"), any(Pageable.class));
    }

    @Test
    void listMyHistory_ClampsOversizedPageSize() throws Exception {
        Page<GameHistoryEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(gameHistoryService.listForUser(eq("user-1"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/me/history").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    private GameHistoryEntry sampleEntry(String userId, String sessionId, String deckId,
            String deckName, int score, int placement) {
        GameHistoryEntry e = new GameHistoryEntry();
        e.setId("h-" + sessionId);
        e.setUserId(userId);
        e.setInteractiveSessionId(sessionId);
        e.setDeckId(deckId);
        e.setDeckName(deckName);
        e.setHostUserId("host-1");
        e.setHostName("Kevin");
        e.setFinalScore(score);
        e.setPlacement(placement);
        e.setTotalQuestions(10);
        e.setCorrectAnswers(8);
        e.setLongestStreak(3);
        e.setAccuracy(0.8);
        e.setDurationMs(180_000L);
        e.setPlayedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        return e;
    }
}
