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
import cephadex.brainflex.dto.ChatSendRequest;
import cephadex.brainflex.dto.TeamUpdateMessage;
import cephadex.brainflex.dto.ReactionBroadcastMessage;
import cephadex.brainflex.dto.ReactionSendRequest;
import cephadex.brainflex.dto.ShowcaseChatMessageDTO;
import cephadex.brainflex.model.Reaction;
import cephadex.brainflex.model.ShowcaseChatMessage;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ReactionRepository;
import cephadex.brainflex.repository.ShowcaseChatMessageRepository;
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
    @Mock private ReactionRepository reactionRepository;
    @Mock private ShowcaseChatMessageRepository chatRepository;
    @Mock private ShowcaseRateLimiter rateLimiter;
    @Mock private AvatarService avatarService;
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
                    15, null, null, null, null, null, MediaPosition.NONE,
                    true, false, 0,
                    null, null, null, null, List.of(),
                    null, null, true, 1));
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
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
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
                "deck1", null, 5, 20, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        Showcase result = showcaseService.createShowcase(host, request);

        assertEquals(5, result.getSettings().getTotalRounds());
        assertEquals(20, result.getSettings().getTimePerQuestion());
        assertEquals(5, result.getDeckSnapshot().size());
    }

    @Test
    void createShowcase_WithUnknownDeck_ThrowsNotFound() {
        when(deckRepository.findById("baddeck")).thenReturn(Optional.empty());

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "baddeck", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
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
                "empty", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
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
                15, null, null, null, null, null, MediaPosition.NONE,
                null, null, null, null, List.of(),
                null, null, true, 1);
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
                15, null, null, null, null, null, MediaPosition.NONE,
                null, null, null, null, List.of(),
                null, null, true, 1);
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
                15, null, null, null, null, null, MediaPosition.NONE,
                true, false, 0,
                null, null, null, null, List.of(),
                null, null, true, 1);
    }

    // ---- Audience engagement (chunk 11) ----

    @Test
    void acceptReaction_OnInProgressShowcase_PersistsAndBroadcasts() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(true);

        showcaseService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1");

        verify(reactionRepository).save(any(Reaction.class));
        verify(showcaseCache).incrementReactionCount("ABCD12", session.getDeckSnapshot().get(0).id(), "👍");
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/reaction"),
                any(ReactionBroadcastMessage.class));
    }

    @Test
    void acceptReaction_WhenShowcaseFlagOff_ThrowsForbidden() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        session.getSettings().setReactionsEnabled(false);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void acceptReaction_WithEmojiOffAllowList_ThrowsBadRequest() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptReaction("ABCD12", new ReactionSendRequest("🦄"), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptReaction_WhenRateLimited_ThrowsTooManyRequests() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void acceptChat_OnLobby_PersistsAndBroadcastsWithHostFlag() {
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.empty());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("host1"));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(true);

        // resolveUserId path for non-guest: principalName = googleId, returns user.id
        host.setGoogleId("google-host");
        when(userRepository.findByGoogleId("google-host")).thenReturn(Optional.of(host));

        ShowcaseChatMessageDTO dto = showcaseService.acceptChat(
                "ABCD12", new ChatSendRequest("hello world"), "google-host");

        assertTrue(dto.fromHost());
        assertEquals("hello world", dto.body());
        verify(chatRepository).save(any(ShowcaseChatMessage.class));
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/chat"),
                any(ShowcaseChatMessageDTO.class));
    }

    @Test
    void acceptChat_TrimsAndRejectsBlankBody() {
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.empty());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("p1"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptChat("ABCD12", new ChatSendRequest("   "), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptChat_RejectsBodyOver500Chars() {
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.empty());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("p1"));

        String longBody = "x".repeat(501);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptChat("ABCD12", new ChatSendRequest(longBody), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptChat_WhenShowcaseFlagOff_ThrowsForbidden() {
        lobbySession.getSettings().setChatEnabled(false);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.empty());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.acceptChat("ABCD12", new ChatSendRequest("hi"), "guest:p1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void moderateChatMessage_AsHost_FlipsModeratedAndBroadcasts() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        ShowcaseChatMessage row = new ShowcaseChatMessage();
        row.setId("msg1");
        row.setShowcaseId(session.getId());
        row.setBody("rude message");
        row.setAuthorUserId("p2");
        when(chatRepository.findById("msg1")).thenReturn(Optional.of(row));

        ShowcaseChatMessageDTO dto = showcaseService.moderateChatMessage("ABCD12", "msg1", "guest:p1");

        assertTrue(row.isModerated());
        assertEquals("p1", row.getModeratedByUserId());
        assertTrue(dto.moderated());
        assertEquals("(hidden by host)", dto.body());
        verify(chatRepository).save(row);
    }

    @Test
    void moderateChatMessage_AsNonHost_ThrowsForbidden() {
        Showcase session = bestAnswerSessionWithTwoPlayers();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.moderateChatMessage("ABCD12", "msg1", "guest:p2"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---- Team mode (chunk 12) ----

    @Test
    void createShowcase_WithTeamMode_SeedsDefaultTeamsAndAssignsHost() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(showcaseRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null,
                Boolean.TRUE, 3, null,
                null, null, null, null, null, null, null, null, null);
        Showcase result = showcaseService.createShowcase(host, request);

        assertTrue(result.isTeamMode());
        assertEquals(3, result.getTeams().size());
        // Host joined the smallest (first) team and became its captain.
        ShowcasePlayer hostPlayer = result.getPlayers().get(0);
        assertNotNull(hostPlayer.getTeamId());
        cephadex.brainflex.model.Team firstTeam = result.getTeams().get(0);
        assertEquals(hostPlayer.getTeamId(), firstTeam.getId());
        assertEquals(1, firstTeam.getMemberCount());
        assertEquals("host1", firstTeam.getCaptainUserId());
    }

    @Test
    void joinShowcase_InTeamMode_AutoBalancesIntoSmallestTeam() {
        Showcase session = teamSessionWithHostAlready();
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        Showcase result = showcaseService.joinShowcase("ABCD12", joiner);

        ShowcasePlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        // Host is on team 0 → smallest is team 1 → p2 joins team 1.
        assertEquals(result.getTeams().get(1).getId(), p2.getTeamId());
        assertEquals(1, result.getTeams().get(1).getMemberCount());
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void joinShowcase_InManualTeamMode_RejectsMissingTeamId() {
        Showcase session = teamSessionWithHostAlready();
        session.setAutoBalanceTeams(false);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.joinShowcase("ABCD12", joiner, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void joinShowcase_InManualTeamMode_HonorsValidTeamId() {
        Showcase session = teamSessionWithHostAlready();
        session.setAutoBalanceTeams(false);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        String pickTeamId = session.getTeams().get(1).getId();
        Showcase result = showcaseService.joinShowcase("ABCD12", joiner, pickTeamId);

        ShowcasePlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(pickTeamId, p2.getTeamId());
    }

    @Test
    void submitAnswer_InTeamMode_RecomputesTeamScoreAndBroadcasts() {
        Showcase session = teamModeSubmitSession();
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        showcaseService.submitAnswer("ABCD12", req, "guest:p1");

        ShowcasePlayer p1 = session.getPlayers().stream()
                .filter(p -> "p1".equals(p.getUserId())).findFirst().orElseThrow();
        cephadex.brainflex.model.Team team0 = session.getTeams().get(0);
        // Team score equals the single member's score.
        assertEquals(p1.getScore(), team0.getScore());
        assertTrue(team0.getScore() > 0);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void createTeam_AsHost_AppendsTeamAndBroadcasts() {
        Showcase session = teamSessionWithHostAlready();
        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(session);
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        int before = session.getTeams().size();
        Showcase result = showcaseService.createTeam("ABCD12", "Custom Crew", "pink", host);

        assertEquals(before + 1, result.getTeams().size());
        cephadex.brainflex.model.Team added = result.getTeams().get(result.getTeams().size() - 1);
        assertEquals("Custom Crew", added.getName());
        assertEquals("pink", added.getColor());
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void createTeam_WhenNotTeamMode_ThrowsConflict() {
        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(lobbySession);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.createTeam("ABCD12", "x", "blue", host));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void deleteTeam_ReassignsOrphanedPlayers() {
        Showcase session = teamSessionWithHostAlready();
        // Three teams so we can delete one and still have ≥2 remaining.
        session.getTeams().add(makeTeam("t3", "green"));
        ShowcasePlayer extra = player("player2");
        extra.setTeamId(session.getTeams().get(2).getId());
        session.getTeams().get(2).setMemberCount(1);
        session.getTeams().get(2).setCaptainUserId("player2");
        session.getPlayers().add(extra);

        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(session);
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        String removedTeamId = session.getTeams().get(2).getId();
        Showcase result = showcaseService.deleteTeam("ABCD12", removedTeamId, host);

        assertEquals(2, result.getTeams().size());
        // player2 was on the deleted team → reassigned to one of the remaining two.
        ShowcasePlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertNotNull(p2.getTeamId());
        assertFalse(removedTeamId.equals(p2.getTeamId()));
    }

    @Test
    void deleteTeam_WhenOnlyTwoTeams_ThrowsConflict() {
        Showcase session = teamSessionWithHostAlready();
        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(session);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.deleteTeam("ABCD12", session.getTeams().get(0).getId(), host));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void movePlayerToTeam_RelocatesAndRecomputesScores() {
        Showcase session = teamSessionWithHostAlready();
        // Add a second player on team 1 so we can move them to team 0.
        ShowcasePlayer p2 = player("player2");
        p2.setScore(70);
        p2.setTeamId(session.getTeams().get(1).getId());
        session.getTeams().get(1).setMemberCount(1);
        session.getTeams().get(1).setScore(70);
        session.getTeams().get(1).setCaptainUserId("player2");
        session.getPlayers().add(p2);
        // Host has a score of 30.
        session.getPlayers().get(0).setScore(30);
        session.getTeams().get(0).setScore(30);

        when(authorizationService.requireShowcaseHost("ABCD12", host)).thenReturn(session);
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        Showcase result = showcaseService.movePlayerToTeam(
                "ABCD12", "player2", session.getTeams().get(0).getId(), host);

        ShowcasePlayer movedP2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(result.getTeams().get(0).getId(), movedP2.getTeamId());
        assertEquals(100, result.getTeams().get(0).getScore()); // 30 + 70
        assertEquals(0, result.getTeams().get(1).getScore());
        assertEquals(2, result.getTeams().get(0).getMemberCount());
        assertEquals(0, result.getTeams().get(1).getMemberCount());
    }

    @Test
    void endGame_InTeamMode_StampsTeamIdOnEveryPlacement() {
        Showcase session = teamModeSubmitSession();
        session.getPlayers().get(0).setScore(120);
        cephadex.brainflex.model.Team team0 = session.getTeams().get(0);
        team0.setScore(120);
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        // Use the public end-early path which calls endGame internally.
        showcaseService.endShowcaseEarly("ABCD12", "guest:p1");

        verify(showcaseResultRepository).save(argThat(r -> {
            if (!(r instanceof ShowcaseResult res)) return false;
            return res.getPlacements().stream().allMatch(pp -> pp.getTeamId() != null);
        }));
    }

    private cephadex.brainflex.model.Team makeTeam(String id, String color) {
        cephadex.brainflex.model.Team t = new cephadex.brainflex.model.Team();
        t.setId(id);
        t.setColor(color);
        t.setName("Team " + id);
        return t;
    }

    /** Two-team team-mode session with the host already on team 0. */
    private Showcase teamSessionWithHostAlready() {
        Showcase s = new Showcase();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("host1");
        s.setStatus(GameStatus.LOBBY);
        ShowcaseSettings settings = new ShowcaseSettings();
        settings.setTeamMode(true);
        settings.setAutoBalanceTeams(true);
        s.setSettings(settings);
        s.setTeamMode(true);
        s.setAutoBalanceTeams(true);

        cephadex.brainflex.model.Team t0 = makeTeam("t1", "red");
        cephadex.brainflex.model.Team t1 = makeTeam("t2", "blue");
        s.setTeams(new ArrayList<>(List.of(t0, t1)));

        ShowcasePlayer hostPlayer = player("host1");
        hostPlayer.setTeamId(t0.getId());
        t0.setMemberCount(1);
        t0.setCaptainUserId("host1");
        s.setPlayers(new ArrayList<>(List.of(hostPlayer)));
        return s;
    }

    /** In-progress single-MCQ session with two teams; "p1" is on team 0. */
    private Showcase teamModeSubmitSession() {
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
        settings.setTeamMode(true);
        s.setSettings(settings);
        s.setTeamMode(true);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(sampleElements(1).get(0)));

        cephadex.brainflex.model.Team t0 = makeTeam("t1", "red");
        cephadex.brainflex.model.Team t1 = makeTeam("t2", "blue");
        s.setTeams(new ArrayList<>(List.of(t0, t1)));

        ShowcasePlayer p1 = player("p1");
        p1.setTeamId(t0.getId());
        t0.setMemberCount(1);
        t0.setCaptainUserId("p1");
        s.setPlayers(new ArrayList<>(List.of(p1)));
        return s;
    }

    // ---- Chunk 10: per-player deterministic shuffle on round start ----

    @Test
    void startGame_WithMcqShuffleEnabled_SendsPersonalizedRoundStartToEachPlayer() {
        host.setIsGuest(true); // simplify resolveUserId path (no userRepository lookup)
        lobbySession.setHostUserId("host1");

        // First element opts into shuffleOptions (sampleElements builds with shuffleOptions=true).
        DeckElement first = sampleElements(1).get(0);
        lobbySession.setDeckSnapshot(List.of(first));

        ShowcasePlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        ShowcasePlayer p2 = player("p2");
        p2.setPrincipalName("guest:p2");
        lobbySession.setPlayers(new ArrayList<>(List.of(p1, p2)));

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        showcaseService.startGame("ABCD12", "guest:host1");

        // Canonical topic broadcast still fires for the host/audience view.
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        // Each player receives a personalized RoundStartMessage on their user queue.
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("guest:p1"),
                org.mockito.ArgumentMatchers.eq("/queue/showcase/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("guest:p2"),
                org.mockito.ArgumentMatchers.eq("/queue/showcase/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
    }

    @Test
    void startGame_WithShuffleDisabled_SkipsPerUserBroadcast() {
        host.setIsGuest(true);
        lobbySession.setHostUserId("host1");

        // Build a non-shuffling MCQ — same shape as sampleElements but with shuffleOptions=false.
        McqOption a = new McqOption("nos-a", "A", null, null);
        McqOption b = new McqOption("nos-b", "B", null, null);
        DeckElement first = new McqQuestion(
                "nos", "pub", "priv", "Prompt", null,
                "Prompt", List.of(a, b), List.of(a.id()),
                100, Difficulty.EASY,
                true, false, null, cephadex.brainflex.model.enums.ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE,
                false, false, 0, // shuffleOptions=false
                null, null, null, null, List.of(),
                null, null, true, 1);
        lobbySession.setDeckSnapshot(List.of(first));

        ShowcasePlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        lobbySession.setPlayers(new ArrayList<>(List.of(p1)));

        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        showcaseService.startGame("ABCD12", "guest:host1");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/showcase/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), anyString(), any(cephadex.brainflex.dto.RoundStartMessage.class));
    }

    // ---- Chunk 13 ----

    @Test
    void createShowcase_WithCustomRoomCode_HonorsCode() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(showcaseRepository.findByRoomCode("PARTY1")).thenReturn(Optional.empty());
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "PARTY1", null, null, null, null, null, null, null, null);
        Showcase result = showcaseService.createShowcase(host, request);

        assertEquals("PARTY1", result.getRoomCode());
        assertEquals("PARTY1", result.getCustomRoomCode());
    }

    @Test
    void createShowcase_WithCustomRoomCodeCollision_ThrowsConflict() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        // The collision-check path is the only call to findByRoomCode in this
        // path; return a placeholder session to simulate "code already in use".
        Showcase taken = new Showcase();
        taken.setRoomCode("PARTY1");
        when(showcaseRepository.findByRoomCode("PARTY1")).thenReturn(Optional.of(taken));

        CreateShowcaseRequest request = new CreateShowcaseRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "PARTY1", null, null, null, null, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.createShowcase(host, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void submitAnswer_PopulatesTimingAndStreakFields() {
        Showcase session = readySession();
        ShowcasePlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        session.setPlayers(new ArrayList<>(List.of(p1)));
        session.setRoundStartedAt(java.time.LocalDateTime.now().minusSeconds(2));
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        showcaseService.submitAnswer("ABCD12", req, "guest:p1");

        PlayerAnswer ans = p1.getAnswers().get(0);
        assertTrue(ans.isCorrect());
        // Roughly 2 seconds elapsed since round start.
        assertTrue(ans.getTimeTakenMs() >= 1500L, "timeTakenMs should reflect elapsed time");
        // Streak captured BEFORE the correct answer was applied (so 0 here).
        assertEquals(0, ans.getStreakBeforeAnswer());
        // After this correct answer, currentStreak should be 1, longestStreak 1.
        assertEquals(1, p1.getCurrentStreak());
        assertEquals(1, p1.getLongestStreak());
        assertEquals(1.0, p1.getAccuracy(), 0.0001);
    }

    @Test
    void submitAnswer_WrongAnswer_ResetsStreakAndDoesNotBumpLongest() {
        Showcase session = readySession();
        ShowcasePlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        p1.setCurrentStreak(3);
        p1.setLongestStreak(3);
        session.setPlayers(new ArrayList<>(List.of(p1)));
        session.setRoundStartedAt(java.time.LocalDateTime.now());
        when(showcaseCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        // Pick the second option (B) which is not the correct one (A).
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-b")));
        showcaseService.submitAnswer("ABCD12", req, "guest:p1");

        PlayerAnswer ans = p1.getAnswers().get(0);
        assertFalse(ans.isCorrect());
        assertEquals(3, ans.getStreakBeforeAnswer());
        assertEquals(0, p1.getCurrentStreak());
        // Longest still 3 — we only bump it on the way up, never down.
        assertEquals(3, p1.getLongestStreak());
        assertEquals(0.0, p1.getAccuracy(), 0.0001);
    }

    @Test
    void joinShowcase_WithValidAvatarKey_SetsAvatarAndDerivesColor() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(avatarService.has("fox-orange")).thenReturn(true);
        when(avatarService.get("fox-orange")).thenReturn(
                new AvatarService.AvatarPreset("fox-orange", "Fox", "/x/fox.svg", "orange"));

        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setIsGuest(false);

        Showcase result = showcaseService.joinShowcase("ABCD12", newPlayer, null, "fox-orange", null);
        ShowcasePlayer joined = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals("fox-orange", joined.getAvatarKey());
        assertEquals("orange", joined.getColorTag());
    }

    @Test
    void joinShowcase_WithUnknownAvatar_SilentlyDropsKey() {
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(showcaseRepository.save(any(Showcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(avatarService.has("not-a-real-preset")).thenReturn(false);

        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setIsGuest(false);

        Showcase result = showcaseService.joinShowcase("ABCD12", newPlayer, null, "not-a-real-preset", null);
        ShowcasePlayer joined = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(null, joined.getAvatarKey());
    }

    @Test
    void joinShowcase_WhenRequireFullNameAndGuest_ThrowsForbidden() {
        lobbySession.getSettings().setRequireFullName(true);
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("guest1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> showcaseService.joinShowcase("ABCD12", guest));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Stripped-down session ready to accept a submit (IN_PROGRESS, 1 element). */
    private Showcase readySession() {
        Showcase s = new Showcase();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("host1");
        s.setStatus(GameStatus.IN_PROGRESS);
        s.setPhase(ShowcasePhase.SUBMIT);
        s.setSettings(new ShowcaseSettings());
        s.setCurrentRound(0);
        s.setDeckSnapshot(sampleElements(1));
        s.setPlayers(new ArrayList<>());
        return s;
    }
}
