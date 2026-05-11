/**
 * Unit tests for ShowcaseService state machine and session management.
 * Verifies createShowcase, joinShowcase, cancelShowcase, and getByRoomCode
 * using mocked repositories so no real MongoDB or Redis is required.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.List;
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
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ShowcaseServiceTest {

    @Mock private ShowcaseRepository showcaseRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private ShowcaseResultRepository showcaseResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShowcaseCacheService showcaseCache;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ShowcaseService showcaseService;

    private User host;
    private Deck deck;
    private Showcase lobbySession;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId("host1");
        host.setUserName("hostuser");
        host.setIsGuest(false);

        deck = new Deck();
        deck.setId("deck1");
        deck.setName("Test Deck");
        deck.setElements(sampleElements(20));   // 20 elements so totalRounds=10 default fits

        lobbySession = new Showcase();
        lobbySession.setId("session1");
        lobbySession.setRoomCode("ABCD12");
        lobbySession.setHostUserId("host1");
        lobbySession.setStatus(GameStatus.LOBBY);
        lobbySession.setSettings(new ShowcaseSettings());
        lobbySession.setPlayers(new ArrayList<>());
    }

    private static List<DeckElement> sampleElements(int count) {
        List<DeckElement> els = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = "el-" + i;
            McqOption a = new McqOption(id + "-a", "A", null);
            McqOption b = new McqOption(id + "-b", "B", null);
            els.add(new McqQuestion(
                    id, "Prompt " + i, List.of(a, b), a.id(),
                    100, Difficulty.EASY,
                    false, 0, null,
                    15, null, null, null, null, null, MediaPosition.NONE));
        }
        return els;
    }

    // ---- createShowcase ----

    @Test
    void createShowcase_WithValidDeck_CreatesAndReturnsSession() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "deck1", null, null, null, null, null, null, null, null, null);
        Showcase result = showcaseService.createShowcase(host, request);

        assertNotNull(result);
        assertNotNull(result.getRoomCode());
        assertEquals(GameStatus.LOBBY, result.getStatus());
        assertEquals("host1", result.getHostUserId());
        assertEquals(1, result.getPlayers().size());
        assertEquals(10, result.getDeckSnapshot().size()); // totalRounds default
        verify(showcaseCache).put(any(Showcase.class));
    }

    @Test
    void createShowcase_WithCustomRounds_AppliesSettings() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "deck1", null, 5, 20, null, null, null, null, null, null);
        Showcase result = showcaseService.createShowcase(host, request);

        assertEquals(5, result.getSettings().getTotalRounds());
        assertEquals(20, result.getSettings().getTimePerQuestion());
        assertEquals(5, result.getDeckSnapshot().size());
    }

    @Test
    void createShowcase_WithUnknownDeck_ThrowsNotFound() {
        when(deckRepository.findById("baddeck")).thenReturn(Optional.empty());

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "baddeck", null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.createShowcase(host, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createShowcase_WhenDeckEmpty_ThrowsUnprocessable() {
        Deck empty = new Deck();
        empty.setId("empty");
        empty.setElements(new ArrayList<>());
        when(deckRepository.findById("empty")).thenReturn(Optional.of(empty));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "empty", null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.createShowcase(host, request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
    }

    // ---- getByRoomCode ----

    @Test
    void getByRoomCode_WhenExists_ReturnsSession() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        Showcase result = showcaseService.getByRoomCode("ABCD12");
        assertEquals("ABCD12", result.getRoomCode());
    }

    @Test
    void getByRoomCode_WhenNotFound_ThrowsNotFound() {
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.getByRoomCode("XXXXXX"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- joinShowcase ----

    @Test
    void joinShowcase_WhenLobbyAndSpaceAvailable_AddsPlayer() {
        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setUserName("newguy");
        newPlayer.setIsGuest(false);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Showcase result = showcaseService.joinShowcase("ABCD12", newPlayer);

        assertEquals(1, result.getPlayers().size());
        assertEquals("player2", result.getPlayers().get(0).getUserId());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void joinShowcase_WhenAlreadyJoined_ReturnsExistingSession() {
        ShowcasePlayer existing = new ShowcasePlayer();
        existing.setUserId("player2");
        lobbySession.getPlayers().add(existing);

        User returning = new User();
        returning.setId("player2");
        returning.setIsGuest(false);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        Showcase result = showcaseService.joinShowcase("ABCD12", returning);

        assertEquals(1, result.getPlayers().size());
        verify(showcaseRepository, never()).save(any());
    }

    @Test
    void joinShowcase_WhenGameAlreadyStarted_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.IN_PROGRESS);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User player = new User();
        player.setId("p3");
        player.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.joinShowcase("ABCD12", player));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinShowcase_WhenGuestAndGuestsDisabled_ThrowsForbidden() {
        lobbySession.getSettings().setAllowGuests(false);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("g1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.joinShowcase("ABCD12", guest));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinShowcase_WhenSessionFull_ThrowsConflict() {
        lobbySession.getSettings().setMaxPlayers(1);
        ShowcasePlayer existing = new ShowcasePlayer();
        existing.setUserId("someone");
        lobbySession.getPlayers().add(existing);

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User latePlayer = new User();
        latePlayer.setId("late");
        latePlayer.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.joinShowcase("ABCD12", latePlayer));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- cancelShowcase ----

    @Test
    void cancelShowcase_AsHost_SetsStatusToCancelled() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        showcaseService.cancelShowcase("ABCD12", host);

        verify(showcaseRepository).save(argThat(s -> s.getStatus() == GameStatus.CANCELLED));
        verify(showcaseCache).evict("ABCD12");
    }

    @Test
    void cancelShowcase_AsNonHost_ThrowsForbidden() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User nonHost = new User();
        nonHost.setId("other");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.cancelShowcase("ABCD12", nonHost));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelShowcase_WhenAlreadyFinished_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.FINISHED);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.cancelShowcase("ABCD12", host));

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

        Optional<ShowcaseResult> found = showcaseService.getResults("ABCD12");
        assertTrue(found.isPresent());
        assertEquals("result1", found.get().getId());
    }

    @Test
    void getResults_WhenNoResult_ReturnsEmpty() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseResultRepository.findByShowcaseId("session1")).thenReturn(Optional.empty());

        Optional<ShowcaseResult> found = showcaseService.getResults("ABCD12");
        assertFalse(found.isPresent());
    }
}
