/**
 * Core service for game session lifecycle: creation, joining, and the full
 * round-by-round state machine (LOBBY → IN_PROGRESS → FINISHED).
 *
 * State machine overview:
 *   startGame()      → draws questions, broadcasts ROUND_START, starts timer
 *   submitAnswer()   → records answer; if all players answered, ends round early
 *   timer fires      → ends round for any unanswered players
 *   completeRound()  → broadcasts ROUND_RESULT; advances or ends game
 *   endGame()        → ranks players, saves ShowcaseResult, updates PlayerStats, broadcasts GAME_OVER
 *
 * Per-round locking via a ConcurrentHashMap of per-roomCode locks prevents race
 * conditions between simultaneous answer submissions and the expiry timer.
 * ShowcaseCacheService keeps active session state in Redis so answer submissions
 * read/write Redis instead of MongoDB; MongoDB is only written at round boundaries.
 */
package cephadex.brainflex.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import cephadex.brainflex.dto.ShowcaseEndedMessage;
import cephadex.brainflex.dto.ShowcaseDTO;
import cephadex.brainflex.dto.QuestionDTO;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.RoundStartMessage;
import cephadex.brainflex.dto.ShowcaseReviewDTO;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.McqShuffle;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;
import cephadex.brainflex.model.enums.QuestionType;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;
import jakarta.annotation.PreDestroy;

