/**
 * Core service for showcase lifecycle and the round-by-round state machine.
 *
 * State machine:
 *   startShowcase()  → freezes deck.elements into deckSnapshot, broadcasts ROUND_START
 *   submitAnswer()   → records polymorphic AnswerPayload; ends round if all answered
 *   timer fires      → ends round for unanswered players (or advances if SLIDE)
 *   completeRound()  → broadcasts ROUND_RESULT; advances or ends
 *   endShowcase()    → ranks players, writes ShowcaseResult, updates stats
 *
 * Best Answer mode elements (any element with bestAnswerMode=true) run a
 * three-phase cycle:
 *   SUBMIT → players submit answers as on a normal round
 *   VOTE   → anonymized submissions are broadcast; each player votes for the
 *            submission they think is best (skipping their own is honor-system
 *            on the client; the server doesn't reject self-votes)
 *   REVEAL → server tallies, awards bestAnswerBonus to the player(s) with the
 *            most votes (tie → all tied players get the bonus), broadcasts the
 *            same RoundResultMessage as a normal round with a BestAnswerOutcome
 *            attached for the de-anonymized submissions + crown
 */
package cephadex.brainflex.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.AnswerProgressMessage;
import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.CreateShowcaseRequest;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.RoundStartMessage;
import cephadex.brainflex.dto.ShowcaseDTO;
import cephadex.brainflex.dto.ShowcaseEndedMessage;
import cephadex.brainflex.dto.ShowcaseReviewDTO;
import cephadex.brainflex.dto.VotePhaseStartMessage;
import cephadex.brainflex.dto.VoteProgressMessage;
import cephadex.brainflex.dto.VoteSubmitRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.RoundVote;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.answer.TimeoutAnswer;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.ShowcasePhase;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.UserRepository;
import jakarta.annotation.PreDestroy;

