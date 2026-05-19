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

import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.CreateShowcaseRequest;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.VotePhaseStartMessage;
import cephadex.brainflex.dto.VoteSubmitRequest;
import cephadex.brainflex.dto.WordCloudUpdateMessage;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.answer.DrawingAnswer;
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.Stroke;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ShowcasePhase;
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
    @Mock private AuthorizationService authorizationService;
    @Mock private DeckImageHydrationService deckImageHydrationService;
    @Mock private DeckService deckService;
    @Mock private UserImageHydrator userImageHydrator;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
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
            McqOption a = new McqOption(id + "-a", "A", null, null);
            McqOption b = new McqOption(id + "-b", "B", null, null);
            els.add(new McqQuestion(
                    id, "pub_" + id, "prv_" + id, "Prompt " + i, null,
                    "Prompt " + i, List.of(a, b), List.of(a.id()),
                    100, Difficulty.EASY,
                    true, false, null, cephadex.brainflex.model.enums.ResponseMode.ACCEPTING_RESPONSES,
                    false, null, 0, null,
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
        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(lobbySession);
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        showcaseService.cancelShowcase("ABCD12", host);

        verify(showcaseRepository).save(argThat(s -> s.getStatus() == GameStatus.CANCELLED));
        verify(showcaseCache).evict("ABCD12");
    }

    @Test
    void cancelShowcase_AsNonHost_ThrowsForbidden() {
        User nonHost = new User();
        nonHost.setId("other");
        when(authorizationService.requireShowcaseHost("ABCD12", nonHost))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.cancelShowcase("ABCD12", nonHost));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelShowcase_WhenAlreadyFinished_ThrowsConflict() {
        lobbySession.setStatus(GameStatus.FINISHED);
        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(lobbySession);

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

    // ---- Best Answer phase machine ----

    /** Two players, one Best-Answer MCQ at index 0, both submit → expect VOTE phase. */
    @Test
    void submitAnswer_OnBestAnswerRound_WhenAllAnswered_TransitionsToVotePhase() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));

        showcaseService.submitAnswer("ABCD12", req, "guest:p1");
        showcaseService.submitAnswer("ABCD12", req, "guest:p2");

        assertEquals(ShowcasePhase.VOTE, session.getPhase());

        // Each submitter has a submissionId; no timeouts means two anonymized
        // submissions go out on the votePhase channel.
        long submissionsWithId = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getSubmissionId() != null).count();
        assertEquals(2, submissionsWithId);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/votePhase"),
                any(VotePhaseStartMessage.class));
    }

    /**
     * Vote tally: p1 submission gets two votes, p2 gets one → p1 wins, gets the
     * bonus on top of their answer points.
     */
    @Test
    void submitVote_WhenAllVoted_AwardsBonusToWinnerAndAdvances() {
        Showcase session = bestAnswerSessionWithThreePlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest answerReq = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        showcaseService.submitAnswer("ABCD12", answerReq, "guest:p1");
        showcaseService.submitAnswer("ABCD12", answerReq, "guest:p2");
        showcaseService.submitAnswer("ABCD12", answerReq, "guest:p3");

        // After SUBMIT phase completes we're now in VOTE; grab the players'
        // submissionIds so the votes reference real submissions.
        String p1Sub = answerOf(session, "p1", el.id()).getSubmissionId();
        String p2Sub = answerOf(session, "p2", el.id()).getSubmissionId();

        // p1 and p3 both vote for p1's submission; p2 votes for p2 (themselves).
        showcaseService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p1Sub), "guest:p1");
        showcaseService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p2Sub), "guest:p2");
        showcaseService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p1Sub), "guest:p3");

        // p1's submission won → bestAnswerWinner flag + bonus applied to their score.
        PlayerAnswer p1Answer = answerOf(session, "p1", el.id());
        assertTrue(p1Answer.isBestAnswerWinner());
        ShowcasePlayer p1 = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals("p1")).findFirst().orElseThrow();
        // 100 base (correct answer) + 50 best-answer bonus = 150
        assertEquals(150, p1.getScore());

        // Loser p2 keeps just their base score (incorrect answer chose option-a; we
        // verify they did not receive the winner bonus).
        ShowcasePlayer p2 = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals("p2")).findFirst().orElseThrow();
        assertFalse(answerOf(session, "p2", el.id()).isBestAnswerWinner());
        assertEquals(100, p2.getScore());

        // Reveal broadcast carries BestAnswerOutcome with winner ids + bonus.
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/roundResult"),
                argThat((Object msg) -> {
                    if (!(msg instanceof RoundResultMessage rr)) return false;
                    if (rr.bestAnswer() == null) return false;
                    return rr.bestAnswer().winnerUserIds().contains("p1")
                            && !rr.bestAnswer().winnerUserIds().contains("p2")
                            && rr.bestAnswer().bonusAwarded() == 50;
                }));
    }

    /** A stale vote arriving in SUBMIT phase is silently dropped. */
    @Test
    void submitVote_WhenNotInVotePhase_IsIgnored() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        // Stays in SUBMIT — no answers submitted yet.
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        showcaseService.submitVote("ABCD12",
                new VoteSubmitRequest(el.id(), "any-sub-id"),
                "guest:p1");

        // No vote recorded; no broadcast emitted.
        assertTrue(session.getPlayers().stream().allMatch(p -> p.getVotes().isEmpty()));
        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/voted"),
                any(Object.class));
    }

    // ---- Helpers ----

    // ---- Word Cloud rounds ----

    /**
     * A single submission emits /wordCloud with the normalized words counted.
     * Subsequent submitters add to the rolling map and trigger another emit.
     */
    @Test
    void submitAnswer_OnWordCloudRound_BroadcastsLiveAggregation() {
        Showcase session = wordCloudSessionWithTwoPlayers(2);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("Monday", "rainy"))),
                "guest:p1");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/wordCloud"),
                argThat((Object msg) -> msg instanceof WordCloudUpdateMessage w
                        && w.elementId().equals(el.id())
                        && w.counts().getOrDefault("monday", 0) == 1
                        && w.counts().getOrDefault("rainy", 0) == 1));
    }

    /**
     * Case folding + banned-words filtering happens at submit time so the
     * historical PlayerAnswer.payload matches the aggregator's view.
     */
    @Test
    void submitAnswer_OnWordCloudRound_NormalizesPayloadBeforeStoring() {
        Showcase session = wordCloudSessionWithTwoPlayers(3);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(),
                        new WordCloudAnswer(List.of("  Monday!! ", "spam", "RAINY"))),
                "guest:p1");

        PlayerAnswer stored = answerOf(session, "p1", el.id());
        assertTrue(stored.getPayload() instanceof WordCloudAnswer);
        WordCloudAnswer normalized = (WordCloudAnswer) stored.getPayload();
        assertEquals(List.of("monday", "rainy"), normalized.words());
    }

    /**
     * When every player has submitted, the round completes — the per-player
     * payloads in the RoundResultMessage are nulled out for privacy on a
     * Word Cloud round (the aggregated cloud goes out on its own topic).
     */
    @Test
    void submitAnswer_OnWordCloudRound_WhenAllAnswered_RedactsPerPlayerPayloads() {
        Showcase session = wordCloudSessionWithTwoPlayers(2);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(showcaseRepository.save(any(Showcase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("Monday"))), "guest:p1");
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("monday"))), "guest:p2");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/roundResult"),
                argThat((Object msg) -> msg instanceof RoundResultMessage rr
                        && rr.playerResults().stream().allMatch(r -> r.payload() == null)));
    }

    /**
     * An empty submission (all words filtered out by the banned list) is a
     * silent no-op — no PlayerAnswer is stored and no broadcast goes out.
     */
    @Test
    void submitAnswer_OnWordCloudRound_DroppedWhenEntirelyBanned() {
        Showcase session = wordCloudSessionWithTwoPlayers(3);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("spam"))),
                "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(0, stored);
        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/wordCloud"),
                any(WordCloudUpdateMessage.class));
    }

    // ---- Drawing rounds ----

    /**
     * An in-bounds drawing submission is stored verbatim — the byte cap
     * is sized large enough to accept the payload (mocked ObjectMapper
     * returns a small byte array).
     */
    @Test
    void submitAnswer_OnDrawingRound_StoresInBoundsSubmission() throws Exception {
        Showcase session = drawingSessionWithOnePlayer(200, 500);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[1024]);

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0, 1.0, 1.0))));
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), answer), "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(1, stored);
    }

    /**
     * A submission whose stroke count exceeds the question's
     * {@code maxStrokesPerPlayer} is dropped before any storage or
     * broadcast. The byte cap path isn't hit because the count check
     * short-circuits first.
     */
    @Test
    void submitAnswer_OnDrawingRound_DropsWhenStrokeCountOverCap() {
        Showcase session = drawingSessionWithOnePlayer(2, 500);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0)),
                new Stroke("#000", 4.0, List.of(1.0, 1.0)),
                new Stroke("#000", 4.0, List.of(2.0, 2.0))));
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), answer), "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(0, stored);
    }

    /**
     * A submission whose serialized JSON exceeds the per-answer byte cap
     * is dropped. The mocked ObjectMapper reports an oversize byte array
     * so the cap path triggers without authoring a giant payload.
     */
    @Test
    void submitAnswer_OnDrawingRound_DropsWhenByteCapExceeded() throws Exception {
        Showcase session = drawingSessionWithOnePlayer(200, 500);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[300_000]);

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0, 1.0, 1.0))));
        showcaseService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), answer), "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(0, stored);
    }

    private static Showcase drawingSessionWithOnePlayer(int maxStrokes, int maxPointsPerStroke) {
        Showcase s = new Showcase();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(GameStatus.IN_PROGRESS);
        s.setPhase(ShowcasePhase.SUBMIT);
        ShowcaseSettings settings = new ShowcaseSettings();
        settings.setGameMode(GameMode.SIMULTANEOUS);
        settings.setTotalRounds(1);
        settings.setSpeedBonus(false);
        s.setSettings(settings);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(drawing("draw-0", maxStrokes, maxPointsPerStroke)));
        s.setPlayers(new ArrayList<>(List.of(player("p1"))));
        return s;
    }

    private static DrawingQuestion drawing(String id, int maxStrokes, int maxPointsPerStroke) {
        return new DrawingQuestion(
                id, "pub_" + id, "prv_" + id, "Title", null,
                "Sketch it", null,
                1920, 1080, maxStrokes, maxPointsPerStroke, List.of(),
                0, Difficulty.EASY,
                false, true, null,
                cephadex.brainflex.model.enums.ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE);
    }

    private static Showcase wordCloudSessionWithTwoPlayers(int maxSubmissionsPerPlayer) {
        Showcase s = new Showcase();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(GameStatus.IN_PROGRESS);
        s.setPhase(ShowcasePhase.SUBMIT);
        ShowcaseSettings settings = new ShowcaseSettings();
        settings.setGameMode(GameMode.SIMULTANEOUS);
        settings.setTotalRounds(1);
        settings.setSpeedBonus(false);
        s.setSettings(settings);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(wordCloud("wc-0", maxSubmissionsPerPlayer)));
        s.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
        return s;
    }

    private static WordCloudQuestion wordCloud(String id, int maxSubmissionsPerPlayer) {
        return new WordCloudQuestion(
                id, "pub_" + id, "prv_" + id, "Title", null,
                "How was your weekend?",
                maxSubmissionsPerPlayer, 30, false, true,
                List.of("spam"),
                0, Difficulty.EASY,
                false, true, null,
                cephadex.brainflex.model.enums.ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE);
    }

    private static PlayerAnswer answerOf(Showcase session, String userId, String elementId) {
        return session.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(elementId))
                .findFirst().orElseThrow();
    }

    private static Showcase bestAnswerSessionWithTwoPlayers() {
        Showcase s = new Showcase();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(GameStatus.IN_PROGRESS);
        s.setPhase(ShowcasePhase.SUBMIT);
        ShowcaseSettings settings = new ShowcaseSettings();
        settings.setGameMode(GameMode.SIMULTANEOUS);
        settings.setTotalRounds(1);
        settings.setSpeedBonus(false);
        s.setSettings(settings);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(bestAnswerMcq("ba-0", 50)));

        s.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
        return s;
    }

    private static Showcase bestAnswerSessionWithThreePlayers() {
        Showcase s = bestAnswerSessionWithTwoPlayers();
        s.getPlayers().add(player("p3"));
        return s;
    }

    private static ShowcasePlayer player(String userId) {
        ShowcasePlayer p = new ShowcasePlayer();
        p.setUserId(userId);
        p.setUserName(userId);
        p.setGuest(true);
        return p;
    }

    /** A best-answer-mode MCQ whose correct option is {id}-a. */
    private static McqQuestion bestAnswerMcq(String id, int bonus) {
        McqOption a = new McqOption(id + "-a", "A", null, null);
        McqOption b = new McqOption(id + "-b", "B", null, null);
        return new McqQuestion(
                id, "pub_" + id, "prv_" + id, "Prompt", null,
                "Prompt", List.of(a, b), List.of(a.id()),
                100, Difficulty.EASY,
                true, false, null, cephadex.brainflex.model.enums.ResponseMode.ACCEPTING_RESPONSES,
                true, null, bonus, null,
                15, null, null, null, null, null, MediaPosition.NONE);
    }
}
