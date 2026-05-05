/**
 * Unit tests for GameService state machine and session management.
 * Verifies createSession, joinSession, cancelSession, and getByRoomCode
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

import cephadex.brainflex.dto.CreateGameRequest;
import cephadex.brainflex.model.ContentPack;
import cephadex.brainflex.model.GameResult;
import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.GameSettings;
import cephadex.brainflex.model.SessionPlayer;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.repository.ContentPackRepository;
import cephadex.brainflex.repository.GameResultRepository;
import cephadex.brainflex.repository.GameSessionRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private ContentPackRepository contentPackRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private GameResultRepository gameResultRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GameCacheService gameCache;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameService gameService;

    private User host;
    private ContentPack pack;
    private GameSession lobbySession;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId("host1");
        host.setUserName("hostuser");
        host.setIsGuest(false);

        pack = new ContentPack();
        pack.setId("pack1");
        pack.setName("General Knowledge");
        pack.setPublic(true);

        GameSettings settings = new GameSettings();

        lobbySession = new GameSession();
        lobbySession.setId("session1");
        lobbySession.setRoomCode("ABCD12");
        lobbySession.setHostUserId("host1");
        lobbySession.setStatus(GameStatus.LOBBY);
        lobbySession.setSettings(settings);
        lobbySession.setPlayers(new ArrayList<>());
    }

    // ---- createSession ----

    @Test
    void createSession_WithValidPack_CreatesAndReturnsSession() {
        when(contentPackRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByContentPackId("pack1")).thenReturn(20);
        when(gameSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateGameRequest request = new CreateGameRequest("pack1", null, null, null, null, null, null);
        GameSession result = gameService.createSession(host, request);

        assertNotNull(result);
        assertNotNull(result.getRoomCode());
        assertEquals(GameStatus.LOBBY, result.getStatus());
        assertEquals("host1", result.getHostUserId());
        assertEquals(1, result.getPlayers().size()); // host added automatically
        verify(gameCache).put(any(GameSession.class));
    }

    @Test
    void createSession_WithCustomRounds_AppliesSettings() {
        when(contentPackRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByContentPackId("pack1")).thenReturn(20);
        when(gameSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateGameRequest request = new CreateGameRequest("pack1", null, 5, 20, null, null, null);
        GameSession result = gameService.createSession(host, request);

        assertEquals(5, result.getSettings().getTotalRounds());
        assertEquals(20, result.getSettings().getTimePerQuestion());
    }

    @Test
    void createSession_WithUnknownPack_ThrowsNotFound() {
        when(contentPackRepository.findById("badpack")).thenReturn(Optional.empty());

        CreateGameRequest request = new CreateGameRequest("badpack", null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.createSession(host, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createSession_WhenPackHasNoQuestions_ThrowsUnprocessable() {
        when(contentPackRepository.findById("pack1")).thenReturn(Optional.of(pack));
        when(questionRepository.countByContentPackId("pack1")).thenReturn(0);

        CreateGameRequest request = new CreateGameRequest("pack1", null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.createSession(host, request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
    }

    // ---- getByRoomCode ----

    @Test
    void getByRoomCode_WhenExists_ReturnsSession() {
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        GameSession result = gameService.getByRoomCode("ABCD12");

        assertEquals("ABCD12", result.getRoomCode());
    }

    @Test
    void getByRoomCode_WhenNotFound_ThrowsNotFound() {
        when(gameSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getByRoomCode("XXXXXX"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- joinSession ----

    @Test
    void joinSession_WhenLobbyAndSpaceAvailable_AddsPlayer() {
        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setUserName("newguy");
        newPlayer.setIsGuest(false);

        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GameSession result = gameService.joinSession("ABCD12", newPlayer);

        assertEquals(1, result.getPlayers().size());
        assertEquals("player2", result.getPlayers().get(0).getUserId());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void joinSession_WhenAlreadyJoined_ReturnsExistingSession() {
        SessionPlayer existing = new SessionPlayer();
        existing.setUserId("player2");
        lobbySession.getPlayers().add(existing);

        User returningPlayer = new User();
        returningPlayer.setId("player2");
        returningPlayer.setUserName("returner");
        returningPlayer.setIsGuest(false);

        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        GameSession result = gameService.joinSession("ABCD12", returningPlayer);

        assertEquals(1, result.getPlayers().size()); // no duplicate added
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void joinSession_WhenGameAlreadyStarted_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.IN_PROGRESS);
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User player = new User();
        player.setId("p3");
        player.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinSession("ABCD12", player));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinSession_WhenGuestAndGuestsDisabled_ThrowsForbidden() {
        lobbySession.getSettings().setAllowGuests(false);
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("g1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinSession("ABCD12", guest));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinSession_WhenSessionFull_ThrowsConflict() {
        lobbySession.getSettings().setMaxPlayers(1);
        SessionPlayer existing = new SessionPlayer();
        existing.setUserId("someone");
        lobbySession.getPlayers().add(existing);

        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User latePlayer = new User();
        latePlayer.setId("late");
        latePlayer.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.joinSession("ABCD12", latePlayer));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- cancelSession ----

    @Test
    void cancelSession_AsHost_SetsStatusToCancelled() {
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        gameService.cancelSession("ABCD12", host);

        verify(gameSessionRepository).save(argThat(s -> s.getStatus() == GameStatus.CANCELLED));
        verify(gameCache).evict("ABCD12");
    }

    @Test
    void cancelSession_AsNonHost_ThrowsForbidden() {
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User nonHost = new User();
        nonHost.setId("other");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.cancelSession("ABCD12", nonHost));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelSession_WhenAlreadyFinished_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.FINISHED);
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.cancelSession("ABCD12", host));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- getResults ----

    @Test
    void getResults_WhenExists_ReturnsResult() {
        GameResult result = new GameResult();
        result.setId("result1");
        result.setGameSessionId("session1");

        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(gameResultRepository.findByGameSessionId("session1")).thenReturn(Optional.of(result));

        Optional<GameResult> found = gameService.getResults("ABCD12");

        assertTrue(found.isPresent());
        assertEquals("result1", found.get().getId());
    }

    @Test
    void getResults_WhenNoResult_ReturnsEmpty() {
        when(gameSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(gameResultRepository.findByGameSessionId("session1")).thenReturn(Optional.empty());

        Optional<GameResult> found = gameService.getResults("ABCD12");

        assertFalse(found.isPresent());
    }
}
