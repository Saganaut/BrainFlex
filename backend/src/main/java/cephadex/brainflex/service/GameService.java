/**
 * Core service for game session lifecycle: creation, joining, and the full
 * round-by-round state machine (LOBBY → IN_PROGRESS → FINISHED).
 *
 * State machine overview:
 *   startGame()      → draws questions, broadcasts ROUND_START, starts timer
 *   submitAnswer()   → records answer; if all players answered, ends round early
 *   timer fires      → ends round for any unanswered players
 *   completeRound()  → broadcasts ROUND_RESULT; advances or ends game
 *   endGame()        → ranks players, saves GameResult, updates PlayerStats, broadcasts GAME_OVER
 *
 * Per-round locking via a ConcurrentHashMap of per-roomCode locks prevents race
 * conditions between simultaneous answer submissions and the expiry timer.
 * GameCacheService keeps active session state in Redis so answer submissions
 * read/write Redis instead of MongoDB; MongoDB is only written at round boundaries.
 */
package cephadex.brainflex.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.CreateGameRequest;
import cephadex.brainflex.dto.GameOverMessage;
import cephadex.brainflex.dto.GameSessionDTO;
import cephadex.brainflex.dto.QuestionDTO;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.RoundStartMessage;
import cephadex.brainflex.model.GameResult;
import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.GameSettings;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.SessionPlayer;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;
import cephadex.brainflex.repository.ContentPackRepository;
import cephadex.brainflex.repository.GameResultRepository;
import cephadex.brainflex.repository.GameSessionRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;
import jakarta.annotation.PreDestroy;

