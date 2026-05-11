/**
 * Unit tests for ShowcaseService state machine and session management.
 * Verifies createShowcase, joinShowcase, cancelShowcase, and getByRoomCode
 * using mocked repositories so no real MongoDB or Redis is required.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateShowcaseRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ShowcaseServiceTest {

    @Mock
    private ShowcaseRepository showcaseRepository;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ShowcaseResultRepository showcaseResultRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShowcaseCacheService showcaseCache;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ShowcaseService gameService;

    private User host;
    private Deck pack;
    private Showcase lobbySession;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId("host1");
        host.setUserName("hostuser");
        host.setIsGuest(false);

        pack = new Deck();
        pack.setId("pack1");
        pack.setName("General Knowledge");
        pack.setPublic(true);

        ShowcaseSettings settings = new ShowcaseSettings();

        lobbySession = new Showcase();
        lobbySession.setId("session1");
        lobbySession.setRoomCode("ABCD12");
        lobbySession.setHostUserId("host1");
        lobbySession.setStatus(GameStatus.LOBBY);
        lobbySession.setSettings(settings);
        lobbySession.setPlayers(new ArrayList<>());
    }

    // ---- createShowcase ----

    @Test
    void createSession_WithValidPack_CreatesAndReturnsSession() {
        when(deckRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByDeckId("pack1")).thenReturn(20);
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest("pack1", null, null, null, null, null, null, null, null, null, null);
        Showcase result = gameService.createShowcase(host, request);

        assertNotNull(result);
        assertNotNull(result.getRoomCode());
        assertEquals(GameStatus.LOBBY, result.getStatus());
        assertEquals("host1", result.getHostUserId());
        assertEquals(1, result.getPlayers().size()); // host added automatically
        verify(showcaseCache).put(any(Showcase.class));
    }

    @Test
    void createSession_WithCustomRounds_AppliesSettings() {
        when(deckRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByDeckId("pack1")).thenReturn(20);
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest("pack1", null, 5, 20, null, null, null, null, null, null, null);
        Showcase result = gameService.createShowcase(host, request);

        assertEquals(5, result.getSettings().getTotalRounds());
        assertEquals(20, result.getSettings().getTimePerQuestion());
    }

    @Test
    void createSession_WithUnknownPack_ThrowsNotFound() {
        when(deckRepository.findById("badpack")).thenReturn(Optional.empty());

        CreateShowcaseRequest request = new CreateShowcaseRequest("badpack", null, null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.createShowcase(host, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createSession_WhenPackHasNoQuestions_ThrowsUnprocessable() {
        when(deckRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByDeckId("pack1")).thenReturn(0);

        CreateShowcaseRequest request = new CreateShowcaseRequest("pack1", null, null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.createShowcase(host, request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
    }

    // ---- getByRoomCode ----

    @Test
    void getByRoomCode_WhenExists_ReturnsSession() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        Showcase result = gameService.getByRoomCode("ABCD12");

        assertEquals("ABCD12", result.getRoomCode());
    }

    @Test
    void getByRoomCode_WhenNotFound_ThrowsNotFound() {
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getByRoomCode("XXXXXX"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- joinShowcase ----

    @Test
    void joinSession_WhenLobbyAndSpaceAvailable_AddsPlayer() {
        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setUserName("newguy");
        newPlayer.setIsGuest(false);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Showcase result = gameService.joinShowcase("ABCD12", newPlayer);

        assertEquals(1, result.getPlayers().size());
        assertEquals("player2", result.getPlayers().get(0).getUserId());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void joinSession_WhenAlreadyJoined_ReturnsExistingSession() {
        ShowcasePlayer existing = new ShowcasePlayer();
        existing.setUserId("player2");
        lobbySession.getPlayers().add(existing);

        User returningPlayer = new User();
        returningPlayer.setId("player2");
        returningPlayer.setUserName("returner");
        returningPlayer.setIsGuest(false);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        Showcase result = gameService.joinShowcase("ABCD12", returningPlayer);

        assertEquals(1, result.getPlayers().size()); // no duplicate added
        verify(showcaseRepository, never()).save(any());
    }

    @Test
    void joinSession_WhenGameAlreadyStarted_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.IN_PROGRESS);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User player = new User();
        player.setId("p3");
        player.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinShowcase("ABCD12", player));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinSession_WhenGuestAndGuestsDisabled_ThrowsForbidden() {
        lobbySession.getSettings().setAllowGuests(false);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("g1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinShowcase("ABCD12", guest));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinSession_WhenSessionFull_ThrowsConflict() {
        lobbySession.getSettings().setMaxPlayers(1);
        ShowcasePlayer existing = new ShowcasePlayer();
        existing.setUserId("someone");
        lobbySession.getPlayers().add(existing);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User latePlayer = new User();
        latePlayer.setId("late");
        latePlayer.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinShowcase("ABCD12", latePlayer));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- cancelShowcase ----

    @Test
    void cancelSession_AsHost_SetsStatusToCancelled() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        gameService.cancelShowcase("ABCD12", host);

        verify(showcaseRepository).save(argThat(s -> s.getStatus() == GameStatus.CANCELLED));
        verify(showcaseCache).evict("ABCD12");
    }

    @Test
    void cancelSession_AsNonHost_ThrowsForbidden() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User nonHost = new User();
        nonHost.setId("other");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.cancelShowcase("ABCD12", nonHost));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelSession_WhenAlreadyFinished_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.FINISHED);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.cancelShowcase("ABCD12", host));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- getResults ----

    @Test
    void getResults_WhenExists_ReturnsResult() {
        ShowcaseResult result = new ShowcaseResult();
        result.setId("result1");
        result.setShowcaseId("session1");

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseResultRepository.findByShowcaseId("session1")).thenReturn(Optional.of(result));

        Optional<ShowcaseResult> found = gameService.getResults("ABCD12");

        assertTrue(found.isPresent());
        assertEquals("result1", found.get().getId());
    }

    @Test
    void getResults_WhenNoResult_ReturnsEmpty() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseResultRepository.findByShowcaseId("session1")).thenReturn(Optional.empty());

        Optional<ShowcaseResult> found = gameService.getResults("ABCD12");

        assertFalse(found.isPresent());
    }
}
