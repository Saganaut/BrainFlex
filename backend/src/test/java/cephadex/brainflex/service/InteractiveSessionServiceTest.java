/**
 * Unit tests for InteractiveSessionService state machine and session management.
 * Verifies createInteractiveSession, joinInteractiveSession, cancelInteractiveSession, and getByRoomCode
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

import cephadex.brainflex.model.enums.BestAnswerScoring;
import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.CreateInteractiveSessionRequest;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.VotePhaseStartMessage;
import cephadex.brainflex.dto.VoteSubmitRequest;
import cephadex.brainflex.dto.WordCloudUpdateMessage;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionPlayer;
import cephadex.brainflex.model.InteractiveSessionResult;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserSnapshot;
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
import cephadex.brainflex.model.enums.AnswerSubmissionMode;
import cephadex.brainflex.model.enums.InteractiveSessionStatus;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.InteractiveSessionPhase;
import cephadex.brainflex.dto.ChatSendRequest;
import cephadex.brainflex.dto.TeamUpdateMessage;
import cephadex.brainflex.dto.ReactionBroadcastMessage;
import cephadex.brainflex.dto.ReactionSendRequest;
import cephadex.brainflex.dto.InteractiveSessionChatMessageDTO;
import cephadex.brainflex.model.Reaction;
import cephadex.brainflex.model.InteractiveSessionChatMessage;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ReactionRepository;
import cephadex.brainflex.repository.InteractiveSessionChatMessageRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.InteractiveSessionResultRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InteractiveSessionServiceTest {

    @Mock private InteractiveSessionRepository interactiveSessionRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private InteractiveSessionResultRepository interactiveSessionResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private OAuthProviderService oAuthProviderService;
    @Mock private InteractiveSessionCacheService interactiveSessionCache;
    @Mock private AuthorizationService authorizationService;
    @Mock private DeckImageHydrationService deckImageHydrationService;
    @Mock private DeckService deckService;
    @Mock private UserImageHydrator userImageHydrator;
    @Mock private ReactionRepository reactionRepository;
    @Mock private InteractiveSessionChatMessageRepository chatRepository;
    @Mock private InteractiveSessionRateLimiter rateLimiter;
    @Mock private AvatarService avatarService;
    @Mock private GameHistoryService gameHistoryService;
    @Mock private DeckAnalyticsService deckAnalyticsService;
    @Mock private AchievementService achievementService;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private org.springframework.context.ApplicationEventPublisher events;
    @org.mockito.Spy
    private cephadex.brainflex.service.bestanswer.BestAnswerScoringRegistry bestAnswerScoringRegistry =
            new cephadex.brainflex.service.bestanswer.BestAnswerScoringRegistry(java.util.List.of(
                    new cephadex.brainflex.service.bestanswer.PointsPerVoteStrategy(),
                    new cephadex.brainflex.service.bestanswer.FlatWinnerStrategy()));
    @org.mockito.Spy
    private ShowResponsesResolver showResponsesResolver = new ShowResponsesResolver();

    @InjectMocks
    private InteractiveSessionService interactiveSessionService;

    private User host;
    private Deck deck;
    private InteractiveSession lobbySession;

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

        lobbySession = new InteractiveSession();
        lobbySession.setId("session1");
        lobbySession.setRoomCode("ABCD12");
        lobbySession.setHostUserId("host1");
        lobbySession.setStatus(InteractiveSessionStatus.LOBBY);
        lobbySession.setSettings(new InteractiveSessionSettings());
        lobbySession.setPlayers(new ArrayList<>());
    }

    private static List<DeckElement> sampleElements(int count) {
        List<DeckElement> els = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = "el-" + i;
            McqOption a = new McqOption(id + "-a", "A", null, null);
            McqOption b = new McqOption(id + "-b", "B", null, null);
            els.add(new McqQuestion(
                    id,
                    "Prompt " + i, List.of(a, b), List.of(a.id()),
                    100, Difficulty.EASY, null,
                    true, false, 0,
                    TestElementChromes.scored(id, "Prompt " + i)));
        }
        return els;
    }

    // ---- createInteractiveSession ----

    @Test
    void createInteractiveSession_WithValidDeck_CreatesAndReturnsSession() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(interactiveSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        InteractiveSession result = interactiveSessionService.createInteractiveSession(host, request);

        assertNotNull(result);
        assertNotNull(result.getRoomCode());
        assertEquals(InteractiveSessionStatus.LOBBY, result.getStatus());
        assertEquals("host1", result.getHostUserId());
        assertEquals(1, result.getPlayers().size());
        assertEquals(10, result.getDeckSnapshot().size()); // totalRounds default
        verify(interactiveSessionCache).put(any(InteractiveSession.class));
    }

    @Test
    void createInteractiveSession_WithCustomRounds_AppliesSettings() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(interactiveSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "deck1", null, null, null, 5, 20, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        InteractiveSession result = interactiveSessionService.createInteractiveSession(host, request);

        assertEquals(5, result.getSettings().getTotalRounds());
        assertEquals(20, result.getSettings().getTimePerQuestion());
        assertEquals(5, result.getDeckSnapshot().size());
    }

    @Test
    void createInteractiveSession_WithUnknownDeck_ThrowsNotFound() {
        when(deckRepository.findById("baddeck")).thenReturn(Optional.empty());

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "baddeck", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.createInteractiveSession(host, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createInteractiveSession_WhenDeckEmpty_ThrowsUnprocessable() {
        Deck empty = new Deck();
        empty.setId("empty");
        empty.setElements(new ArrayList<>());
        when(deckRepository.findById("empty")).thenReturn(Optional.of(empty));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "empty", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.createInteractiveSession(host, request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
    }

    // ---- getByRoomCode ----

    @Test
    void getByRoomCode_WhenExists_ReturnsSession() {
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        InteractiveSession result = interactiveSessionService.getByRoomCode("ABCD12");
        assertEquals("ABCD12", result.getRoomCode());
    }

    @Test
    void getByRoomCode_WhenNotFound_ThrowsNotFound() {
        when(interactiveSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.getByRoomCode("XXXXXX"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- joinInteractiveSession ----

    @Test
    void joinInteractiveSession_WhenLobbyAndSpaceAvailable_AddsPlayer() {
        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setUserName("newguy");
        newPlayer.setIsGuest(false);

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", newPlayer);

        assertEquals(1, result.getPlayers().size());
        assertEquals("player2", result.getPlayers().get(0).getUserId());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void joinInteractiveSession_WhenAlreadyJoined_ReturnsExistingSession() {
        InteractiveSessionPlayer existing = new InteractiveSessionPlayer();
        existing.setUser(UserSnapshot.of("player2", null));
        lobbySession.getPlayers().add(existing);

        User returning = new User();
        returning.setId("player2");
        returning.setIsGuest(false);

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", returning);

        assertEquals(1, result.getPlayers().size());
        verify(interactiveSessionRepository, never()).save(any());
    }

    @Test
    void joinInteractiveSession_WhenGameAlreadyStarted_ThrowsConflict() {
        lobbySession.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User player = new User();
        player.setId("p3");
        player.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.joinInteractiveSession("ABCD12", player));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinInteractiveSession_WhenGuestAndGuestsDisabled_ThrowsForbidden() {
        lobbySession.getSettings().setAllowGuests(false);
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("g1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.joinInteractiveSession("ABCD12", guest));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinInteractiveSession_WhenSessionFull_ThrowsConflict() {
        lobbySession.getSettings().setMaxPlayers(1);
        InteractiveSessionPlayer existing = new InteractiveSessionPlayer();
        existing.setUser(UserSnapshot.of("someone", null));
        lobbySession.getPlayers().add(existing);

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User latePlayer = new User();
        latePlayer.setId("late");
        latePlayer.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.joinInteractiveSession("ABCD12", latePlayer));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- cancelInteractiveSession ----

    @Test
    void cancelInteractiveSession_AsHost_SetsStatusToCancelled() {
        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(lobbySession);
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        interactiveSessionService.cancelInteractiveSession("ABCD12", host);

        verify(interactiveSessionRepository).save(argThat(s -> s.getStatus() == InteractiveSessionStatus.CANCELLED));
        verify(interactiveSessionCache).evict("ABCD12");
    }

    @Test
    void cancelInteractiveSession_AsNonHost_ThrowsForbidden() {
        User nonHost = new User();
        nonHost.setId("other");
        when(authorizationService.requireInteractiveSessionHost("ABCD12", nonHost))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.cancelInteractiveSession("ABCD12", nonHost));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelInteractiveSession_WhenAlreadyFinished_ThrowsConflict() {
        lobbySession.setStatus(InteractiveSessionStatus.FINISHED);
        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(lobbySession);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.cancelInteractiveSession("ABCD12", host));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- getResults ----

    @Test
    void getResults_WhenExists_ReturnsResult() {
        InteractiveSessionResult result = new InteractiveSessionResult();
        result.setId("result1");
        result.setInteractiveSessionId("session1");

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionResultRepository.findByInteractiveSessionId("session1")).thenReturn(Optional.of(result));

        Optional<InteractiveSessionResult> found = interactiveSessionService.getResults("ABCD12");
        assertTrue(found.isPresent());
        assertEquals("result1", found.get().getId());
    }

    @Test
    void getResults_WhenNoResult_ReturnsEmpty() {
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionResultRepository.findByInteractiveSessionId("session1")).thenReturn(Optional.empty());

        Optional<InteractiveSessionResult> found = interactiveSessionService.getResults("ABCD12");
        assertFalse(found.isPresent());
    }

    // ---- Best Answer phase machine ----

    /** Two players, one Best-Answer MCQ at index 0, both submit → expect VOTE phase. */
    @Test
    void submitAnswer_OnBestAnswerRound_WhenAllAnswered_TransitionsToVotePhase() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));

        interactiveSessionService.submitAnswer("ABCD12", req, "guest:p1");
        interactiveSessionService.submitAnswer("ABCD12", req, "guest:p2");

        assertEquals(InteractiveSessionPhase.VOTE, session.getPhase());

        // Each submitter has a submissionId; no timeouts means two anonymized
        // submissions go out on the votePhase channel.
        long submissionsWithId = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getSubmissionId() != null).count();
        assertEquals(2, submissionsWithId);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/votePhase"),
                any(VotePhaseStartMessage.class));
    }

    /**
     * Vote tally: p1 submission gets two votes, p2 gets one → p1 wins, gets the
     * bonus on top of their answer points.
     */
    @Test
    void submitVote_WhenAllVoted_AwardsBonusToWinnerAndAdvances() {
        InteractiveSession session = bestAnswerSessionWithThreePlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest answerReq = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        interactiveSessionService.submitAnswer("ABCD12", answerReq, "guest:p1");
        interactiveSessionService.submitAnswer("ABCD12", answerReq, "guest:p2");
        interactiveSessionService.submitAnswer("ABCD12", answerReq, "guest:p3");

        // After SUBMIT phase completes we're now in VOTE; grab the players'
        // submissionIds so the votes reference real submissions.
        String p1Sub = answerOf(session, "p1", el.id()).getSubmissionId();
        String p2Sub = answerOf(session, "p2", el.id()).getSubmissionId();

        // p1 and p3 both vote for p1's submission; p2 votes for p2 (themselves).
        interactiveSessionService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p1Sub), "guest:p1");
        interactiveSessionService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p2Sub), "guest:p2");
        interactiveSessionService.submitVote("ABCD12", new VoteSubmitRequest(el.id(), p1Sub), "guest:p3");

        // p1's submission won → bestAnswerWinner flag + per-vote bonus applied.
        // Chunk 24: default POINTS_PER_VOTE strategy means winner earns
        // votesReceived (2) × bestAnswerPoints (50) = 100, on top of the
        // 100 base for getting the question right → 200 total.
        PlayerAnswer p1Answer = answerOf(session, "p1", el.id());
        assertTrue(p1Answer.isBestAnswerWinner());
        InteractiveSessionPlayer p1 = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals("p1")).findFirst().orElseThrow();
        assertEquals(200, p1.getScore());

        // p2 got 1 vote (from themselves) — under POINTS_PER_VOTE that still
        // earns the per-vote award. 100 base (correct answer) + 1*50 = 150.
        InteractiveSessionPlayer p2 = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals("p2")).findFirst().orElseThrow();
        assertTrue(answerOf(session, "p2", el.id()).isBestAnswerWinner());
        assertEquals(150, p2.getScore());

        // Reveal broadcast carries BestAnswerOutcome with both vote-getters in
        // the winners list (POINTS_PER_VOTE rewards anyone who got ≥1 vote).
        // bonusAwarded surfaces the largest single-player award.
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/roundResult"),
                argThat((Object msg) -> {
                    if (!(msg instanceof RoundResultMessage rr)) return false;
                    if (rr.bestAnswer() == null) return false;
                    return rr.bestAnswer().winnerUserIds().contains("p1")
                            && rr.bestAnswer().winnerUserIds().contains("p2")
                            && rr.bestAnswer().bonusAwarded() == 100;
                }));
    }

    /** A stale vote arriving in SUBMIT phase is silently dropped. */
    @Test
    void submitVote_WhenNotInVotePhase_IsIgnored() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        // Stays in SUBMIT — no answers submitted yet.
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitVote("ABCD12",
                new VoteSubmitRequest(el.id(), "any-sub-id"),
                "guest:p1");

        // No vote recorded; no broadcast emitted.
        assertTrue(session.getPlayers().stream().allMatch(p -> p.getVotes().isEmpty()));
        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/voted"),
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
        InteractiveSession session = wordCloudSessionWithTwoPlayers(2);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("Monday", "rainy"))),
                "guest:p1");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/wordCloud"),
                argThat((Object msg) -> msg instanceof WordCloudUpdateMessage w
                        && w.elementId().equals(el.id())
                        && w.counts().getOrDefault("monday", 0) == 1
                        && w.counts().getOrDefault("rainy", 0) == 1));
    }

    /**
     * Chunk 21 A.6 — when the runtime showResponses cascade resolves to
     * PRIVATE (here via deck-level defaultShowResponses), the live aggregate
     * broadcast in submitAnswer is suppressed. answerProgress still fires
     * because it carries no payload data.
     */
    @Test
    void submitAnswer_OnWordCloudRound_WhenShowResponsesPrivate_DoesNotBroadcastWordCloud() {
        InteractiveSession session = wordCloudSessionWithTwoPlayers(2);
        session.setDeckId("deck-private");
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        Deck privateDeck = new Deck();
        privateDeck.setId("deck-private");
        privateDeck.setDefaultShowResponses(cephadex.brainflex.model.enums.ShowResponsesMode.PRIVATE);
        when(deckRepository.findById("deck-private")).thenReturn(Optional.of(privateDeck));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("Monday", "rainy"))),
                "guest:p1");

        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/wordCloud"),
                any(Object.class));
        // answerProgress is still broadcast — it's a counter, not aggregated answers.
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/answered"),
                any(Object.class));
    }

    /**
     * Case folding + banned-words filtering happens at submit time so the
     * historical PlayerAnswer.payload matches the aggregator's view.
     */
    @Test
    void submitAnswer_OnWordCloudRound_NormalizesPayloadBeforeStoring() {
        InteractiveSession session = wordCloudSessionWithTwoPlayers(3);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitAnswer("ABCD12",
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
        InteractiveSession session = wordCloudSessionWithTwoPlayers(2);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(interactiveSessionRepository.save(any(InteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("Monday"))), "guest:p1");
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("monday"))), "guest:p2");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/roundResult"),
                argThat((Object msg) -> msg instanceof RoundResultMessage rr
                        && rr.playerResults().stream().allMatch(r -> r.payload() == null)));
    }

    /**
     * An empty submission (all words filtered out by the banned list) is a
     * silent no-op — no PlayerAnswer is stored and no broadcast goes out.
     */
    @Test
    void submitAnswer_OnWordCloudRound_DroppedWhenEntirelyBanned() {
        InteractiveSession session = wordCloudSessionWithTwoPlayers(3);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), new WordCloudAnswer(List.of("spam"))),
                "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(0, stored);
        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/wordCloud"),
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
        InteractiveSession session = drawingSessionWithOnePlayer(200, 500);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[1024]);

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0, 1.0, 1.0))));
        interactiveSessionService.submitAnswer("ABCD12",
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
        InteractiveSession session = drawingSessionWithOnePlayer(2, 500);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0)),
                new Stroke("#000", 4.0, List.of(1.0, 1.0)),
                new Stroke("#000", 4.0, List.of(2.0, 2.0))));
        interactiveSessionService.submitAnswer("ABCD12",
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
        InteractiveSession session = drawingSessionWithOnePlayer(200, 500);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[300_000]);

        DeckElement el = session.getDeckSnapshot().get(0);
        DrawingAnswer answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0, 1.0, 1.0))));
        interactiveSessionService.submitAnswer("ABCD12",
                new AnswerSubmitRequest(el.id(), answer), "guest:p1");

        long stored = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(el.id()))
                .count();
        assertEquals(0, stored);
    }

    private static InteractiveSession drawingSessionWithOnePlayer(int maxStrokes, int maxPointsPerStroke) {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        s.setPhase(InteractiveSessionPhase.SUBMIT);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setAnswerSubmissionMode(AnswerSubmissionMode.SIMULTANEOUS);
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
                id,
                "Sketch it", null,
                1920, 1080, maxStrokes, maxPointsPerStroke, List.of(),
                0, Difficulty.EASY, null,
                TestElementChromes.survey(id, "Sketch it"));
    }

    private static InteractiveSession wordCloudSessionWithTwoPlayers(int maxSubmissionsPerPlayer) {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        s.setPhase(InteractiveSessionPhase.SUBMIT);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setAnswerSubmissionMode(AnswerSubmissionMode.SIMULTANEOUS);
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
                id,
                "How was your weekend?",
                maxSubmissionsPerPlayer, 30, false, true,
                List.of("spam"),
                0, Difficulty.EASY, null,
                TestElementChromes.survey(id, "How was your weekend?"));
    }

    private static PlayerAnswer answerOf(InteractiveSession session, String userId, String elementId) {
        return session.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> a.getElementId().equals(elementId))
                .findFirst().orElseThrow();
    }

    private static InteractiveSession bestAnswerSessionWithTwoPlayers() {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        s.setPhase(InteractiveSessionPhase.SUBMIT);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setAnswerSubmissionMode(AnswerSubmissionMode.SIMULTANEOUS);
        settings.setTotalRounds(1);
        settings.setSpeedBonus(false);
        s.setSettings(settings);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(bestAnswerMcq("ba-0", 50)));

        s.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
        return s;
    }

    private static InteractiveSession bestAnswerSessionWithThreePlayers() {
        InteractiveSession s = bestAnswerSessionWithTwoPlayers();
        s.getPlayers().add(player("p3"));
        return s;
    }

    private static InteractiveSessionPlayer player(String userId) {
        InteractiveSessionPlayer p = new InteractiveSessionPlayer();
        p.setUser(UserSnapshot.of(userId, userId, null, true));
        return p;
    }

    /** A best-answer-mode MCQ whose correct option is {id}-a. */
    private static McqQuestion bestAnswerMcq(String id, int bonus) {
        McqOption a = new McqOption(id + "-a", "A", null, null);
        McqOption b = new McqOption(id + "-b", "B", null, null);
        return new McqQuestion(
                id,
                "Prompt", List.of(a, b), List.of(a.id()),
                100, Difficulty.EASY, null,
                true, false, 0,
                TestElementChromes.chrome(id, "Prompt",
                        true, false, null, 15,
                        true, null, bonus, BestAnswerScoring.POINTS_PER_VOTE));
    }

    // ---- Audience engagement (chunk 11) ----

    @Test
    void acceptReaction_OnInProgressInteractiveSession_PersistsAndBroadcasts() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(true);

        interactiveSessionService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1");

        verify(reactionRepository).save(any(Reaction.class));
        verify(interactiveSessionCache).incrementReactionCount("ABCD12", session.getDeckSnapshot().get(0).id(), "👍");
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/reaction"),
                any(ReactionBroadcastMessage.class));
    }

    @Test
    void acceptReaction_WhenInteractiveSessionFlagOff_ThrowsForbidden() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        session.getSettings().setReactionsEnabled(false);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void acceptReaction_WithEmojiOffAllowList_ThrowsBadRequest() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptReaction("ABCD12", new ReactionSendRequest("🦄"), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptReaction_WhenRateLimited_ThrowsTooManyRequests() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptReaction("ABCD12", new ReactionSendRequest("👍"), "guest:p1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void acceptChat_OnLobby_PersistsAndBroadcastsWithHostFlag() {
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.empty());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("host1"));
        when(rateLimiter.allow(any(), any(), any())).thenReturn(true);

        // resolveUserId path for non-guest: principalName is the provider id,
        // OAuthProviderService.findByAnyProviderId routes it back to the user.
        host.setGoogleId("google-host");
        when(oAuthProviderService.findByAnyProviderId("google-host")).thenReturn(Optional.of(host));

        InteractiveSessionChatMessageDTO dto = interactiveSessionService.acceptChat(
                "ABCD12", new ChatSendRequest("hello world"), "google-host");

        assertTrue(dto.fromHost());
        assertEquals("hello world", dto.body());
        verify(chatRepository).save(any(InteractiveSessionChatMessage.class));
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/chat"),
                any(InteractiveSessionChatMessageDTO.class));
    }

    @Test
    void acceptChat_TrimsAndRejectsBlankBody() {
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.empty());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("p1"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptChat("ABCD12", new ChatSendRequest("   "), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptChat_RejectsBodyOver500Chars() {
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.empty());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        lobbySession.getPlayers().add(player("p1"));

        String longBody = "x".repeat(501);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptChat("ABCD12", new ChatSendRequest(longBody), "guest:p1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptChat_WhenInteractiveSessionFlagOff_ThrowsForbidden() {
        lobbySession.getSettings().setChatEnabled(false);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.empty());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.acceptChat("ABCD12", new ChatSendRequest("hi"), "guest:p1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void moderateChatMessage_AsHost_FlipsModeratedAndBroadcasts() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        InteractiveSessionChatMessage row = new InteractiveSessionChatMessage();
        row.setId("msg1");
        row.setInteractiveSessionId(session.getId());
        row.setBody("rude message");
        row.setAuthor(UserSnapshot.of("p2", null));
        when(chatRepository.findById("msg1")).thenReturn(Optional.of(row));

        InteractiveSessionChatMessageDTO dto = interactiveSessionService.moderateChatMessage("ABCD12", "msg1", "guest:p1");

        assertTrue(row.isModerated());
        assertEquals("p1", row.getModeratedByUserId());
        assertTrue(dto.moderated());
        assertEquals("(hidden by host)", dto.body());
        verify(chatRepository).save(row);
    }

    @Test
    void moderateChatMessage_AsNonHost_ThrowsForbidden() {
        InteractiveSession session = bestAnswerSessionWithTwoPlayers();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.moderateChatMessage("ABCD12", "msg1", "guest:p2"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---- Team mode (chunk 12) ----

    @Test
    void createInteractiveSession_WithTeamMode_SeedsDefaultTeamsAndAssignsHost() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(interactiveSessionRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null,
                Boolean.TRUE, 3, null,
                null, null, null, null, null, null, null, null, null);
        InteractiveSession result = interactiveSessionService.createInteractiveSession(host, request);

        assertTrue(result.getSettings().isTeamMode());
        assertEquals(3, result.getTeams().size());
        // Host joined the smallest (first) team and became its captain.
        InteractiveSessionPlayer hostPlayer = result.getPlayers().get(0);
        assertNotNull(hostPlayer.getTeamId());
        cephadex.brainflex.model.Team firstTeam = result.getTeams().get(0);
        assertEquals(hostPlayer.getTeamId(), firstTeam.getId());
        assertEquals(1, firstTeam.getMemberCount());
        assertEquals("host1", firstTeam.getCaptainUserId());
    }

    @Test
    void joinInteractiveSession_InTeamMode_AutoBalancesIntoSmallestTeam() {
        InteractiveSession session = teamSessionWithHostAlready();
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", joiner);

        InteractiveSessionPlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        // Host is on team 0 → smallest is team 1 → p2 joins team 1.
        assertEquals(result.getTeams().get(1).getId(), p2.getTeamId());
        assertEquals(1, result.getTeams().get(1).getMemberCount());
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void joinInteractiveSession_InManualTeamMode_RejectsMissingTeamId() {
        InteractiveSession session = teamSessionWithHostAlready();
        session.getSettings().setAutoBalanceTeams(false);
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.joinInteractiveSession("ABCD12", joiner, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void joinInteractiveSession_InManualTeamMode_HonorsValidTeamId() {
        InteractiveSession session = teamSessionWithHostAlready();
        session.getSettings().setAutoBalanceTeams(false);
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(session));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        User joiner = new User();
        joiner.setId("player2");
        joiner.setIsGuest(false);

        String pickTeamId = session.getTeams().get(1).getId();
        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", joiner, pickTeamId);

        InteractiveSessionPlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(pickTeamId, p2.getTeamId());
    }

    @Test
    void submitAnswer_InTeamMode_RecomputesTeamScoreAndBroadcasts() {
        InteractiveSession session = teamModeSubmitSession();
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        interactiveSessionService.submitAnswer("ABCD12", req, "guest:p1");

        InteractiveSessionPlayer p1 = session.getPlayers().stream()
                .filter(p -> "p1".equals(p.getUserId())).findFirst().orElseThrow();
        cephadex.brainflex.model.Team team0 = session.getTeams().get(0);
        // Team score equals the single member's score.
        assertEquals(p1.getScore(), team0.getScore());
        assertTrue(team0.getScore() > 0);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void createTeam_AsHost_AppendsTeamAndBroadcasts() {
        InteractiveSession session = teamSessionWithHostAlready();
        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(session);
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        int before = session.getTeams().size();
        InteractiveSession result = interactiveSessionService.createTeam("ABCD12", "Custom Crew", "pink", host);

        assertEquals(before + 1, result.getTeams().size());
        cephadex.brainflex.model.Team added = result.getTeams().get(result.getTeams().size() - 1);
        assertEquals("Custom Crew", added.getName());
        assertEquals("pink", added.getColor());
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/teams"),
                any(TeamUpdateMessage.class));
    }

    @Test
    void createTeam_WhenNotTeamMode_ThrowsConflict() {
        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(lobbySession);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.createTeam("ABCD12", "x", "blue", host));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void deleteTeam_ReassignsOrphanedPlayers() {
        InteractiveSession session = teamSessionWithHostAlready();
        // Three teams so we can delete one and still have ≥2 remaining.
        session.getTeams().add(makeTeam("t3", "green"));
        InteractiveSessionPlayer extra = player("player2");
        extra.setTeamId(session.getTeams().get(2).getId());
        session.getTeams().get(2).setMemberCount(1);
        session.getTeams().get(2).setCaptainUserId("player2");
        session.getPlayers().add(extra);

        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(session);
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        String removedTeamId = session.getTeams().get(2).getId();
        InteractiveSession result = interactiveSessionService.deleteTeam("ABCD12", removedTeamId, host);

        assertEquals(2, result.getTeams().size());
        // player2 was on the deleted team → reassigned to one of the remaining two.
        InteractiveSessionPlayer p2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertNotNull(p2.getTeamId());
        assertFalse(removedTeamId.equals(p2.getTeamId()));
    }

    @Test
    void deleteTeam_WhenOnlyTwoTeams_ThrowsConflict() {
        InteractiveSession session = teamSessionWithHostAlready();
        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(session);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.deleteTeam("ABCD12", session.getTeams().get(0).getId(), host));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void movePlayerToTeam_RelocatesAndRecomputesScores() {
        InteractiveSession session = teamSessionWithHostAlready();
        // Add a second player on team 1 so we can move them to team 0.
        InteractiveSessionPlayer p2 = player("player2");
        p2.setScore(70);
        p2.setTeamId(session.getTeams().get(1).getId());
        session.getTeams().get(1).setMemberCount(1);
        session.getTeams().get(1).setScore(70);
        session.getTeams().get(1).setCaptainUserId("player2");
        session.getPlayers().add(p2);
        // Host has a score of 30.
        session.getPlayers().get(0).setScore(30);
        session.getTeams().get(0).setScore(30);

        when(authorizationService.requireInteractiveSessionHost("ABCD12", host)).thenReturn(session);
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractiveSession result = interactiveSessionService.movePlayerToTeam(
                "ABCD12", "player2", session.getTeams().get(0).getId(), host);

        InteractiveSessionPlayer movedP2 = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(result.getTeams().get(0).getId(), movedP2.getTeamId());
        assertEquals(100, result.getTeams().get(0).getScore()); // 30 + 70
        assertEquals(0, result.getTeams().get(1).getScore());
        assertEquals(2, result.getTeams().get(0).getMemberCount());
        assertEquals(0, result.getTeams().get(1).getMemberCount());
    }

    @Test
    void endGame_InTeamMode_StampsTeamIdOnEveryPlacement() {
        InteractiveSession session = teamModeSubmitSession();
        session.getPlayers().get(0).setScore(120);
        cephadex.brainflex.model.Team team0 = session.getTeams().get(0);
        team0.setScore(120);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        // Use the public end-early path which calls endGame internally.
        interactiveSessionService.endInteractiveSessionEarly("ABCD12", "guest:p1");

        verify(interactiveSessionResultRepository).save(argThat(r -> {
            if (!(r instanceof InteractiveSessionResult res)) return false;
            return res.getPlacements().stream().allMatch(pp -> pp.getTeamId() != null);
        }));
    }

    @Test
    void endGame_DelegatesPlacementsToGameHistoryService() {
        // Chunk 15 — endGame must hand its computed placements to the
        // GameHistoryService so per-user history rows get written before the
        // PlayerStats rollup mutates.
        InteractiveSession session = teamModeSubmitSession();
        session.getPlayers().get(0).setScore(75);
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        interactiveSessionService.endInteractiveSessionEarly("ABCD12", "guest:p1");

        verify(gameHistoryService).recordFinish(
                argThat(s -> s instanceof InteractiveSession is && "session1".equals(is.getId())),
                argThat(placements -> placements != null && !placements.isEmpty()));
    }

    private cephadex.brainflex.model.Team makeTeam(String id, String color) {
        cephadex.brainflex.model.Team t = new cephadex.brainflex.model.Team();
        t.setId(id);
        t.setColor(color);
        t.setName("Team " + id);
        return t;
    }

    /** Two-team team-mode session with the host already on team 0. */
    private InteractiveSession teamSessionWithHostAlready() {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("host1");
        s.setStatus(InteractiveSessionStatus.LOBBY);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setTeamMode(true);
        settings.setAutoBalanceTeams(true);
        s.setSettings(settings);

        cephadex.brainflex.model.Team t0 = makeTeam("t1", "red");
        cephadex.brainflex.model.Team t1 = makeTeam("t2", "blue");
        s.setTeams(new ArrayList<>(List.of(t0, t1)));

        InteractiveSessionPlayer hostPlayer = player("host1");
        hostPlayer.setTeamId(t0.getId());
        t0.setMemberCount(1);
        t0.setCaptainUserId("host1");
        s.setPlayers(new ArrayList<>(List.of(hostPlayer)));
        return s;
    }

    /** In-progress single-MCQ session with two teams; "p1" is on team 0. */
    private InteractiveSession teamModeSubmitSession() {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("p1");
        s.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        s.setPhase(InteractiveSessionPhase.SUBMIT);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setAnswerSubmissionMode(AnswerSubmissionMode.SIMULTANEOUS);
        settings.setTotalRounds(1);
        settings.setSpeedBonus(false);
        settings.setTeamMode(true);
        s.setSettings(settings);
        s.setCurrentRound(0);
        s.setDeckSnapshot(List.of(sampleElements(1).get(0)));

        cephadex.brainflex.model.Team t0 = makeTeam("t1", "red");
        cephadex.brainflex.model.Team t1 = makeTeam("t2", "blue");
        s.setTeams(new ArrayList<>(List.of(t0, t1)));

        InteractiveSessionPlayer p1 = player("p1");
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

        InteractiveSessionPlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        InteractiveSessionPlayer p2 = player("p2");
        p2.setPrincipalName("guest:p2");
        lobbySession.setPlayers(new ArrayList<>(List.of(p1, p2)));

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        interactiveSessionService.startGame("ABCD12", "guest:host1");

        // Canonical topic broadcast still fires for the host/audience view.
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        // Each player receives a personalized RoundStartMessage on their user queue.
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("guest:p1"),
                org.mockito.ArgumentMatchers.eq("/queue/interactiveSession/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("guest:p2"),
                org.mockito.ArgumentMatchers.eq("/queue/interactiveSession/ABCD12/round"),
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
                "nos",
                "Prompt", List.of(a, b), List.of(a.id()),
                100, Difficulty.EASY, null,
                false, false, 0, // shuffleOptions=false
                TestElementChromes.scored("nos", "Prompt"));
        lobbySession.setDeckSnapshot(List.of(first));

        InteractiveSessionPlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        lobbySession.setPlayers(new ArrayList<>(List.of(p1)));

        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        interactiveSessionService.startGame("ABCD12", "guest:host1");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/interactive-session/ABCD12/round"),
                any(cephadex.brainflex.dto.RoundStartMessage.class));
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), anyString(), any(cephadex.brainflex.dto.RoundStartMessage.class));
    }

    // ---- Chunk 13 ----

    @Test
    void createInteractiveSession_WithCustomRoomCode_HonorsCode() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(interactiveSessionRepository.findByRoomCode("PARTY1")).thenReturn(Optional.empty());
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "PARTY1", null, null, null, null, null, null, null, null);
        InteractiveSession result = interactiveSessionService.createInteractiveSession(host, request);

        assertEquals("PARTY1", result.getRoomCode());
        assertEquals("PARTY1", result.getCustomRoomCode());
    }

    @Test
    void createInteractiveSession_WithCustomRoomCodeCollision_ThrowsConflict() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        // The collision-check path is the only call to findByRoomCode in this
        // path; return a placeholder session to simulate "code already in use".
        InteractiveSession taken = new InteractiveSession();
        taken.setRoomCode("PARTY1");
        when(interactiveSessionRepository.findByRoomCode("PARTY1")).thenReturn(Optional.of(taken));

        CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest(
                "deck1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "PARTY1", null, null, null, null, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.createInteractiveSession(host, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void submitAnswer_PopulatesTimingAndStreakFields() {
        InteractiveSession session = readySession();
        InteractiveSessionPlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        session.setPlayers(new ArrayList<>(List.of(p1)));
        session.setRoundStartedAt(java.time.LocalDateTime.now().minusSeconds(2));
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-a")));
        interactiveSessionService.submitAnswer("ABCD12", req, "guest:p1");

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
        InteractiveSession session = readySession();
        InteractiveSessionPlayer p1 = player("p1");
        p1.setPrincipalName("guest:p1");
        p1.setCurrentStreak(3);
        p1.setLongestStreak(3);
        session.setPlayers(new ArrayList<>(List.of(p1)));
        session.setRoundStartedAt(java.time.LocalDateTime.now());
        when(interactiveSessionCache.get("ABCD12")).thenReturn(Optional.of(session));

        DeckElement el = session.getDeckSnapshot().get(0);
        // Pick the second option (B) which is not the correct one (A).
        AnswerSubmitRequest req = new AnswerSubmitRequest(el.id(), new McqAnswer(List.of(el.id() + "-b")));
        interactiveSessionService.submitAnswer("ABCD12", req, "guest:p1");

        PlayerAnswer ans = p1.getAnswers().get(0);
        assertFalse(ans.isCorrect());
        assertEquals(3, ans.getStreakBeforeAnswer());
        assertEquals(0, p1.getCurrentStreak());
        // Longest still 3 — we only bump it on the way up, never down.
        assertEquals(3, p1.getLongestStreak());
        assertEquals(0.0, p1.getAccuracy(), 0.0001);
    }

    @Test
    void joinInteractiveSession_WithValidAvatarKey_SetsAvatarAndDerivesColor() {
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(avatarService.has("fox-orange")).thenReturn(true);
        when(avatarService.get("fox-orange")).thenReturn(
                new AvatarService.AvatarPreset("fox-orange", "Fox", "/x/fox.svg", "orange"));

        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setIsGuest(false);

        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", newPlayer, null, "fox-orange", null);
        InteractiveSessionPlayer joined = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals("fox-orange", joined.getAvatarKey());
        assertEquals("orange", joined.getColorTag());
    }

    @Test
    void joinInteractiveSession_WithUnknownAvatar_SilentlyDropsKey() {
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));
        when(interactiveSessionRepository.save(any(InteractiveSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(avatarService.has("not-a-real-preset")).thenReturn(false);

        User newPlayer = new User();
        newPlayer.setId("player2");
        newPlayer.setIsGuest(false);

        InteractiveSession result = interactiveSessionService.joinInteractiveSession("ABCD12", newPlayer, null, "not-a-real-preset", null);
        InteractiveSessionPlayer joined = result.getPlayers().stream()
                .filter(p -> "player2".equals(p.getUserId())).findFirst().orElseThrow();
        assertEquals(null, joined.getAvatarKey());
    }

    @Test
    void joinInteractiveSession_WhenRequireFullNameAndGuest_ThrowsForbidden() {
        lobbySession.getSettings().setRequireFullName(true);
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(lobbySession));

        User guest = new User();
        guest.setId("guest1");
        guest.setIsGuest(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interactiveSessionService.joinInteractiveSession("ABCD12", guest));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Stripped-down session ready to accept a submit (IN_PROGRESS, 1 element). */
    private InteractiveSession readySession() {
        InteractiveSession s = new InteractiveSession();
        s.setId("session1");
        s.setRoomCode("ABCD12");
        s.setHostUserId("host1");
        s.setStatus(InteractiveSessionStatus.IN_PROGRESS);
        s.setPhase(InteractiveSessionPhase.SUBMIT);
        s.setSettings(new InteractiveSessionSettings());
        s.setCurrentRound(0);
        s.setDeckSnapshot(sampleElements(1));
        s.setPlayers(new ArrayList<>());
        return s;
    }
}