@Service
public class ShowcaseService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final int BETWEEN_ROUNDS_DELAY_SECONDS = 4;
    private static final int SLIDE_DEFAULT_SECONDS = 8;

    private final ShowcaseRepository showcaseRepository;
    private final DeckRepository deckRepository;
    private final ShowcaseResultRepository showcaseResultRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShowcaseCacheService showcaseCache;
    private final AuthorizationService authorizationService;
    private final DeckImageHydrationService deckImageHydrationService;
    private final DeckService deckService;
    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, Object> roundLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public ShowcaseService(
            ShowcaseRepository showcaseRepository,
            DeckRepository deckRepository,
            ShowcaseResultRepository showcaseResultRepository,
            UserRepository userRepository,
            ShowcaseCacheService showcaseCache,
            AuthorizationService authorizationService,
            DeckImageHydrationService deckImageHydrationService,
            DeckService deckService,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.showcaseRepository = showcaseRepository;
        this.deckRepository = deckRepository;
        this.showcaseResultRepository = showcaseResultRepository;
        this.userRepository = userRepository;
        this.showcaseCache = showcaseCache;
        this.authorizationService = authorizationService;
        this.deckImageHydrationService = deckImageHydrationService;
        this.deckService = deckService;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ---- CRUD ----

    public Showcase createShowcase(User host, CreateShowcaseRequest request) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        // Bake presigned URLs into the snapshot we're about to freeze, so the
        // session's elements + cover/background carry renderable URLs (gallery
        // internalImgId references would otherwise have imgUrl=null on the
        // persisted deck).
        deckImageHydrationService.hydrate(deck);

        List<DeckElement> elements = deck.getElements();
        if (elements == null || elements.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Deck has no elements");
        }

        // Start from the deck's author-suggested defaults, then layer the host's
        // overrides.
        ShowcaseSettings settings = copyOf(deck.getDefaultSettings());
        if (request.gameMode() != null)
            settings.setGameMode(request.gameMode());
        if (request.totalRounds() != null)
            settings.setTotalRounds(request.totalRounds());
        if (request.timePerQuestion() != null)
            settings.setTimePerQuestion(request.timePerQuestion());
        if (request.speedBonus() != null)
            settings.setSpeedBonus(request.speedBonus());
        if (request.allowGuests() != null)
            settings.setAllowGuests(request.allowGuests());
        if (request.maxPlayers() != null)
            settings.setMaxPlayers(request.maxPlayers());
        if (request.allowLateJoin() != null)
            settings.setAllowLateJoin(request.allowLateJoin());
        if (request.showScoresImmediately() != null)
            settings.setShowScoresImmediately(request.showScoresImmediately());
        if (request.scoringEnabled() != null)
            settings.setScoringEnabled(request.scoringEnabled());
        // totalRounds is upper-bounded by the deck's actual element count.
        settings.setTotalRounds(Math.min(settings.getTotalRounds(), elements.size()));

        // Frozen snapshot of the elements as authored — drawn in deck order, truncated
        // to totalRounds. Slides participate in deck order; we never shuffle.
        List<DeckElement> snapshot = new ArrayList<>(elements.subList(0, settings.getTotalRounds()));

        Showcase session = new Showcase();
        session.setHostUserId(host.getId());
        session.setDeckId(request.deckId());
        session.setDeckSnapshot(snapshot);
        session.setDeckCoverImageUrl(urlOf(deck.getCover()));
        session.setDeckBackgroundImageUrl(urlOf(deck.getBackground()));
        session.setThemeId(deck.getThemeId());
        session.setSettings(settings);
        session.setRoomCode(generateUniqueRoomCode());
        session.setInviteToken(UUID.randomUUID().toString());
        session.getPlayers().add(playerFromUser(host));

        session = showcaseRepository.save(session);
        showcaseCache.put(session);
        return session;
    }

    public Showcase getByRoomCode(String roomCode) {
        return showcaseRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showcase not found"));
    }

    public Showcase getByInviteToken(String inviteToken) {
        return showcaseRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showcase not found"));
    }

    public Showcase joinShowcase(String roomCode, User player) {
        Showcase session = getByRoomCode(roomCode);

        GameStatus status = session.getStatus();
        if (status == GameStatus.FINISHED || status == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is over");
        if (status == GameStatus.IN_PROGRESS && !session.getSettings().isAllowLateJoin())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase has already started");

        boolean alreadyJoined = session.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(player.getId()));
        if (alreadyJoined)
            return session;

        if (!session.getSettings().isAllowGuests() && Boolean.TRUE.equals(player.getIsGuest()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This showcase does not allow guests");

        if (session.getPlayers().size() >= session.getSettings().getMaxPlayers())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is full");

        session.getPlayers().add(playerFromUser(player));
        session = showcaseRepository.save(session);
        showcaseCache.put(session);

        messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
        return session;
    }

    public void cancelShowcase(String roomCode, User requestingUser) {
        Showcase session = authorizationService.requireShowcaseHost(roomCode, requestingUser);
        if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is already ended");

        session.setStatus(GameStatus.CANCELLED);
        showcaseRepository.save(session);
        showcaseCache.evict(roomCode);
        messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
    }

    public Optional<ShowcaseResult> getResults(String roomCode) {
        Showcase session = getByRoomCode(roomCode);
        return showcaseResultRepository.findByShowcaseId(session.getId());
    }

    // ---- Review ----

    public ShowcaseReviewDTO buildReview(String roomCode) {
        Showcase session = getByRoomCode(roomCode);
        if (session.getStatus() != GameStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is not yet finished");

        ShowcaseResult result = showcaseResultRepository.findByShowcaseId(session.getId()).orElse(null);
        List<PlayerPlacement> placements = result != null ? result.getPlacements() : List.of();

        List<ShowcaseReviewDTO.RoundReview> rounds = new ArrayList<>();
        List<DeckElement> snap = session.getDeckSnapshot();
        for (int i = 0; i < snap.size(); i++) {
            DeckElement element = snap.get(i);
            List<ShowcaseReviewDTO.PlayerRoundDetail> details = new ArrayList<>();
            int timedOut = 0;
            for (ShowcasePlayer player : session.getPlayers()) {
                PlayerAnswer ans = player.getAnswers().stream()
                        .filter(a -> a.getElementId().equals(element.id()))
                        .findFirst().orElse(null);
                if (ans == null)
                    continue;
                details.add(new ShowcaseReviewDTO.PlayerRoundDetail(
                        player.getUserId(), player.getUserName(),
                        ans.getPayload(), ans.isCorrect(), ans.getPointsAwarded()));
                if (ans.getPayload() instanceof TimeoutAnswer)
                    timedOut++;
            }
            rounds.add(new ShowcaseReviewDTO.RoundReview(i, element, timedOut, details));
        }

        return new ShowcaseReviewDTO(
                session.getId(),
                session.getRoomCode(),
                session.getEndedAt(),
                session.getSettings().isScoringEnabled(),
                placements,
                rounds);
    }

    // ---- State machine (called by ShowcaseWebSocketController) ----

    public void startGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = getByRoomCode(roomCode);
            validateHost(session, principalName);

            if (session.getStatus() != GameStatus.LOBBY)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase has already started");

            session.setStatus(GameStatus.IN_PROGRESS);
            session.setPhase(ShowcasePhase.SUBMIT);
            session.setStartedAt(LocalDateTime.now());
            session.setCurrentRound(0);
            session.setRoundStartedAt(LocalDateTime.now());
            showcaseRepository.save(session);
            showcaseCache.put(session);

            DeckElement first = session.getDeckSnapshot().get(0);
            broadcastRoundStart(session, first);
            scheduleElementTimer(session, 0, first);
        }
    }

    public void submitAnswer(String roomCode, AnswerSubmitRequest request, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveSession(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getPhase() != ShowcasePhase.SUBMIT)
                return;

            DeckElement current = session.getDeckSnapshot().get(session.getCurrentRound());
            if (!current.id().equals(request.elementId()))
                return; // stale answer
            if (current instanceof Slide)
                return; // slides accept no answers

            String userId = resolveUserId(principalName);
            ShowcasePlayer player = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
            if (player == null)
                return;

            boolean alreadyAnswered = player.getAnswers().stream()
                    .anyMatch(a -> a.getElementId().equals(current.id()));
            if (alreadyAnswered)
                return;

            ElementScorer.Result result = ElementScorer.score(current, request.payload());
            int points = result.points();
            if (result.correct()) {
                points = applySpeedBonus(points, session);
            }

            PlayerAnswer answer = new PlayerAnswer();
            answer.setElementId(current.id());
            answer.setPayload(request.payload());
            answer.setCorrect(result.correct());
            answer.setPointsAwarded(points);
            answer.setAnsweredAt(LocalDateTime.now());
            // For Best Answer rounds: assign a server-side submissionId so the
            // VOTE-phase broadcast can reference this submission anonymously.
            if (current.bestAnswerMode()) {
                answer.setSubmissionId(UUID.randomUUID().toString());
            }

            player.getAnswers().add(answer);
            player.setScore(player.getScore() + points);
            showcaseCache.put(session);

            broadcastAnswerProgress(session, current.id());

            boolean allAnswered = session.getPlayers().stream()
                    .allMatch(p -> p.getAnswers().stream()
                            .anyMatch(a -> a.getElementId().equals(current.id())));
            if (allAnswered)
                completeRound(session);
        }
    }

    public void bootPlayer(String roomCode, String hostPrincipalName, String targetUserId) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveSession(roomCode);
            validateHost(session, hostPrincipalName);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is already over");
            if (session.getHostUserId().equals(targetUserId))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host cannot boot themselves");

            boolean removed = session.getPlayers().removeIf(p -> p.getUserId().equals(targetUserId));
            if (!removed)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in this showcase");

            showcaseRepository.save(session);
            showcaseCache.put(session);
            messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
        }
    }

    public void endShowcaseEarly(String roomCode, String hostPrincipalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveSession(roomCode);
            validateHost(session, hostPrincipalName);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is not in progress");
            endGame(session);
        }
    }

    public void nextRound(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = getByRoomCode(roomCode);
            validateHost(session, principalName);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getSettings().getGameMode() != GameMode.TURN_BASED)
                return;
            startNextRound(roomCode);
        }
    }

    public void leaveGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveSession(roomCode);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                return;

            String userId = resolveUserId(principalName);
            session.getPlayers().removeIf(p -> p.getUserId().equals(userId));
            showcaseRepository.save(session);
            showcaseCache.put(session);
            messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
        }
    }

    // ---- Round transitions ----

    private void handleRoundTimeout(String roomCode, int timedRound) {
        synchronized (getLock(roomCode)) {
            Showcase session = showcaseCache.get(roomCode)
                    .orElseGet(() -> showcaseRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null)
                return;
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getCurrentRound() != timedRound)
                return;

            DeckElement current = session.getDeckSnapshot().get(session.getCurrentRound());
            if (current instanceof Slide) {
                advanceToNextRound(session);
                return;
            }
            completeRound(session);
        }
    }

    private void advanceToNextRound(Showcase session) {
        boolean isLast = session.getCurrentRound() >= session.getDeckSnapshot().size() - 1;
        if (isLast) {
            showcaseRepository.save(session);
            endGame(session);
            return;
        }
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setRoundStartedAt(null);
        showcaseRepository.save(session);
        showcaseCache.put(session);
        startNextRound(session.getRoomCode());
    }

    private void completeRound(Showcase session) {
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        stampTimeoutAnswers(session, element);

        // Best Answer mode hijacks the normal complete-round flow: instead of
        // revealing immediately we transition to VOTE phase and broadcast the
        // anonymized submissions. The reveal happens in completeVotePhase.
        if (element.bestAnswerMode() && hasVoteEligibleSubmission(session, element)) {
            startVotePhase(session, element);
            return;
        }

        broadcastRoundResult(session, element, null);
        advanceRound(session);
    }

    /** Drops a TimeoutAnswer onto any player who didn't submit for this element. */
    private void stampTimeoutAnswers(Showcase session, DeckElement element) {
        for (ShowcasePlayer player : session.getPlayers()) {
            boolean answered = player.getAnswers().stream()
                    .anyMatch(a -> a.getElementId().equals(element.id()));
            if (answered)
                continue;
            PlayerAnswer timeout = new PlayerAnswer();
            timeout.setElementId(element.id());
            timeout.setPayload(new TimeoutAnswer());
            timeout.setCorrect(false);
            timeout.setPointsAwarded(0);
            timeout.setAnsweredAt(LocalDateTime.now());
            // Deliberately no submissionId — timed-out players are not vote-eligible.
            player.getAnswers().add(timeout);
        }
    }

    /**
     * True if at least one player submitted a real answer (i.e. has a submissionId)
     * for the element.
     */
    private static boolean hasVoteEligibleSubmission(Showcase session, DeckElement element) {
        return session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .anyMatch(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null);
    }

    /**
     * Broadcast the standard round-result message (best-answer outcome may be
     * null).
     */
    private void broadcastRoundResult(
            Showcase session,
            DeckElement element,
            RoundResultMessage.BestAnswerOutcome bestAnswer) {
        List<RoundResultMessage.PlayerRoundResult> results = session.getPlayers().stream()
                .map(player -> {
                    PlayerAnswer ans = player.getAnswers().stream()
                            .filter(a -> a.getElementId().equals(element.id()))
                            .findFirst().orElseThrow();
                    return new RoundResultMessage.PlayerRoundResult(
                            player.getUserId(), player.getUserName(),
                            ans.getPayload(), ans.isCorrect(), ans.getPointsAwarded(),
                            player.getScore());
                })
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/roundResult",
                new RoundResultMessage(session.getCurrentRound(), element, results, bestAnswer));
    }

    /** Advance to the next round (or end the showcase if this was the last). */
    private void advanceRound(Showcase session) {
        boolean isLast = session.getCurrentRound() >= session.getDeckSnapshot().size() - 1;
        if (isLast) {
            showcaseRepository.save(session);
            endGame(session);
            return;
        }
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setRoundStartedAt(null);
        session.setPhase(ShowcasePhase.SUBMIT);
        showcaseRepository.save(session);
        showcaseCache.put(session);

        if (session.getSettings().getGameMode() == GameMode.SIMULTANEOUS) {
            String roomCode = session.getRoomCode();
            scheduler.schedule(() -> {
                try {
                    startNextRound(roomCode);
                } catch (Exception ignored) {
                }
            }, BETWEEN_ROUNDS_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    // ---- Best Answer phase machine ----

    /**
     * Transition from SUBMIT to VOTE phase: collect vote-eligible submissions,
     * broadcast them anonymously, and schedule the vote-phase timer.
     */
    private void startVotePhase(Showcase session, DeckElement element) {
        session.setPhase(ShowcasePhase.VOTE);
        session.setRoundStartedAt(LocalDateTime.now());

        List<VotePhaseStartMessage.AnonymizedSubmission> submissions = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null)
                .map(a -> new VotePhaseStartMessage.AnonymizedSubmission(a.getSubmissionId(), a.getPayload()))
                .toList();

        int timePerVote = effectiveDisplaySeconds(element, session.getSettings());

        showcaseRepository.save(session);
        showcaseCache.put(session);

        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/votePhase",
                new VotePhaseStartMessage(
                        session.getCurrentRound(),
                        ElementRedactor.redact(element),
                        submissions,
                        timePerVote,
                        session.getRoundStartedAt()));

        if (timePerVote > 0) {
            scheduleVoteTimer(session.getRoomCode(), session.getCurrentRound(), timePerVote);
        }
    }

    /** Client → server vote handler (called from ShowcaseWebSocketController). */
    public void submitVote(String roomCode, VoteSubmitRequest request, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveSession(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getPhase() != ShowcasePhase.VOTE)
                return;

            DeckElement current = session.getDeckSnapshot().get(session.getCurrentRound());
            if (!current.id().equals(request.elementId()))
                return; // stale vote

            // Validate the voted submission exists for this round.
            boolean validSubmission = session.getPlayers().stream()
                    .flatMap(p -> p.getAnswers().stream())
                    .anyMatch(a -> request.submissionId().equals(a.getSubmissionId())
                            && current.id().equals(a.getElementId()));
            if (!validSubmission)
                return;

            String userId = resolveUserId(principalName);
            ShowcasePlayer voter = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
            if (voter == null)
                return;

            boolean alreadyVoted = voter.getVotes().stream()
                    .anyMatch(v -> current.id().equals(v.getElementId()));
            if (alreadyVoted)
                return;

            RoundVote vote = new RoundVote();
            vote.setElementId(current.id());
            vote.setVotedSubmissionId(request.submissionId());
            vote.setVotedAt(LocalDateTime.now());
            voter.getVotes().add(vote);
            showcaseCache.put(session);

            broadcastVoteProgress(session, current.id());

            boolean allVoted = session.getPlayers().stream()
                    .allMatch(p -> p.getVotes().stream()
                            .anyMatch(v -> current.id().equals(v.getElementId())));
            if (allVoted)
                completeVotePhase(session);
        }
    }

    /**
     * Tally votes, award the bonus to the winner(s), and broadcast the
     * de-anonymized REVEAL on the standard /roundResult channel with a
     * BestAnswerOutcome attached.
     */
    private void completeVotePhase(Showcase session) {
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        int bonus = Math.max(0, element.bestAnswerBonus());

        // Tally votes per submissionId. Submissions with zero votes still
        // appear in the tally so the REVEAL UI can show "no one voted for X".
        Map<String, Integer> voteCounts = new HashMap<>();
        session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null)
                .forEach(a -> voteCounts.put(a.getSubmissionId(), 0));

        for (ShowcasePlayer p : session.getPlayers()) {
            p.getVotes().stream()
                    .filter(v -> element.id().equals(v.getElementId()))
                    .findFirst()
                    .ifPresent(v -> voteCounts.computeIfPresent(
                            v.getVotedSubmissionId(), (k, c) -> c + 1));
        }

        int maxVotes = voteCounts.values().stream().max(Integer::compareTo).orElse(0);

        // Build the tally + identify winners. Winner-detection only triggers
        // when at least one vote was cast — an all-skipped round awards no bonus.
        List<RoundResultMessage.SubmissionTally> tallies = new ArrayList<>();
        List<String> winnerUserIds = new ArrayList<>();
        for (ShowcasePlayer p : session.getPlayers()) {
            for (PlayerAnswer a : p.getAnswers()) {
                if (!element.id().equals(a.getElementId()))
                    continue;
                if (a.getSubmissionId() == null)
                    continue;
                int count = voteCounts.getOrDefault(a.getSubmissionId(), 0);
                tallies.add(new RoundResultMessage.SubmissionTally(
                        a.getSubmissionId(),
                        p.getUserId(),
                        p.getUserName(),
                        a.getPayload(),
                        count));
                if (maxVotes > 0 && count == maxVotes) {
                    winnerUserIds.add(p.getUserId());
                    a.setBestAnswerWinner(true);
                    a.setPointsAwarded(a.getPointsAwarded() + bonus);
                    p.setScore(p.getScore() + bonus);
                }
            }
        }

        broadcastRoundResult(session, element,
                new RoundResultMessage.BestAnswerOutcome(tallies, winnerUserIds, bonus));
        advanceRound(session);
    }

    private void broadcastVoteProgress(Showcase session, String elementId) {
        List<String> votedUserIds = session.getPlayers().stream()
                .filter(p -> p.getVotes().stream().anyMatch(v -> elementId.equals(v.getElementId())))
                .map(ShowcasePlayer::getUserId)
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/voted",
                new VoteProgressMessage(
                        session.getCurrentRound(), votedUserIds, session.getPlayers().size()));
    }

    private void scheduleVoteTimer(String roomCode, int round, int timeLimitSeconds) {
        scheduler.schedule(() -> {
            try {
                handleVoteTimeout(roomCode, round);
            } catch (Exception ignored) {
            }
        }, timeLimitSeconds, TimeUnit.SECONDS);
    }

    private void handleVoteTimeout(String roomCode, int timedRound) {
        synchronized (getLock(roomCode)) {
            Showcase session = showcaseCache.get(roomCode)
                    .orElseGet(() -> showcaseRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null)
                return;
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getCurrentRound() != timedRound)
                return;
            if (session.getPhase() != ShowcasePhase.VOTE)
                return;
            completeVotePhase(session);
        }
    }

    private void startNextRound(String roomCode) {
        synchronized (getLock(roomCode)) {
            Showcase session = showcaseCache.get(roomCode)
                    .orElseGet(() -> showcaseRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null || session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            session.setRoundStartedAt(LocalDateTime.now());
            session.setPhase(ShowcasePhase.SUBMIT);
            showcaseRepository.save(session);
            showcaseCache.put(session);

            DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
            broadcastRoundStart(session, element);
            scheduleElementTimer(session, session.getCurrentRound(), element);
        }
    }

    private void endGame(Showcase session) {
        session.setStatus(GameStatus.FINISHED);
        session.setEndedAt(LocalDateTime.now());

        List<ShowcasePlayer> ranked = session.getPlayers().stream()
                .sorted((a, b) -> b.getScore() - a.getScore())
                .toList();

        List<PlayerPlacement> placements = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            ShowcasePlayer sp = ranked.get(i);
            PlayerPlacement p = new PlayerPlacement();
            p.setUserId(sp.getUserId());
            p.setUserName(sp.getUserName());
            p.setGuest(sp.isGuest());
            p.setFinalScore(sp.getScore());
            p.setPlacement(i + 1);
            p.setCorrectAnswers((int) sp.getAnswers().stream().filter(PlayerAnswer::isCorrect).count());
            p.setTotalQuestions(session.getDeckSnapshot().size());
            placements.add(p);
        }

        ShowcaseResult result = new ShowcaseResult();
        result.setShowcaseId(session.getId());
        result.setPlacements(placements);
        showcaseResultRepository.save(result);
        showcaseRepository.save(session);
        showcaseCache.evict(session.getRoomCode());

        // Bump the deck's denormalized play counter atomically so Explore's
        // "most played" / "trending" sorts reflect this finish without a
        // cross-collection aggregation.
        deckService.incrementPlayCount(session.getDeckId());

        for (PlayerPlacement p : placements) {
            if (!p.isGuest())
                updateStatsAfterGame(p.getUserId(), p.getFinalScore(), p.getPlacement() == 1);
        }

        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/gameOver",
                new ShowcaseEndedMessage(placements));
    }

    // ---- Scoring + broadcast helpers ----

    /** Layers speed bonus on top of the base score for SIMULTANEOUS mode. */
    private int applySpeedBonus(int basePoints, Showcase session) {
        ShowcaseSettings s = session.getSettings();
        if (!s.isSpeedBonus() || s.getGameMode() != GameMode.SIMULTANEOUS || session.getRoundStartedAt() == null) {
            return basePoints;
        }
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        int timeWindow = effectiveDisplaySeconds(element, s);
        if (timeWindow <= 0)
            return basePoints;
        long totalMillis = timeWindow * 1000L;
        long elapsed = Duration.between(session.getRoundStartedAt(), LocalDateTime.now()).toMillis();
        elapsed = Math.min(Math.max(elapsed, 0), totalMillis);
        double speedFraction = 1.0 - ((double) elapsed / totalMillis);
        return basePoints + (int) (basePoints * 0.5 * speedFraction);
    }

    private void updateStatsAfterGame(String userId, int finalScore, boolean won) {
        userRepository.findById(userId).ifPresent(user -> {
            var stats = user.getStats();
            stats.setGamesPlayed(stats.getGamesPlayed() + 1);
            stats.setTotalPoints(stats.getTotalPoints() + finalScore);
            if (finalScore > stats.getHighScore())
                stats.setHighScore(finalScore);
            stats.setCurrentStreak(won ? stats.getCurrentStreak() + 1 : 0);
            userRepository.save(user);
        });
    }

    private void broadcastRoundStart(Showcase session, DeckElement element) {
        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/round",
                new RoundStartMessage(
                        session.getCurrentRound(),
                        session.getDeckSnapshot().size(),
                        ElementRedactor.redact(element),
                        session.getRoundStartedAt()));
    }

    private void broadcastAnswerProgress(Showcase session, String elementId) {
        List<String> answeredUserIds = session.getPlayers().stream()
                .filter(p -> p.getAnswers().stream().anyMatch(a -> a.getElementId().equals(elementId)))
                .map(ShowcasePlayer::getUserId)
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/answered",
                new AnswerProgressMessage(
                        session.getCurrentRound(), answeredUserIds, session.getPlayers().size()));
    }

    private void scheduleElementTimer(Showcase session, int round, DeckElement element) {
        int seconds = effectiveDisplaySeconds(element, session.getSettings());
        if (seconds <= 0 && !(element instanceof Slide)) {
            // unlimited for questions; slides force a sane minimum so they advance
            return;
        }
        if (seconds <= 0)
            seconds = SLIDE_DEFAULT_SECONDS;
        scheduleRoundTimer(session.getRoomCode(), round, seconds);
    }

    /**
     * Resolves the actual duration for an element:
     * element.displaySeconds > 0 → use it
     * else settings.timePerQuestion > 0 → use it
     * else 0 (unlimited)
     */
    private static int effectiveDisplaySeconds(DeckElement element, ShowcaseSettings settings) {
        if (element.displaySeconds() > 0)
            return element.displaySeconds();
        if (settings.getTimePerQuestion() > 0)
            return settings.getTimePerQuestion();
        return 0;
    }

    private void scheduleRoundTimer(String roomCode, int round, int timeLimitSeconds) {
        scheduler.schedule(() -> {
            try {
                handleRoundTimeout(roomCode, round);
            } catch (Exception ignored) {
            }
        }, timeLimitSeconds, TimeUnit.SECONDS);
    }

    // ---- Utility ----

    private void validateHost(Showcase session, String principalName) {
        String userId = resolveUserId(principalName);
        if (!session.getHostUserId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action");
    }

    private String resolveUserId(String principalName) {
        if (principalName == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        if (principalName.startsWith("guest:"))
            return principalName.substring(6);
        return userRepository.findByGoogleId(principalName)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Showcase loadActiveSession(String roomCode) {
        return showcaseCache.get(roomCode).orElseGet(() -> getByRoomCode(roomCode));
    }

    private Object getLock(String roomCode) {
        return roundLocks.computeIfAbsent(roomCode, k -> new Object());
    }

    private ShowcasePlayer playerFromUser(User user) {
        ShowcasePlayer p = new ShowcasePlayer();
        p.setUserId(user.getId());
        p.setUserName(user.getUserName());
        p.setPictureUrl(user.getPictureUrl());
        p.setGuest(Boolean.TRUE.equals(user.getIsGuest()));
        return p;
    }

    /**
     * Defensive copy so the deck's `defaultSettings` isn't mutated by a showcase.
     */
    private static ShowcaseSettings copyOf(ShowcaseSettings src) {
        ShowcaseSettings out = new ShowcaseSettings();
        if (src == null)
            return out;
        out.setMaxPlayers(src.getMaxPlayers());
        out.setTotalRounds(src.getTotalRounds());
        out.setTimePerQuestion(src.getTimePerQuestion());
        out.setSpeedBonus(src.isSpeedBonus());
        out.setAllowGuests(src.isAllowGuests());
        out.setGameMode(src.getGameMode());
        out.setAllowLateJoin(src.isAllowLateJoin());
        out.setShowScoresImmediately(src.isShowScoresImmediately());
        out.setScoringEnabled(src.isScoringEnabled());
        return out;
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (showcaseRepository.findByRoomCode(code).isEmpty())
                return code;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not generate a unique room code");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++)
            sb.append(ROOM_CODE_CHARS.charAt(secureRandom.nextInt(ROOM_CODE_CHARS.length())));
        return sb.toString();
    }

    private static String urlOf(Image image) {
        return image == null ? null : image.imgUrl();
    }
}