@Service
public class ShowcaseService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 10;
    // Delay in seconds between ROUND_RESULT broadcast and the next ROUND_START in
    // SIMULTANEOUS mode
    private static final int BETWEEN_ROUNDS_DELAY_SECONDS = 4;

    private final ShowcaseRepository showcaseRepository;
    private final DeckRepository deckRepository;
    private final QuestionRepository questionRepository;
    private final ShowcaseResultRepository showcaseResultRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShowcaseCacheService showcaseCache;
    private final SecureRandom secureRandom = new SecureRandom();

    // Per-roomCode locks prevent concurrent answer/timer races on the same session
    private final ConcurrentHashMap<String, Object> roundLocks = new ConcurrentHashMap<>();
    // Shared pool for round timers and between-round delays
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // @Lazy breaks the potential circular dependency with SimpMessagingTemplate at
    // startup
    public ShowcaseService(
            ShowcaseRepository showcaseRepository,
            DeckRepository deckRepository,
            QuestionRepository questionRepository,
            ShowcaseResultRepository showcaseResultRepository,
            UserRepository userRepository,
            ShowcaseCacheService showcaseCache,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.showcaseRepository = showcaseRepository;
        this.deckRepository = deckRepository;
        this.questionRepository = questionRepository;
        this.showcaseResultRepository = showcaseResultRepository;
        this.userRepository = userRepository;
        this.showcaseCache = showcaseCache;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ---- Session CRUD (used by ShowcaseController REST endpoints) ----

    public Showcase createShowcase(User host, CreateShowcaseRequest request) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content pack not found"));

        int available = questionRepository.countByDeckId(request.deckId());
        if (available == 0)
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "Content pack has no questions");

        ShowcaseSettings settings = new ShowcaseSettings();
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
        if (request.shuffleMcqOptions() != null)
            settings.setShuffleMcqOptions(request.shuffleMcqOptions());
        settings.setTotalRounds(Math.min(settings.getTotalRounds(), available));

        Showcase session = new Showcase();
        session.setType(GameType.TRIVIA);
        session.setHostUserId(host.getId());
        session.setDeckId(request.deckId());
        session.setDeckCoverImageUrl(deck.getCoverImageUrl());
        session.setDeckBackgroundImageUrl(deck.getBackgroundImageUrl());
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
    }

    public Showcase getByInviteToken(String inviteToken) {
        return showcaseRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
    }

    public Showcase joinShowcase(String roomCode, User player) {
        Showcase session = getByRoomCode(roomCode);

        GameStatus status = session.getStatus();
        if (status == GameStatus.FINISHED || status == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is over");
        if (status == GameStatus.IN_PROGRESS && !session.getSettings().isAllowLateJoin())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started");

        boolean alreadyJoined = session.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(player.getId()));
        if (alreadyJoined)
            return session;

        if (!session.getSettings().isAllowGuests() && Boolean.TRUE.equals(player.getIsGuest()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This game does not allow guests");

        if (session.getPlayers().size() >= session.getSettings().getMaxPlayers())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is full");

        session.getPlayers().add(playerFromUser(player));
        session = showcaseRepository.save(session);
        showcaseCache.put(session);

        // Notify the lobby of the new player list
        messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
        return session;
    }

    public void cancelShowcase(String roomCode, User requestingUser) {
        Showcase session = getByRoomCode(roomCode);
        if (!session.getHostUserId().equals(requestingUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can cancel this game");
        if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already ended");

        session.setStatus(GameStatus.CANCELLED);
        showcaseRepository.save(session);
        showcaseCache.evict(roomCode);
        messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
    }

    public Optional<ShowcaseResult> getResults(String roomCode) {
        Showcase session = getByRoomCode(roomCode);
        return showcaseResultRepository.findByShowcaseId(session.getId());
    }

    /**
     * Builds a full post-showcase review: each round's question, aggregate distribution,
     * and per-player breakdown. Only available once the showcase is FINISHED.
     */
    public ShowcaseReviewDTO buildReview(String roomCode) {
        Showcase session = getByRoomCode(roomCode);
        if (session.getStatus() != GameStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is not yet finished");

        ShowcaseResult result = showcaseResultRepository.findByShowcaseId(session.getId()).orElse(null);
        List<PlayerPlacement> placements = result != null ? result.getPlacements() : List.of();

        List<String> questionIds = session.getQuestionIds();
        Map<String, Question> questionsById = new HashMap<>();
        questionRepository.findAllById(questionIds).forEach(q -> questionsById.put(q.getId(), q));

        List<ShowcaseReviewDTO.RoundReview> rounds = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            String qid = questionIds.get(i);
            Question q = questionsById.get(qid);
            if (q == null) continue; // question deleted post-game; skip rather than crash review
            rounds.add(buildRoundReview(i, q, session));
        }

        return new ShowcaseReviewDTO(
                session.getId(),
                session.getRoomCode(),
                session.getEndedAt(),
                session.getSettings().isScoringEnabled(),
                placements,
                rounds);
    }

    /** Per-round aggregation: option counts (MCQ) or text-frequency (TEXT_INPUT), plus details. */
    private ShowcaseReviewDTO.RoundReview buildRoundReview(int roundIndex, Question q, Showcase session) {
        ElementKind kind = q.getKind() == null ? ElementKind.QUESTION : q.getKind();

        // Slides have nothing to aggregate — return a placeholder row so the review
        // panel still reflects the deck order.
        if (kind == ElementKind.SLIDE) {
            return new ShowcaseReviewDTO.RoundReview(
                    roundIndex,
                    q.getId(),
                    kind,
                    q.getType(),
                    q.getTitle(),
                    q.getQuestionText(),
                    q.getImageUrl(),
                    -1,
                    null,
                    null,
                    null,
                    null,
                    0,
                    List.of());
        }

        boolean isTextInput = q.getType() == QuestionType.TEXT_INPUT;

        List<ShowcaseReviewDTO.PlayerRoundDetail> details = new ArrayList<>();
        Map<Integer, Integer> mcqDistribution = isTextInput ? null : new LinkedHashMap<>();
        Map<String, Integer> textCounts = isTextInput ? new LinkedHashMap<>() : null;
        int timedOut = 0;

        for (ShowcasePlayer player : session.getPlayers()) {
            PlayerAnswer ans = player.getAnswers().stream()
                    .filter(a -> a.getQuestionId().equals(q.getId()))
                    .findFirst().orElse(null);
            if (ans == null) continue;

            details.add(new ShowcaseReviewDTO.PlayerRoundDetail(
                    player.getUserId(),
                    player.getUserName(),
                    ans.getSelectedOption(),
                    ans.getTextAnswer(),
                    ans.isCorrect(),
                    ans.getPointsAwarded()));

            if (isTextInput) {
                if (ans.getTextAnswer() == null || ans.getTextAnswer().isBlank()) {
                    timedOut++;
                } else {
                    String key = ans.getTextAnswer().trim();
                    textCounts.merge(key, 1, Integer::sum);
                }
            } else {
                int opt = ans.getSelectedOption();
                if (opt < 0) {
                    timedOut++;
                } else {
                    mcqDistribution.merge(opt, 1, Integer::sum);
                }
            }
        }

        List<ShowcaseReviewDTO.TextSubmission> textSubmissions = null;
        if (isTextInput) {
            String expected = q.getCorrectAnswerText() == null ? "" : q.getCorrectAnswerText().trim();
            textSubmissions = textCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(e -> new ShowcaseReviewDTO.TextSubmission(
                            e.getKey(),
                            e.getValue(),
                            e.getKey().equalsIgnoreCase(expected)))
                    .toList();
        }

        // Use the shuffled order if this MCQ was shuffled during play so the labels
        // and correct-index in the review match exactly what players answered against.
        McqShuffle reviewShuffle = session.getMcqShuffles() == null
                ? null
                : session.getMcqShuffles().get(q.getId());
        List<String> reviewOptions;
        int reviewCorrectIndex;
        String correctText;
        if (isTextInput) {
            reviewOptions = null;
            reviewCorrectIndex = -1;
            correctText = q.getCorrectAnswerText();
        } else if (reviewShuffle != null) {
            reviewOptions = reviewShuffle.getShuffledOptions();
            reviewCorrectIndex = reviewShuffle.getShuffledCorrectIndex();
            correctText = reviewShuffle.getShuffledOptions().get(reviewShuffle.getShuffledCorrectIndex());
        } else {
            reviewOptions = q.getOptions();
            reviewCorrectIndex = q.getCorrectAnswer();
            correctText = q.getOptions() != null && q.getCorrectAnswer() < q.getOptions().size()
                    ? q.getOptions().get(q.getCorrectAnswer())
                    : null;
        }

        return new ShowcaseReviewDTO.RoundReview(
                roundIndex,
                q.getId(),
                kind,
                q.getType(),
                null,
                q.getQuestionText(),
                q.getImageUrl(),
                reviewCorrectIndex,
                correctText,
                reviewOptions,
                mcqDistribution,
                textSubmissions,
                timedOut,
                details);
    }

    // ---- State machine (called by ShowcaseWebSocketController) ----

    /**
     * Host triggers: shuffle questions, flip to IN_PROGRESS, broadcast first
     * question.
     */
    public void startGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = getByRoomCode(roomCode);
            validateHost(session, principalName);

            if (session.getStatus() != GameStatus.LOBBY)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started");

            List<Question> all = questionRepository.findByDeckId(session.getDeckId());
            // Decks can mix slides and questions, where authored order matters. We
            // preserve insertion order here rather than shuffling so slides land where
            // the deck author intended. Pure-question decks lose a bit of variety;
            // a future per-deck "shuffleQuestions" setting could restore it.
            List<Question> drawn = all.stream().limit(session.getSettings().getTotalRounds()).toList();

            session.setQuestionIds(drawn.stream().map(Question::getId).toList());
            session.setStatus(GameStatus.IN_PROGRESS);
            session.setStartedAt(LocalDateTime.now());
            session.setCurrentRound(0);
            session.setRoundStartedAt(LocalDateTime.now());
            showcaseRepository.save(session);
            showcaseCache.put(session);

            broadcastRoundStart(session, drawn.get(0));
            scheduleElementTimer(session, 0, drawn.get(0));
        }
    }

    /**
     * Player submits an answer; if everyone has answered the round ends
     * immediately.
     */
    public void submitAnswer(String roomCode, AnswerSubmitRequest request, String principalName) {
        synchronized (getLock(roomCode)) {
            // Prefer the Redis-cached session (fast); fall back to MongoDB if cache
            // cold/missed
            Showcase session = loadActiveShowcase(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            String currentQuestionId = session.getQuestionIds().get(session.getCurrentRound());
            if (!currentQuestionId.equals(request.questionId()))
                return; // stale answer

            String userId = resolveUserId(principalName);
            ShowcasePlayer player = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
            if (player == null)
                return; // not in this session

            boolean alreadyAnswered = player.getAnswers().stream()
                    .anyMatch(a -> a.getQuestionId().equals(currentQuestionId));
            if (alreadyAnswered)
                return;

            Question question = questionRepository.findById(currentQuestionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

            // Slides accept no answers; silently ignore stray submissions for them.
            if (question.getKind() == ElementKind.SLIDE) return;

            McqShuffle shuffle = session.getMcqShuffles() == null
                    ? null
                    : session.getMcqShuffles().get(currentQuestionId);
            boolean isCorrect = isAnswerCorrect(question, request, shuffle);
            int points = isCorrect ? calculatePoints(question, session) : 0;

            PlayerAnswer answer = new PlayerAnswer();
            answer.setQuestionId(currentQuestionId);
            answer.setSelectedOption(request.selectedOption());
            answer.setTextAnswer(request.textAnswer());
            answer.setCorrect(isCorrect);
            answer.setPointsAwarded(points);
            answer.setAnsweredAt(LocalDateTime.now());

            player.getAnswers().add(answer);
            player.setScore(player.getScore() + points);
            // Write to Redis only — MongoDB flush happens inside completeRound
            showcaseCache.put(session);

            // Live progress: tell every client who has answered so far (no answer details)
            broadcastAnswerProgress(session, currentQuestionId);

            boolean allAnswered = session.getPlayers().stream()
                    .allMatch(p -> p.getAnswers().stream()
                            .anyMatch(a -> a.getQuestionId().equals(currentQuestionId)));
            if (allAnswered)
                completeRound(session);
        }
    }

    /**
     * Host removes a player from the showcase. Works in lobby and during play.
     * Booted player notices on the next lobby broadcast that they're no longer in the
     * player list and the frontend navigates them away.
     */
    public void bootPlayer(String roomCode, String hostPrincipalName, String targetUserId) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveShowcase(roomCode);
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

    /**
     * Host ends the showcase mid-game. Computes placements from the current state and
     * fires the same end-of-game flow as a natural finish.
     */
    public void endShowcaseEarly(String roomCode, String hostPrincipalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveShowcase(roomCode);
            validateHost(session, hostPrincipalName);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Showcase is not in progress");
            endGame(session);
        }
    }

    /**
     * Host advances to next question in TURN_BASED mode after reviewing the result.
     */
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

    /** Player leaves the lobby or an active session. */
    public void leaveGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            Showcase session = loadActiveShowcase(roomCode);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                return;

            String userId = resolveUserId(principalName);
            session.getPlayers().removeIf(p -> p.getUserId().equals(userId));
            showcaseRepository.save(session);
            showcaseCache.put(session);
            messagingTemplate.convertAndSend("/topic/showcase/" + roomCode + "/lobby", new ShowcaseDTO(session));
        }
    }

    // ---- Private state machine helpers ----

    /**
     * Called when the round timer fires; no-ops if the round was already completed.
     */
    private void handleRoundTimeout(String roomCode, int timedRound) {
        synchronized (getLock(roomCode)) {
            Showcase session = showcaseCache.get(roomCode)
                    .orElseGet(() -> showcaseRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null)
                return;
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getCurrentRound() != timedRound)
                return; // round already finished

            // Slides never go through completeRound (no answers to aggregate) —
            // their display timer just advances to the next element.
            Question current = currentElementOrNull(session);
            if (current != null && current.getKind() == ElementKind.SLIDE) {
                advanceToNextRound(session);
                return;
            }
            completeRound(session);
        }
    }

    /** Helper: look up the element for the round currently in progress. */
    private Question currentElementOrNull(Showcase session) {
        if (session.getQuestionIds().isEmpty()) return null;
        int idx = session.getCurrentRound();
        if (idx < 0 || idx >= session.getQuestionIds().size()) return null;
        return questionRepository.findById(session.getQuestionIds().get(idx)).orElse(null);
    }

    /**
     * Advance to the next element without broadcasting a round result. Used after a
     * slide times out — there's nothing to reveal, so we just kick off the next round
     * (or end the showcase if this was the last element).
     */
    private void advanceToNextRound(Showcase session) {
        boolean isLastRound = session.getCurrentRound() >= session.getQuestionIds().size() - 1;
        if (isLastRound) {
            showcaseRepository.save(session);
            endGame(session);
            return;
        }
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setRoundStartedAt(null);
        showcaseRepository.save(session);
        showcaseCache.put(session);
        // No between-rounds delay after a slide — the slide's display window already
        // gave players time to process the content.
        startNextRound(session.getRoomCode());
    }

    /**
     * Finalises a round: fills timeout answers, broadcasts ROUND_RESULT, then
     * either
     * schedules the next round (SIMULTANEOUS) or waits for host input (TURN_BASED).
     */
    private void completeRound(Showcase session) {
        String questionId = session.getQuestionIds().get(session.getCurrentRound());
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        // Fill timeout placeholder for players who never answered
        for (ShowcasePlayer player : session.getPlayers()) {
            boolean answered = player.getAnswers().stream()
                    .anyMatch(a -> a.getQuestionId().equals(questionId));
            if (!answered) {
                PlayerAnswer timeout = new PlayerAnswer();
                timeout.setQuestionId(questionId);
                timeout.setSelectedOption(-1);
                timeout.setCorrect(false);
                timeout.setPointsAwarded(0);
                timeout.setAnsweredAt(LocalDateTime.now());
                player.getAnswers().add(timeout);
            }
        }

        // Build per-player results for the broadcast
        List<RoundResultMessage.PlayerRoundResult> results = session.getPlayers().stream()
                .map(player -> {
                    PlayerAnswer ans = player.getAnswers().stream()
                            .filter(a -> a.getQuestionId().equals(questionId))
                            .findFirst().orElseThrow();
                    return new RoundResultMessage.PlayerRoundResult(
                            player.getUserId(),
                            player.getUserName(),
                            ans.getSelectedOption(),
                            ans.getTextAnswer(),
                            ans.isCorrect(),
                            ans.getPointsAwarded(),
                            player.getScore());
                })
                .toList();

        // For text-in rounds we don't have an option index; surface the canonical answer instead.
        // For MCQ rounds with a shuffle, reveal the index/text in the order players saw,
        // so the frontend's "highlight the correct bar" logic targets the right cell.
        boolean isTextInput = question.getType() == QuestionType.TEXT_INPUT;
        McqShuffle shuffle = session.getMcqShuffles() == null
                ? null
                : session.getMcqShuffles().get(questionId);
        int revealIndex;
        String revealText;
        if (isTextInput) {
            revealIndex = -1;
            revealText = question.getCorrectAnswerText();
        } else if (shuffle != null) {
            revealIndex = shuffle.getShuffledCorrectIndex();
            revealText = shuffle.getShuffledOptions().get(shuffle.getShuffledCorrectIndex());
        } else {
            revealIndex = question.getCorrectAnswer();
            revealText = question.getOptions().get(question.getCorrectAnswer());
        }

        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/roundResult",
                new RoundResultMessage(
                        session.getCurrentRound(),
                        revealIndex,
                        revealText,
                        results));

        boolean isLastRound = session.getCurrentRound() >= session.getQuestionIds().size() - 1;
        if (isLastRound) {
            showcaseRepository.save(session);
            endGame(session);
        } else {
            session.setCurrentRound(session.getCurrentRound() + 1);
            session.setRoundStartedAt(null); // set again when round actually starts
            showcaseRepository.save(session);
            showcaseCache.put(session);

            if (session.getSettings().getGameMode() == GameMode.SIMULTANEOUS) {
                String roomCode = session.getRoomCode();
                // Auto-advance after a brief pause so players can see the result screen
                scheduler.schedule(() -> {
                    try {
                        startNextRound(roomCode);
                    } catch (Exception ignored) {
                    }
                }, BETWEEN_ROUNDS_DELAY_SECONDS, TimeUnit.SECONDS);
            }
            // TURN_BASED: host must send /app/showcase/{roomCode}/nextRound to continue
        }
    }

    /**
     * Stamps the round start time, broadcasts the question, and starts the
     * countdown.
     */
    private void startNextRound(String roomCode) {
        synchronized (getLock(roomCode)) {
            Showcase session = showcaseCache.get(roomCode)
                    .orElseGet(() -> showcaseRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null || session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            session.setRoundStartedAt(LocalDateTime.now());
            showcaseRepository.save(session);
            showcaseCache.put(session);

            String questionId = session.getQuestionIds().get(session.getCurrentRound());
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

            broadcastRoundStart(session, question);
            scheduleElementTimer(session, session.getCurrentRound(), question);
        }
    }

    /**
     * Ranks players, persists ShowcaseResult, updates each registered player's global
     * stats.
     */
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
            p.setTotalQuestions(session.getQuestionIds().size());
            placements.add(p);
        }

        ShowcaseResult result = new ShowcaseResult();
        result.setShowcaseId(session.getId());
        result.setPlacements(placements);
        showcaseResultRepository.save(result);
        showcaseRepository.save(session);
        showcaseCache.evict(session.getRoomCode()); // game is over; no more hot-path reads needed

        // Update global PlayerStats for registered players only (guests don't have
        // persistent stats)
        for (PlayerPlacement p : placements) {
            if (!p.isGuest()) {
                updateStatsAfterGame(p.getUserId(), p.getFinalScore(), p.getPlacement() == 1);
            }
        }

        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/gameOver",
                new ShowcaseEndedMessage(placements));
    }

    // ---- Scoring ----

    /**
     * Determines whether a submitted answer matches the question's expected answer.
     * MCQ: exact index match against correctAnswer — but when a per-round shuffle
     * exists, the player's selectedOption is the index in shuffledOptions, so we
     * compare against shuffledCorrectIndex instead.
     * TEXT_INPUT: case-insensitive trimmed equality against correctAnswerText.
     */
    private boolean isAnswerCorrect(Question question, AnswerSubmitRequest request, McqShuffle shuffle) {
        if (question.getType() == QuestionType.TEXT_INPUT) {
            String submitted = request.textAnswer();
            String expected = question.getCorrectAnswerText();
            if (submitted == null || expected == null) return false;
            return submitted.trim().equalsIgnoreCase(expected.trim());
        }
        int expected = shuffle != null ? shuffle.getShuffledCorrectIndex() : question.getCorrectAnswer();
        return request.selectedOption() == expected;
    }

    /**
     * Returns points for a correct answer.
     * In SIMULTANEOUS mode with speedBonus enabled: base + up to 50% bonus
     * for answering quickly. Linear decay from full bonus (instant answer)
     * to zero bonus (answered at the very last second).
     */
    private int calculatePoints(Question question, Showcase session) {
        int base = question.getPointValue();
        if (!session.getSettings().isSpeedBonus()
                || session.getSettings().getGameMode() != GameMode.SIMULTANEOUS
                || session.getRoundStartedAt() == null) {
            return base;
        }
        long totalMillis = question.getTimeLimit() * 1000L;
        long elapsed = Duration.between(session.getRoundStartedAt(), LocalDateTime.now()).toMillis();
        elapsed = Math.min(Math.max(elapsed, 0), totalMillis);
        double speedFraction = 1.0 - ((double) elapsed / totalMillis);
        return base + (int) (base * 0.5 * speedFraction);
    }

    // ---- Stats update ----

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

    // ---- Broadcast helpers ----

    /**
     * Live "who has answered" progress for the in-game player list. Sent on every
     * answer submission so clients can render a ✓ next to players as they answer.
     */
    private void broadcastAnswerProgress(Showcase session, String questionId) {
        List<String> answeredUserIds = session.getPlayers().stream()
                .filter(p -> p.getAnswers().stream()
                        .anyMatch(a -> a.getQuestionId().equals(questionId)))
                .map(ShowcasePlayer::getUserId)
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/answered",
                new AnswerProgressMessage(
                        session.getCurrentRound(),
                        answeredUserIds,
                        session.getPlayers().size()));
    }

    private void broadcastRoundStart(Showcase session, Question question) {
        QuestionDTO payload = buildBroadcastQuestion(session, question);
        messagingTemplate.convertAndSend(
                "/topic/showcase/" + session.getRoomCode() + "/round",
                new RoundStartMessage(
                        session.getCurrentRound(),
                        session.getSettings().getTotalRounds(),
                        payload,
                        session.getRoundStartedAt()));
    }

    /**
     * Returns the QuestionDTO clients will render. For MCQ questions with the
     * shuffle setting enabled, picks (or reuses) a per-question option order and
     * stores it on the session so scoring + review see the same labels everyone
     * answered against.
     */
    private QuestionDTO buildBroadcastQuestion(Showcase session, Question question) {
        boolean shuffleable = question.getKind() != ElementKind.SLIDE
                && question.getType() == QuestionType.MULTIPLE_CHOICE
                && question.getOptions() != null
                && question.getOptions().size() > 1;
        if (!shuffleable || !session.getSettings().isShuffleMcqOptions()) {
            return new QuestionDTO(question);
        }
        McqShuffle shuffle = ensureMcqShuffle(session, question);
        return new QuestionDTO(question, shuffle.getShuffledOptions());
    }

    /** Looks up the persisted shuffle for this question; creates one on first use. */
    private McqShuffle ensureMcqShuffle(Showcase session, Question question) {
        Map<String, McqShuffle> shuffles = session.getMcqShuffles();
        if (shuffles == null) {
            shuffles = new HashMap<>();
            session.setMcqShuffles(shuffles);
        }
        McqShuffle existing = shuffles.get(question.getId());
        if (existing != null) return existing;

        List<String> originalOptions = question.getOptions();
        int n = originalOptions.size();
        List<Integer> permutation = new ArrayList<>();
        for (int i = 0; i < n; i++) permutation.add(i);
        java.util.Collections.shuffle(permutation, secureRandom);

        List<String> shuffledOptions = new ArrayList<>(n);
        int shuffledCorrectIndex = -1;
        for (int i = 0; i < n; i++) {
            int originalIdx = permutation.get(i);
            shuffledOptions.add(originalOptions.get(originalIdx));
            if (originalIdx == question.getCorrectAnswer()) shuffledCorrectIndex = i;
        }

        McqShuffle snap = new McqShuffle();
        snap.setQuestionId(question.getId());
        snap.setShuffledOptions(shuffledOptions);
        snap.setShuffledCorrectIndex(shuffledCorrectIndex);
        shuffles.put(question.getId(), snap);
        // Persist so a cache miss or restart can still resolve the shuffle.
        showcaseRepository.save(session);
        showcaseCache.put(session);
        return snap;
    }

    private void scheduleRoundTimer(String roomCode, int round, int timeLimitSeconds) {
        scheduler.schedule(() -> {
            try {
                handleRoundTimeout(roomCode, round);
            } catch (Exception ignored) {
            }
        }, timeLimitSeconds, TimeUnit.SECONDS);
    }

    /**
     * Wrapper around scheduleRoundTimer that respects the host's timePerQuestion
     * setting. timePerQuestion == 0 means "unlimited" — no timeout is scheduled
     * and the question round only ends when all players have answered
     * (SIMULTANEOUS) or the host advances (TURN_BASED). Per-element timeLimit
     * is the actual countdown duration when the timer is on.
     */
    private void scheduleRoundTimerIfEnabled(Showcase session, int round, int timeLimitSeconds) {
        if (session.getSettings().getTimePerQuestion() <= 0) return;
        scheduleRoundTimer(session.getRoomCode(), round, timeLimitSeconds);
    }

    /**
     * Element-aware timer: slides always schedule a display timer (otherwise they'd
     * hang forever — no player can advance them by answering), even when the host
     * has timePerQuestion = 0 for questions. Questions defer to the timePerQuestion
     * helper above.
     */
    private void scheduleElementTimer(Showcase session, int round, Question element) {
        if (element.getKind() == ElementKind.SLIDE) {
            int display = element.getTimeLimit() > 0 ? element.getTimeLimit() : 8;
            scheduleRoundTimer(session.getRoomCode(), round, display);
        } else {
            scheduleRoundTimerIfEnabled(session, round, element.getTimeLimit());
        }
    }

    // ---- Utility ----

    private void validateHost(Showcase session, String principalName) {
        String userId = resolveUserId(principalName);
        if (!session.getHostUserId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action");
    }

    /** Maps a Spring Security principal name to a User.id. */
    private String resolveUserId(String principalName) {
        if (principalName == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        if (principalName.startsWith("guest:"))
            return principalName.substring(6);
        return userRepository.findByGoogleId(principalName)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Cache-first session load: Redis hit is the fast path; MongoDB is the
     * fallback.
     */
    private Showcase loadActiveShowcase(String roomCode) {
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
}