@Service
public class GameService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 10;
    // Delay in seconds between ROUND_RESULT broadcast and the next ROUND_START in
    // SIMULTANEOUS mode
    private static final int BETWEEN_ROUNDS_DELAY_SECONDS = 4;

    private final GameSessionRepository gameSessionRepository;
    private final ContentPackRepository contentPackRepository;
    private final QuestionRepository questionRepository;
    private final GameResultRepository gameResultRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameCacheService gameCache;
    private final SecureRandom secureRandom = new SecureRandom();

    // Per-roomCode locks prevent concurrent answer/timer races on the same session
    private final ConcurrentHashMap<String, Object> roundLocks = new ConcurrentHashMap<>();
    // Shared pool for round timers and between-round delays
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // @Lazy breaks the potential circular dependency with SimpMessagingTemplate at
    // startup
    public GameService(
            GameSessionRepository gameSessionRepository,
            ContentPackRepository contentPackRepository,
            QuestionRepository questionRepository,
            GameResultRepository gameResultRepository,
            UserRepository userRepository,
            GameCacheService gameCache,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.gameSessionRepository = gameSessionRepository;
        this.contentPackRepository = contentPackRepository;
        this.questionRepository = questionRepository;
        this.gameResultRepository = gameResultRepository;
        this.userRepository = userRepository;
        this.gameCache = gameCache;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ---- Session CRUD (used by GameController REST endpoints) ----

    public GameSession createSession(User host, CreateGameRequest request) {
        contentPackRepository.findById(request.contentPackId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content pack not found"));

        int available = questionRepository.countByContentPackId(request.contentPackId());
        if (available == 0)
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "Content pack has no questions");

        GameSettings settings = new GameSettings();
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
        settings.setTotalRounds(Math.min(settings.getTotalRounds(), available));

        GameSession session = new GameSession();
        session.setType(GameType.TRIVIA);
        session.setHostUserId(host.getId());
        session.setContentPackId(request.contentPackId());
        session.setSettings(settings);
        session.setRoomCode(generateUniqueRoomCode());
        session.setInviteToken(UUID.randomUUID().toString());
        session.getPlayers().add(playerFromUser(host));

        session = gameSessionRepository.save(session);
        gameCache.put(session);
        return session;
    }

    public GameSession getByRoomCode(String roomCode) {
        return gameSessionRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
    }

    public GameSession getByInviteToken(String inviteToken) {
        return gameSessionRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
    }

    public GameSession joinSession(String roomCode, User player) {
        GameSession session = getByRoomCode(roomCode);

        if (session.getStatus() != GameStatus.LOBBY)
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
        session = gameSessionRepository.save(session);
        gameCache.put(session);

        // Notify the lobby of the new player list
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/lobby", new GameSessionDTO(session));
        return session;
    }

    public void cancelSession(String roomCode, User requestingUser) {
        GameSession session = getByRoomCode(roomCode);
        if (!session.getHostUserId().equals(requestingUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can cancel this game");
        if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already ended");

        session.setStatus(GameStatus.CANCELLED);
        gameSessionRepository.save(session);
        gameCache.evict(roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/lobby", new GameSessionDTO(session));
    }

    public Optional<GameResult> getResults(String roomCode) {
        GameSession session = getByRoomCode(roomCode);
        return gameResultRepository.findByGameSessionId(session.getId());
    }

    // ---- State machine (called by GameWebSocketController) ----

    /**
     * Host triggers: shuffle questions, flip to IN_PROGRESS, broadcast first
     * question.
     */
    public void startGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            GameSession session = getByRoomCode(roomCode);
            validateHost(session, principalName);

            if (session.getStatus() != GameStatus.LOBBY)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started");

            List<Question> all = questionRepository.findByContentPackId(session.getContentPackId());
            Collections.shuffle(all);
            List<Question> drawn = all.stream().limit(session.getSettings().getTotalRounds()).toList();

            session.setQuestionIds(drawn.stream().map(Question::getId).toList());
            session.setStatus(GameStatus.IN_PROGRESS);
            session.setStartedAt(LocalDateTime.now());
            session.setCurrentRound(0);
            session.setRoundStartedAt(LocalDateTime.now());
            gameSessionRepository.save(session);
            gameCache.put(session);

            broadcastRoundStart(session, drawn.get(0));
            scheduleRoundTimer(roomCode, 0, drawn.get(0).getTimeLimit());
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
            GameSession session = loadActiveSession(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            String currentQuestionId = session.getQuestionIds().get(session.getCurrentRound());
            if (!currentQuestionId.equals(request.questionId()))
                return; // stale answer

            String userId = resolveUserId(principalName);
            SessionPlayer player = session.getPlayers().stream()
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

            boolean isCorrect = request.selectedOption() == question.getCorrectAnswer();
            int points = isCorrect ? calculatePoints(question, session) : 0;

            PlayerAnswer answer = new PlayerAnswer();
            answer.setQuestionId(currentQuestionId);
            answer.setSelectedOption(request.selectedOption());
            answer.setCorrect(isCorrect);
            answer.setPointsAwarded(points);
            answer.setAnsweredAt(LocalDateTime.now());

            player.getAnswers().add(answer);
            player.setScore(player.getScore() + points);
            // Write to Redis only — MongoDB flush happens inside completeRound
            gameCache.put(session);

            boolean allAnswered = session.getPlayers().stream()
                    .allMatch(p -> p.getAnswers().stream()
                            .anyMatch(a -> a.getQuestionId().equals(currentQuestionId)));
            if (allAnswered)
                completeRound(session);
        }
    }

    /**
     * Host advances to next question in TURN_BASED mode after reviewing the result.
     */
    public void nextRound(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            GameSession session = getByRoomCode(roomCode);
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
            GameSession session = loadActiveSession(roomCode);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                return;

            String userId = resolveUserId(principalName);
            session.getPlayers().removeIf(p -> p.getUserId().equals(userId));
            gameSessionRepository.save(session);
            gameCache.put(session);
            messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/lobby", new GameSessionDTO(session));
        }
    }

    // ---- Private state machine helpers ----

    /**
     * Called when the round timer fires; no-ops if the round was already completed.
     */
    private void handleRoundTimeout(String roomCode, int timedRound) {
        synchronized (getLock(roomCode)) {
            GameSession session = gameCache.get(roomCode)
                    .orElseGet(() -> gameSessionRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null)
                return;
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getCurrentRound() != timedRound)
                return; // round already finished
            completeRound(session);
        }
    }

    /**
     * Finalises a round: fills timeout answers, broadcasts ROUND_RESULT, then
     * either
     * schedules the next round (SIMULTANEOUS) or waits for host input (TURN_BASED).
     */
    private void completeRound(GameSession session) {
        String questionId = session.getQuestionIds().get(session.getCurrentRound());
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        // Fill timeout placeholder for players who never answered
        for (SessionPlayer player : session.getPlayers()) {
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
                            ans.isCorrect(),
                            ans.getPointsAwarded(),
                            player.getScore());
                })
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/game/" + session.getRoomCode() + "/roundResult",
                new RoundResultMessage(
                        session.getCurrentRound(),
                        question.getCorrectAnswer(),
                        question.getOptions().get(question.getCorrectAnswer()),
                        results));

        boolean isLastRound = session.getCurrentRound() >= session.getQuestionIds().size() - 1;
        if (isLastRound) {
            gameSessionRepository.save(session);
            endGame(session);
        } else {
            session.setCurrentRound(session.getCurrentRound() + 1);
            session.setRoundStartedAt(null); // set again when round actually starts
            gameSessionRepository.save(session);
            gameCache.put(session);

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
            // TURN_BASED: host must send /app/game/{roomCode}/nextRound to continue
        }
    }

    /**
     * Stamps the round start time, broadcasts the question, and starts the
     * countdown.
     */
    private void startNextRound(String roomCode) {
        synchronized (getLock(roomCode)) {
            GameSession session = gameCache.get(roomCode)
                    .orElseGet(() -> gameSessionRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null || session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            session.setRoundStartedAt(LocalDateTime.now());
            gameSessionRepository.save(session);
            gameCache.put(session);

            String questionId = session.getQuestionIds().get(session.getCurrentRound());
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

            broadcastRoundStart(session, question);
            scheduleRoundTimer(roomCode, session.getCurrentRound(), question.getTimeLimit());
        }
    }

    /**
     * Ranks players, persists GameResult, updates each registered player's global
     * stats.
     */
    private void endGame(GameSession session) {
        session.setStatus(GameStatus.FINISHED);
        session.setEndedAt(LocalDateTime.now());

        List<SessionPlayer> ranked = session.getPlayers().stream()
                .sorted((a, b) -> b.getScore() - a.getScore())
                .toList();

        List<PlayerPlacement> placements = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            SessionPlayer sp = ranked.get(i);
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

        GameResult result = new GameResult();
        result.setGameSessionId(session.getId());
        result.setPlacements(placements);
        gameResultRepository.save(result);
        gameSessionRepository.save(session);
        gameCache.evict(session.getRoomCode()); // game is over; no more hot-path reads needed

        // Update global PlayerStats for registered players only (guests don't have
        // persistent stats)
        for (PlayerPlacement p : placements) {
            if (!p.isGuest()) {
                updateStatsAfterGame(p.getUserId(), p.getFinalScore(), p.getPlacement() == 1);
            }
        }

        messagingTemplate.convertAndSend(
                "/topic/game/" + session.getRoomCode() + "/gameOver",
                new GameOverMessage(placements));
    }

    // ---- Scoring ----

    /**
     * Returns points for a correct answer.
     * In SIMULTANEOUS mode with speedBonus enabled: base + up to 50% bonus
     * for answering quickly. Linear decay from full bonus (instant answer)
     * to zero bonus (answered at the very last second).
     */
    private int calculatePoints(Question question, GameSession session) {
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

    private void broadcastRoundStart(GameSession session, Question question) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + session.getRoomCode() + "/round",
                new RoundStartMessage(
                        session.getCurrentRound(),
                        session.getSettings().getTotalRounds(),
                        new QuestionDTO(question),
                        session.getRoundStartedAt()));
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

    private void validateHost(GameSession session, String principalName) {
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
    private GameSession loadActiveSession(String roomCode) {
        return gameCache.get(roomCode).orElseGet(() -> getByRoomCode(roomCode));
    }

    private Object getLock(String roomCode) {
        return roundLocks.computeIfAbsent(roomCode, k -> new Object());
    }

    private SessionPlayer playerFromUser(User user) {
        SessionPlayer p = new SessionPlayer();
        p.setUserId(user.getId());
        p.setUserName(user.getUserName());
        p.setPictureUrl(user.getPictureUrl());
        p.setGuest(Boolean.TRUE.equals(user.getIsGuest()));
        return p;
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (gameSessionRepository.findByRoomCode(code).isEmpty())
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
