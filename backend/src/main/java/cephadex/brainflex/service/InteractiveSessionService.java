/**
 * Core service for interactiveSession lifecycle and the round-by-round state machine.
 *
 * State machine:
 *   startInteractiveSession()  → freezes deck.elements into deckSnapshot, broadcasts ROUND_START
 *   submitAnswer()   → records polymorphic AnswerPayload; ends round if all answered
 *   timer fires      → ends round for unanswered players (or advances if SLIDE)
 *   completeRound()  → broadcasts ROUND_RESULT; advances or ends
 *   endInteractiveSession()    → ranks players, writes InteractiveSessionResult, updates stats
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.dto.AnswerProgressMessage;
import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.ChatSendRequest;
import cephadex.brainflex.dto.CreateInteractiveSessionRequest;
import cephadex.brainflex.dto.ReactionBroadcastMessage;
import cephadex.brainflex.dto.ReactionSendRequest;
import cephadex.brainflex.dto.RoundResultMessage;
import cephadex.brainflex.dto.RoundStartMessage;
import cephadex.brainflex.dto.InteractiveSessionChatMessageDTO;
import cephadex.brainflex.dto.InteractiveSessionDTO;
import cephadex.brainflex.dto.InteractiveSessionEndedMessage;
import cephadex.brainflex.dto.InteractiveSessionReviewDTO;
import cephadex.brainflex.dto.TeamUpdateMessage;
import cephadex.brainflex.dto.VotePhaseStartMessage;
import cephadex.brainflex.dto.VoteProgressMessage;
import cephadex.brainflex.dto.VoteSubmitRequest;
import cephadex.brainflex.dto.WordCloudUpdateMessage;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.Reaction;
import cephadex.brainflex.model.RoundVote;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionChatMessage;
import cephadex.brainflex.model.InteractiveSessionPlayer;
import cephadex.brainflex.model.InteractiveSessionResult;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.Team;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.answer.DrawingAnswer;
import cephadex.brainflex.model.answer.Stroke;
import cephadex.brainflex.model.answer.TimeoutAnswer;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.InteractiveSessionPhase;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ReactionRepository;
import cephadex.brainflex.repository.InteractiveSessionChatMessageRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.InteractiveSessionResultRepository;
import cephadex.brainflex.repository.UserRepository;
import jakarta.annotation.PreDestroy;

@Service
public class InteractiveSessionService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final int BETWEEN_ROUNDS_DELAY_SECONDS = 4;
    private static final int SLIDE_DEFAULT_SECONDS = 8;

    private final InteractiveSessionRepository interactiveSessionRepository;
    private final DeckRepository deckRepository;
    private final InteractiveSessionResultRepository interactiveSessionResultRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final InteractiveSessionCacheService interactiveSessionCache;
    private final AuthorizationService authorizationService;
    private final DeckImageHydrationService deckImageHydrationService;
    private final DeckService deckService;
    private final UserImageHydrator userImageHydrator;
    private final ReactionRepository reactionRepository;
    private final InteractiveSessionChatMessageRepository chatRepository;
    private final InteractiveSessionRateLimiter rateLimiter;
    private final AvatarService avatarService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int CHAT_MAX_BODY = 500;
    private static final int CHAT_PAGE_MAX_SIZE = 100;

    private final ConcurrentHashMap<String, Object> roundLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Per-answer byte cap for DRAWING submissions. Inline stroke lists can get
     * large; we count the serialized JSON and silently drop anything over the
     * cap so a single PlayerAnswer can't blow past Mongo's 16 MB document
     * limit. Default 256 KB.
     */
    @Value("${app.drawing.max-payload-bytes:262144}")
    private int drawingMaxPayloadBytes = 262144;

    /** Used to count the serialized size of {@link DrawingAnswer} submissions. */
    private final ObjectMapper objectMapper;

    public InteractiveSessionService(
            InteractiveSessionRepository interactiveSessionRepository,
            DeckRepository deckRepository,
            InteractiveSessionResultRepository interactiveSessionResultRepository,
            UserRepository userRepository,
            InteractiveSessionCacheService interactiveSessionCache,
            AuthorizationService authorizationService,
            DeckImageHydrationService deckImageHydrationService,
            DeckService deckService,
            UserImageHydrator userImageHydrator,
            ReactionRepository reactionRepository,
            InteractiveSessionChatMessageRepository chatRepository,
            InteractiveSessionRateLimiter rateLimiter,
            AvatarService avatarService,
            ObjectMapper objectMapper,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.interactiveSessionRepository = interactiveSessionRepository;
        this.deckRepository = deckRepository;
        this.interactiveSessionResultRepository = interactiveSessionResultRepository;
        this.userRepository = userRepository;
        this.interactiveSessionCache = interactiveSessionCache;
        this.authorizationService = authorizationService;
        this.deckImageHydrationService = deckImageHydrationService;
        this.deckService = deckService;
        this.userImageHydrator = userImageHydrator;
        this.reactionRepository = reactionRepository;
        this.chatRepository = chatRepository;
        this.rateLimiter = rateLimiter;
        this.avatarService = avatarService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ---- CRUD ----

    public InteractiveSession createInteractiveSession(User host, CreateInteractiveSessionRequest request) {
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
        InteractiveSessionSettings settings = copyOf(deck.getDefaultSettings());
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
        if (request.reactionsEnabled() != null)
            settings.setReactionsEnabled(request.reactionsEnabled());
        if (request.chatEnabled() != null)
            settings.setChatEnabled(request.chatEnabled());
        if (request.teamMode() != null)
            settings.setTeamMode(request.teamMode());
        if (request.teamCount() != null)
            settings.setTeamCount(request.teamCount());
        if (request.autoBalanceTeams() != null)
            settings.setAutoBalanceTeams(request.autoBalanceTeams());
        // Chunk 13 — live-show polish knobs.
        if (request.shuffleQuestions() != null)
            settings.setShuffleQuestions(request.shuffleQuestions());
        if (request.shuffleAnswers() != null)
            settings.setShuffleAnswers(request.shuffleAnswers());
        if (request.autoAdvance() != null)
            settings.setAutoAdvance(request.autoAdvance());
        if (request.podiumDuration() != null)
            settings.setPodiumDuration(request.podiumDuration());
        if (request.lobbyCountdownSeconds() != null)
            settings.setLobbyCountdownSeconds(request.lobbyCountdownSeconds());
        if (request.requireFullName() != null)
            settings.setRequireFullName(request.requireFullName());
        if (request.spectatorsAllowed() != null)
            settings.setSpectatorsAllowed(request.spectatorsAllowed());
        // totalRounds is upper-bounded by the deck's actual element count.
        settings.setTotalRounds(Math.min(settings.getTotalRounds(), elements.size()));

        // Frozen snapshot of the elements as authored — drawn in deck order, truncated
        // to totalRounds. Slides participate in deck order; we never shuffle.
        List<DeckElement> snapshot = new ArrayList<>(elements.subList(0, settings.getTotalRounds()));

        InteractiveSession session = new InteractiveSession();
        session.setHostUserId(host.getId());
        session.setDeckId(request.deckId());
        session.setDeckSnapshot(snapshot);
        session.setDeckCoverImageUrl(urlOf(deck.getCover()));
        session.setDeckBackgroundImageUrl(urlOf(deck.getBackground()));
        session.setThemeId(deck.getThemeId());
        session.setSettings(settings);

        // Chunk 13 — customRoomCode is the host-typed override. We validate the
        // shape via @Pattern on the request, then collision-check against
        // existing interactiveSessions. The auto-generator owns the rest.
        String customCode = request.customRoomCode();
        if (customCode != null && !customCode.isBlank()) {
            String normalised = customCode.toUpperCase();
            if (interactiveSessionRepository.findByRoomCode(normalised).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Room code is already in use");
            }
            session.setCustomRoomCode(normalised);
            session.setRoomCode(normalised);
        } else {
            session.setRoomCode(generateUniqueRoomCode());
        }
        session.setInviteToken(UUID.randomUUID().toString());

        // Chunk 13 — denorm host display fields so the lobby header doesn't
        // round-trip through UserRepository on every refresh.
        session.setHostName(host.getName() != null ? host.getName() : host.getUserName());
        session.setHostAvatarUrl(userImageHydrator.pictureUrlOf(host));
        session.setLobbyOpenedAt(LocalDateTime.now());
        if (request.anonymousMode() != null) {
            session.setAnonymousMode(request.anonymousMode());
        }

        // Seed default teams + mirror the team-mode flags onto the session
        // before adding the host, so the host can be auto-balanced into the
        // first team and team.memberCount is in sync from the start.
        if (settings.isTeamMode()) {
            session.setTeamMode(true);
            session.setAutoBalanceTeams(settings.isAutoBalanceTeams());
            session.setTeams(buildDefaultTeams(settings.getTeamCount()));
        }

        InteractiveSessionPlayer hostPlayer = playerFromUser(host);
        if (session.isTeamMode()) {
            assignPlayerToTeam(session, hostPlayer, null);
        }
        session.getPlayers().add(hostPlayer);

        session = interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);
        return session;
    }

    /** Sequential default team names + palette tokens. */
    private static final List<String> DEFAULT_TEAM_NAMES = List.of(
            "Red Lions", "Blue Sharks", "Green Dragons", "Yellow Phoenix",
            "Violet Owls", "Teal Wolves", "Orange Tigers", "Pink Pandas");
    private static final List<String> DEFAULT_TEAM_COLORS = List.of(
            "red", "blue", "green", "yellow", "violet", "teal", "orange", "pink");

    private static List<Team> buildDefaultTeams(int count) {
        int clamped = Math.max(2, Math.min(count, DEFAULT_TEAM_NAMES.size()));
        List<Team> out = new ArrayList<>(clamped);
        for (int i = 0; i < clamped; i++) {
            Team team = new Team();
            team.setId(UUID.randomUUID().toString());
            team.setName(DEFAULT_TEAM_NAMES.get(i));
            team.setColor(DEFAULT_TEAM_COLORS.get(i));
            out.add(team);
        }
        return out;
    }

    /**
     * Place a player into a team. The preferred {@code teamId} is honored
     * when non-null and references an existing team; otherwise we pick the
     * smallest team (round-robin tie-break by current team order). The
     * captain slot is filled by the first joiner.
     *
     * Mutates the team's score + memberCount in place but does NOT touch
     * persistence — callers save the parent interactiveSession.
     */
    private static void assignPlayerToTeam(InteractiveSession session, InteractiveSessionPlayer player, String preferredTeamId) {
        List<Team> teams = session.getTeams();
        if (teams == null || teams.isEmpty())
            return;

        Team target = null;
        if (preferredTeamId != null) {
            target = teams.stream()
                    .filter(t -> preferredTeamId.equals(t.getId()))
                    .findFirst().orElse(null);
        }
        if (target == null) {
            target = teams.stream()
                    .min((a, b) -> Integer.compare(a.getMemberCount(), b.getMemberCount()))
                    .orElseThrow();
        }

        player.setTeamId(target.getId());
        target.setMemberCount(target.getMemberCount() + 1);
        if (target.getCaptainUserId() == null) {
            target.setCaptainUserId(player.getUserId());
        }
    }

    public InteractiveSession getByRoomCode(String roomCode) {
        return interactiveSessionRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InteractiveSession not found"));
    }

    public InteractiveSession getByInviteToken(String inviteToken) {
        return interactiveSessionRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InteractiveSession not found"));
    }

    public InteractiveSession joinInteractiveSession(String roomCode, User player) {
        return joinInteractiveSession(roomCode, player, null, null, null);
    }

    public InteractiveSession joinInteractiveSession(String roomCode, User player, String preferredTeamId) {
        return joinInteractiveSession(roomCode, player, preferredTeamId, null, null);
    }

    public InteractiveSession joinInteractiveSession(String roomCode, User player, String preferredTeamId,
                                  String avatarKey, String colorTag) {
        InteractiveSession session = getByRoomCode(roomCode);

        GameStatus status = session.getStatus();
        if (status == GameStatus.FINISHED || status == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is over");
        if (status == GameStatus.IN_PROGRESS && !session.getSettings().isAllowLateJoin())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession has already started");

        boolean alreadyJoined = session.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(player.getId()));
        if (alreadyJoined)
            return session;

        if (!session.getSettings().isAllowGuests() && Boolean.TRUE.equals(player.getIsGuest()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This interactiveSession does not allow guests");

        // Chunk 13 — requireFullName disallows nickname-only joins. Guests
        // have no registered name on file, so requireFullName implicitly
        // blocks them too (even if allowGuests is on).
        if (session.getSettings().isRequireFullName()) {
            if (Boolean.TRUE.equals(player.getIsGuest()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This interactiveSession requires a real account (guests not allowed)");
            if (player.getName() == null || player.getName().isBlank())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This interactiveSession requires a full name on your account");
        }

        if (session.getPlayers().size() >= session.getSettings().getMaxPlayers())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is full");

        InteractiveSessionPlayer newPlayer = playerFromUser(player);

        // Chunk 13 — lobby avatar pick. Unknown keys are silently dropped so
        // a client with a stale preset list doesn't trip a 400; the player
        // just falls back to their real pictureUrl.
        if (avatarService.has(avatarKey)) {
            newPlayer.setAvatarKey(avatarKey);
            if (colorTag == null || colorTag.isBlank()) {
                newPlayer.setColorTag(avatarService.get(avatarKey).colorTag());
            } else {
                newPlayer.setColorTag(colorTag);
            }
        } else if (colorTag != null && !colorTag.isBlank()) {
            newPlayer.setColorTag(colorTag);
        }

        // Chunk 13 — late-join tag is purely informational on the player
        // record (lobby vs. mid-game arrival); allowLateJoin is the gate.
        if (status == GameStatus.IN_PROGRESS) {
            newPlayer.setLateJoin(true);
        }

        if (session.isTeamMode()) {
            // Manual team mode demands an explicit, valid teamId. Auto-balance
            // mode treats the value as a hint — assignPlayerToTeam picks the
            // smallest team when the hint is missing or invalid.
            if (!session.isAutoBalanceTeams()) {
                if (preferredTeamId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "teamId is required when auto-balance is disabled");
                }
                boolean teamExists = session.getTeams().stream()
                        .anyMatch(t -> preferredTeamId.equals(t.getId()));
                if (!teamExists) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown team");
                }
            }
            assignPlayerToTeam(session, newPlayer, preferredTeamId);
        }
        session.getPlayers().add(newPlayer);

        session = interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);

        messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
        if (session.isTeamMode()) {
            broadcastTeamUpdate(session);
        }
        return session;
    }

    /**
     * Chunk 13 — flip {@code disconnected} + bump {@code lastSeenAt} on the
     * caller's {@link InteractiveSessionPlayer} record in every IN_PROGRESS interactiveSession
     * they're part of. Called by {@link PresenceService} on WS connect /
     * disconnect events. A player can only really be in one active game at
     * a time, but the query is over all matching IN_PROGRESS interactiveSessions so
     * the same code path handles "host who also joined" or any weird
     * cross-game state.
     *
     * Falls open on every failure path — presence is best-effort and must
     * not break the connection lifecycle.
     */
    public void markPlayerPresence(String userId, boolean online) {
        if (userId == null)
            return;
        try {
            List<InteractiveSession> sessions = interactiveSessionRepository.findByStatusAndPlayersUserId(
                    GameStatus.IN_PROGRESS, userId);
            LocalDateTime now = LocalDateTime.now();
            for (InteractiveSession session : sessions) {
                synchronized (getLock(session.getRoomCode())) {
                    InteractiveSession live = loadActiveSession(session.getRoomCode());
                    boolean dirty = false;
                    for (InteractiveSessionPlayer player : live.getPlayers()) {
                        if (!userId.equals(player.getUserId()))
                            continue;
                        boolean wantDisconnected = !online;
                        if (player.isDisconnected() != wantDisconnected) {
                            player.setDisconnected(wantDisconnected);
                            dirty = true;
                        }
                        player.setLastSeenAt(now);
                        dirty = true;
                    }
                    if (dirty) {
                        interactiveSessionRepository.save(live);
                        interactiveSessionCache.put(live);
                        messagingTemplate.convertAndSend(
                                "/topic/interactive-session/" + live.getRoomCode() + "/lobby",
                                new InteractiveSessionDTO(live));
                    }
                }
            }
        } catch (Exception ignored) {
            // Presence is best-effort.
        }
    }

    public void cancelInteractiveSession(String roomCode, User requestingUser) {
        InteractiveSession session = authorizationService.requireInteractiveSessionHost(roomCode, requestingUser);
        if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is already ended");

        session.setStatus(GameStatus.CANCELLED);
        interactiveSessionRepository.save(session);
        interactiveSessionCache.evict(roomCode);
        messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
    }

    public Optional<InteractiveSessionResult> getResults(String roomCode) {
        InteractiveSession session = getByRoomCode(roomCode);
        return interactiveSessionResultRepository.findByInteractiveSessionId(session.getId());
    }

    // ---- Review ----

    public InteractiveSessionReviewDTO buildReview(String roomCode) {
        InteractiveSession session = getByRoomCode(roomCode);
        if (session.getStatus() != GameStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is not yet finished");

        InteractiveSessionResult result = interactiveSessionResultRepository.findByInteractiveSessionId(session.getId()).orElse(null);
        List<PlayerPlacement> placements = result != null ? result.getPlacements() : List.of();

        List<InteractiveSessionReviewDTO.RoundReview> rounds = new ArrayList<>();
        List<DeckElement> snap = session.getDeckSnapshot();
        for (int i = 0; i < snap.size(); i++) {
            DeckElement element = snap.get(i);
            List<InteractiveSessionReviewDTO.PlayerRoundDetail> details = new ArrayList<>();
            int timedOut = 0;
            for (InteractiveSessionPlayer player : session.getPlayers()) {
                PlayerAnswer ans = player.getAnswers().stream()
                        .filter(a -> a.getElementId().equals(element.id()))
                        .findFirst().orElse(null);
                if (ans == null)
                    continue;
                details.add(new InteractiveSessionReviewDTO.PlayerRoundDetail(
                        player.getUserId(), player.getUserName(),
                        ans.getPayload(), ans.isCorrect(), ans.getPointsAwarded()));
                if (ans.getPayload() instanceof TimeoutAnswer)
                    timedOut++;
            }
            rounds.add(new InteractiveSessionReviewDTO.RoundReview(i, element, timedOut, details));
        }

        return new InteractiveSessionReviewDTO(
                session.getId(),
                session.getRoomCode(),
                session.getEndedAt(),
                session.getSettings().isScoringEnabled(),
                placements,
                rounds);
    }

    // ---- State machine (called by InteractiveSessionWebSocketController) ----

    public void startGame(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = getByRoomCode(roomCode);
            validateHost(session, principalName);

            if (session.getStatus() != GameStatus.LOBBY)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession has already started");

            // Chunk 13 — when the host opts in to shuffleQuestions, permute
            // the frozen snapshot once at game start so the elementId order is
            // stable for the rest of the show. Seed is the interactiveSession id so a
            // backend restart can re-derive the same order (deckSnapshot is
            // persisted, so this is more of a safety net than a hot path).
            if (session.getSettings().isShuffleQuestions()) {
                List<DeckElement> shuffled = new ArrayList<>(session.getDeckSnapshot());
                java.util.Collections.shuffle(shuffled,
                        new java.util.Random(session.getId().hashCode()));
                session.setDeckSnapshot(shuffled);
            }

            session.setStatus(GameStatus.IN_PROGRESS);
            session.setPhase(InteractiveSessionPhase.SUBMIT);
            session.setStartedAt(LocalDateTime.now());
            session.setCurrentRound(0);
            session.setRoundStartedAt(LocalDateTime.now());
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);

            DeckElement first = session.getDeckSnapshot().get(0);
            broadcastRoundStart(session, first);
            scheduleElementTimer(session, 0, first);
        }
    }

    public void submitAnswer(String roomCode, AnswerSubmitRequest request, String principalName) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = loadActiveSession(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getPhase() != InteractiveSessionPhase.SUBMIT)
                return;

            DeckElement current = session.getDeckSnapshot().get(session.getCurrentRound());
            if (!current.id().equals(request.elementId()))
                return; // stale answer
            if (current instanceof Slide)
                return; // slides accept no answers

            String userId = resolveUserId(principalName);
            InteractiveSessionPlayer player = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
            if (player == null)
                return;

            boolean alreadyAnswered = player.getAnswers().stream()
                    .anyMatch(a -> a.getElementId().equals(current.id()));
            if (alreadyAnswered)
                return;

            // Word Cloud submissions are normalized server-side so the value
            // stored on PlayerAnswer.payload matches what the aggregator counted
            // (case-folded, trimmed, banned-word filtered, capped at the
            // question's per-player limit). Submissions whose words all get
            // stripped become a no-op rather than a stored empty answer — the
            // player can submit again.
            AnswerPayload payload = request.payload();
            if (current instanceof WordCloudQuestion wc && payload instanceof WordCloudAnswer wca) {
                List<String> sanitized = WordCloudAggregator.normalize(wc, wca.words());
                if (sanitized.isEmpty())
                    return;
                payload = new WordCloudAnswer(sanitized);
            }
            // DRAWING: silently drop submissions that bust the per-question
            // stroke/point caps or the room-wide byte cap. Failing rather than
            // truncating keeps the on-wire shape stable and pushes the player
            // back into the editor — clients downsample before submitting.
            if (current instanceof DrawingQuestion dq && payload instanceof DrawingAnswer da) {
                if (!isDrawingAnswerWithinLimits(dq, da))
                    return;
            }

            ElementScorer.Result result = ElementScorer.score(current, payload);
            int basePoints = result.points();
            int speedBonus = result.correct() ? computeSpeedBonus(basePoints, session) : 0;
            int points = basePoints + speedBonus;

            // Chunk 13 — capture timing (always, regardless of speedBonus
            // setting; chunks 15/16 need it) and the player's streak as it
            // was BEFORE this answer is applied, so reveal can render
            // "5x streak!" without the client walking the history.
            LocalDateTime now = LocalDateTime.now();
            long timeTakenMs = session.getRoundStartedAt() == null
                    ? 0L
                    : Math.max(0L, Duration.between(session.getRoundStartedAt(), now).toMillis());

            PlayerAnswer answer = new PlayerAnswer();
            answer.setElementId(current.id());
            answer.setPayload(payload);
            answer.setCorrect(result.correct());
            answer.setPointsAwarded(points);
            answer.setSpeedBonusAwarded(speedBonus);
            answer.setTimeTakenMs(timeTakenMs);
            answer.setStreakBeforeAnswer(player.getCurrentStreak());
            answer.setAnsweredAt(now);
            // For Best Answer rounds: assign a server-side submissionId so the
            // VOTE-phase broadcast can reference this submission anonymously.
            if (current.bestAnswerMode()) {
                answer.setSubmissionId(UUID.randomUUID().toString());
            }

            player.getAnswers().add(answer);
            player.setScore(player.getScore() + points);
            player.setSpeedBonusTotal(player.getSpeedBonusTotal() + speedBonus);

            // Streak + accuracy. Slides and survey questions don't count
            // toward accuracy / streaks — we use ElementScorer.Result's
            // "correct" flag, which is false on un-scored kinds, but those
            // don't reach here (Slides are short-circuited above; surveys
            // return correct=false but the streak intent is "wrong answers
            // break streaks", which is the right semantic).
            if (result.correct()) {
                player.setCurrentStreak(player.getCurrentStreak() + 1);
                if (player.getCurrentStreak() > player.getLongestStreak()) {
                    player.setLongestStreak(player.getCurrentStreak());
                }
            } else {
                player.setCurrentStreak(0);
            }
            int answered = player.getAnswers().size();
            int correctSoFar = (int) player.getAnswers().stream().filter(PlayerAnswer::isCorrect).count();
            player.setAccuracy(answered == 0 ? 0.0 : (double) correctSoFar / answered);

            if (session.isTeamMode() && points != 0) {
                recomputeTeamScore(session, player.getTeamId());
                broadcastTeamUpdate(session);
            }
            interactiveSessionCache.put(session);

            broadcastAnswerProgress(session, current.id());
            if (current instanceof WordCloudQuestion wc) {
                broadcastWordCloud(session, wc);
            }

            boolean allAnswered = session.getPlayers().stream()
                    .allMatch(p -> p.getAnswers().stream()
                            .anyMatch(a -> a.getElementId().equals(current.id())));
            if (allAnswered)
                completeRound(session);
        }
    }

    public void bootPlayer(String roomCode, String hostPrincipalName, String targetUserId) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = loadActiveSession(roomCode);
            validateHost(session, hostPrincipalName);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is already over");
            if (session.getHostUserId().equals(targetUserId))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host cannot boot themselves");

            InteractiveSessionPlayer victim = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(targetUserId))
                    .findFirst().orElse(null);
            if (victim == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in this interactiveSession");
            session.getPlayers().remove(victim);
            removePlayerFromTeam(session, victim);

            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            if (session.isTeamMode()) {
                broadcastTeamUpdate(session);
            }
        }
    }

    public void endInteractiveSessionEarly(String roomCode, String hostPrincipalName) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = loadActiveSession(roomCode);
            validateHost(session, hostPrincipalName);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is not in progress");
            endGame(session);
        }
    }

    public void nextRound(String roomCode, String principalName) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = getByRoomCode(roomCode);
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
            InteractiveSession session = loadActiveSession(roomCode);
            if (session.getStatus() == GameStatus.FINISHED || session.getStatus() == GameStatus.CANCELLED)
                return;

            String userId = resolveUserId(principalName);
            InteractiveSessionPlayer leaver = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
            if (leaver == null)
                return;
            session.getPlayers().remove(leaver);
            removePlayerFromTeam(session, leaver);

            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            if (session.isTeamMode()) {
                broadcastTeamUpdate(session);
            }
        }
    }

    // ---- Round transitions ----

    private void handleRoundTimeout(String roomCode, int timedRound) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = interactiveSessionCache.get(roomCode)
                    .orElseGet(() -> interactiveSessionRepository.findByRoomCode(roomCode).orElse(null));
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

    private void advanceToNextRound(InteractiveSession session) {
        boolean isLast = session.getCurrentRound() >= session.getDeckSnapshot().size() - 1;
        if (isLast) {
            interactiveSessionRepository.save(session);
            endGame(session);
            return;
        }
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setRoundStartedAt(null);
        interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);
        startNextRound(session.getRoomCode());
    }

    private void completeRound(InteractiveSession session) {
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        stampTimeoutAnswers(session, element);

        // Best Answer mode hijacks the normal complete-round flow: instead of
        // revealing immediately we transition to VOTE phase and broadcast the
        // anonymized submissions. The reveal happens in completeVotePhase.
        if (element.bestAnswerMode() && hasVoteEligibleSubmission(session, element)) {
            startVotePhase(session, element);
            return;
        }

        // Word Cloud: send the locked-in cloud one more time so any client that
        // joined mid-round and missed an interim broadcast still renders the
        // final tally on reveal.
        if (element instanceof WordCloudQuestion wc) {
            broadcastWordCloud(session, wc);
        }

        broadcastRoundResult(session, element, null);
        advanceRound(session);
    }

    /** Drops a TimeoutAnswer onto any player who didn't submit for this element. */
    private void stampTimeoutAnswers(InteractiveSession session, DeckElement element) {
        for (InteractiveSessionPlayer player : session.getPlayers()) {
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
    private static boolean hasVoteEligibleSubmission(InteractiveSession session, DeckElement element) {
        return session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .anyMatch(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null);
    }

    /**
     * Broadcast the standard round-result message (best-answer outcome may be
     * null).
     */
    private void broadcastRoundResult(
            InteractiveSession session,
            DeckElement element,
            RoundResultMessage.BestAnswerOutcome bestAnswer) {
        // Surveys (Word Cloud, Allocation) can expose players via their open
        // answers — reveal shows aggregated counts/averages, so null each
        // player's payload in the per-player results to avoid leakage.
        boolean redactPayload = element instanceof WordCloudQuestion
                || element instanceof AllocationQuestion;

        List<RoundResultMessage.PlayerRoundResult> results = session.getPlayers().stream()
                .map(player -> {
                    PlayerAnswer ans = player.getAnswers().stream()
                            .filter(a -> a.getElementId().equals(element.id()))
                            .findFirst().orElseThrow();
                    return new RoundResultMessage.PlayerRoundResult(
                            player.getUserId(), player.getUserName(),
                            redactPayload ? null : ans.getPayload(),
                            ans.isCorrect(), ans.getPointsAwarded(),
                            player.getScore());
                })
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/roundResult",
                new RoundResultMessage(session.getCurrentRound(), element, results, bestAnswer));
    }

    /** Advance to the next round (or end the interactiveSession if this was the last). */
    private void advanceRound(InteractiveSession session) {
        boolean isLast = session.getCurrentRound() >= session.getDeckSnapshot().size() - 1;
        if (isLast) {
            interactiveSessionRepository.save(session);
            endGame(session);
            return;
        }
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setRoundStartedAt(null);
        session.setPhase(InteractiveSessionPhase.SUBMIT);
        interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);

        if (session.getSettings().getGameMode() == GameMode.SIMULTANEOUS) {
            String roomCode = session.getRoomCode();
            scheduler.schedule(() -> {
                try {
                    startNextRound(roomCode);
                } catch (Exception ignored) {
                }
            }, BETWEEN_ROUNDS_DELAY_SECONDS, TimeUnit.SECONDS);
        } else if (session.getSettings().getGameMode() == GameMode.TURN_BASED
                && session.getSettings().isAutoAdvance()) {
            // Chunk 13 — when the host turns on autoAdvance for a TURN_BASED
            // session, advance from the reveal to the next round on a timer
            // (podiumDuration seconds) instead of waiting for the host's
            // "Next" click. Host can still call nextRound() to short-circuit
            // the wait; startNextRound is idempotent against currentRound so
            // a double-fire here is a no-op.
            String roomCode = session.getRoomCode();
            int delay = Math.max(0, session.getSettings().getPodiumDuration());
            scheduler.schedule(() -> {
                try {
                    startNextRound(roomCode);
                } catch (Exception ignored) {
                }
            }, delay, TimeUnit.SECONDS);
        }
    }

    // ---- Best Answer phase machine ----

    /**
     * Transition from SUBMIT to VOTE phase: collect vote-eligible submissions,
     * broadcast them anonymously, and schedule the vote-phase timer.
     */
    private void startVotePhase(InteractiveSession session, DeckElement element) {
        session.setPhase(InteractiveSessionPhase.VOTE);
        session.setRoundStartedAt(LocalDateTime.now());

        List<VotePhaseStartMessage.AnonymizedSubmission> submissions = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null)
                .map(a -> new VotePhaseStartMessage.AnonymizedSubmission(a.getSubmissionId(), a.getPayload()))
                .toList();

        int timePerVote = effectiveDisplaySeconds(element, session.getSettings());

        interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);

        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/votePhase",
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

    /** Client → server vote handler (called from InteractiveSessionWebSocketController). */
    public void submitVote(String roomCode, VoteSubmitRequest request, String principalName) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = loadActiveSession(roomCode);
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getPhase() != InteractiveSessionPhase.VOTE)
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
            InteractiveSessionPlayer voter = session.getPlayers().stream()
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
            interactiveSessionCache.put(session);

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
    private void completeVotePhase(InteractiveSession session) {
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        int bonus = Math.max(0, element.bestAnswerBonus());

        // Tally votes per submissionId. Submissions with zero votes still
        // appear in the tally so the REVEAL UI can show "no one voted for X".
        Map<String, Integer> voteCounts = new HashMap<>();
        session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> element.id().equals(a.getElementId()) && a.getSubmissionId() != null)
                .forEach(a -> voteCounts.put(a.getSubmissionId(), 0));

        for (InteractiveSessionPlayer p : session.getPlayers()) {
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
        for (InteractiveSessionPlayer p : session.getPlayers()) {
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

        if (session.isTeamMode() && bonus > 0 && !winnerUserIds.isEmpty()) {
            session.getTeams().forEach(t -> recomputeTeamScore(session, t.getId()));
            broadcastTeamUpdate(session);
        }

        broadcastRoundResult(session, element,
                new RoundResultMessage.BestAnswerOutcome(tallies, winnerUserIds, bonus));
        advanceRound(session);
    }

    private void broadcastVoteProgress(InteractiveSession session, String elementId) {
        List<String> votedUserIds = session.getPlayers().stream()
                .filter(p -> p.getVotes().stream().anyMatch(v -> elementId.equals(v.getElementId())))
                .map(InteractiveSessionPlayer::getUserId)
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/voted",
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
            InteractiveSession session = interactiveSessionCache.get(roomCode)
                    .orElseGet(() -> interactiveSessionRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null)
                return;
            if (session.getStatus() != GameStatus.IN_PROGRESS)
                return;
            if (session.getCurrentRound() != timedRound)
                return;
            if (session.getPhase() != InteractiveSessionPhase.VOTE)
                return;
            completeVotePhase(session);
        }
    }

    private void startNextRound(String roomCode) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = interactiveSessionCache.get(roomCode)
                    .orElseGet(() -> interactiveSessionRepository.findByRoomCode(roomCode).orElse(null));
            if (session == null || session.getStatus() != GameStatus.IN_PROGRESS)
                return;

            session.setRoundStartedAt(LocalDateTime.now());
            session.setPhase(InteractiveSessionPhase.SUBMIT);
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);

            DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
            broadcastRoundStart(session, element);
            scheduleElementTimer(session, session.getCurrentRound(), element);
        }
    }

    private void endGame(InteractiveSession session) {
        session.setStatus(GameStatus.FINISHED);
        session.setEndedAt(LocalDateTime.now());

        List<InteractiveSessionPlayer> ranked = session.getPlayers().stream()
                .sorted((a, b) -> b.getScore() - a.getScore())
                .toList();

        List<PlayerPlacement> placements = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            InteractiveSessionPlayer sp = ranked.get(i);
            PlayerPlacement p = new PlayerPlacement();
            p.setUserId(sp.getUserId());
            p.setUserName(sp.getUserName());
            p.setGuest(sp.isGuest());
            p.setFinalScore(sp.getScore());
            p.setPlacement(i + 1);
            p.setCorrectAnswers((int) sp.getAnswers().stream().filter(PlayerAnswer::isCorrect).count());
            p.setTotalQuestions(session.getDeckSnapshot().size());
            p.setTeamId(sp.getTeamId());
            // Chunk 13 — snapshot the per-player engagement / accuracy stats
            // so the placement card can render without re-loading the
            // (potentially evicted) InteractiveSession document.
            p.setLongestStreak(sp.getLongestStreak());
            p.setAccuracy(sp.getAccuracy());
            p.setReactionsSent(sp.getReactionsSent());
            placements.add(p);
        }

        InteractiveSessionResult result = new InteractiveSessionResult();
        result.setInteractiveSessionId(session.getId());
        result.setPlacements(placements);
        interactiveSessionResultRepository.save(result);
        interactiveSessionRepository.save(session);
        interactiveSessionCache.evict(session.getRoomCode());

        // Bump the deck's denormalized play counter atomically so Explore's
        // "most played" / "trending" sorts reflect this finish without a
        // cross-collection aggregation.
        deckService.incrementPlayCount(session.getDeckId());

        for (PlayerPlacement p : placements) {
            if (!p.isGuest())
                updateStatsAfterGame(p.getUserId(), p.getFinalScore(), p.getPlacement() == 1);
        }

        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/gameOver",
                new InteractiveSessionEndedMessage(placements));
    }

    // ---- Scoring + broadcast helpers ----

    /**
     * Computes the speed bonus portion only (returns 0 when speed bonus is
     * off, not applicable, or the elapsed time matches the window exactly).
     * Chunk 13 stores this on PlayerAnswer.speedBonusAwarded so analytics can
     * separate base points from timing bonus; callers add it to base points
     * themselves.
     */
    private int computeSpeedBonus(int basePoints, InteractiveSession session) {
        InteractiveSessionSettings s = session.getSettings();
        if (!s.isSpeedBonus() || s.getGameMode() != GameMode.SIMULTANEOUS || session.getRoundStartedAt() == null) {
            return 0;
        }
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        int timeWindow = effectiveDisplaySeconds(element, s);
        if (timeWindow <= 0)
            return 0;
        long totalMillis = timeWindow * 1000L;
        long elapsed = Duration.between(session.getRoundStartedAt(), LocalDateTime.now()).toMillis();
        elapsed = Math.min(Math.max(elapsed, 0), totalMillis);
        double speedFraction = 1.0 - ((double) elapsed / totalMillis);
        return Math.max(0, (int) (basePoints * 0.5 * speedFraction));
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

    private void broadcastRoundStart(InteractiveSession session, DeckElement element) {
        DeckElement redacted = ElementRedactor.redact(element);
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/round",
                new RoundStartMessage(
                        session.getCurrentRound(),
                        session.getDeckSnapshot().size(),
                        redacted,
                        session.getRoundStartedAt()));
        // Per-player overlay: when the element opts into shuffle, each player
        // also receives a personalized round payload with the options/items
        // permuted on (roomCode, elementId, userId). The seed is stable so a
        // reconnecting player sees the same arrangement and can resume mid-
        // round without the server tracking per-player order. Chunk 13 layers
        // InteractiveSessionSettings.shuffleAnswers on top — the per-element flag from
        // chunk 10 only fires when the host hasn't disabled shuffling at the
        // session level.
        if (session.getSettings().isShuffleAnswers() && ElementShuffler.shouldShuffle(redacted)) {
            String roomCode = session.getRoomCode();
            for (InteractiveSessionPlayer player : session.getPlayers()) {
                String principal = player.getPrincipalName();
                if (principal == null)
                    continue;
                DeckElement personalized =
                        ElementShuffler.shuffleForPlayer(redacted, roomCode, player.getUserId());
                messagingTemplate.convertAndSendToUser(
                        principal,
                        "/queue/interactiveSession/" + roomCode + "/round",
                        new RoundStartMessage(
                                session.getCurrentRound(),
                                session.getDeckSnapshot().size(),
                                personalized,
                                session.getRoundStartedAt()));
            }
        }
    }

    /**
     * Aggregate every player's Word Cloud submission for {@code question} and
     * broadcast the resulting word -> count map. Called both during SUBMIT
     * (so the cloud animates live as words come in) and one final time on
     * round complete (the locked-in cloud). The frontend only ever sees
     * counts — per-player word lists are never sent on this topic.
     */
    /**
     * Validate a DRAWING submission against the question's caps and the
     * room-wide serialized-byte cap. Returns false (drop the submission) when
     * the stroke list is too long, any stroke has too many points, or the
     * serialized JSON would exceed {@link #drawingMaxPayloadBytes}.
     *
     * Counting points as pairs: {@link Stroke#points()} is flat
     * {@code [x0, y0, x1, y1, ...]}, so the count of (x, y) pairs is
     * {@code points.size() / 2}.
     */
    private boolean isDrawingAnswerWithinLimits(DrawingQuestion question, DrawingAnswer answer) {
        List<Stroke> strokes = answer.strokes();
        if (strokes == null) return true;
        if (question.maxStrokesPerPlayer() > 0 && strokes.size() > question.maxStrokesPerPlayer())
            return false;
        if (question.maxPointsPerStroke() > 0) {
            for (Stroke stroke : strokes) {
                List<Double> pts = stroke.points();
                if (pts != null && (pts.size() / 2) > question.maxPointsPerStroke())
                    return false;
            }
        }
        try {
            return objectMapper.writeValueAsBytes(answer).length <= drawingMaxPayloadBytes;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private void broadcastWordCloud(InteractiveSession session, WordCloudQuestion question) {
        List<WordCloudAnswer> submissions = session.getPlayers().stream()
                .flatMap(p -> p.getAnswers().stream())
                .filter(a -> question.id().equals(a.getElementId()))
                .map(PlayerAnswer::getPayload)
                .filter(WordCloudAnswer.class::isInstance)
                .map(WordCloudAnswer.class::cast)
                .toList();
        Map<String, Integer> counts = WordCloudAggregator.aggregate(question, submissions);
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/wordCloud",
                new WordCloudUpdateMessage(session.getCurrentRound(), question.id(), counts));
    }

    private void broadcastAnswerProgress(InteractiveSession session, String elementId) {
        List<String> answeredUserIds = session.getPlayers().stream()
                .filter(p -> p.getAnswers().stream().anyMatch(a -> a.getElementId().equals(elementId)))
                .map(InteractiveSessionPlayer::getUserId)
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/answered",
                new AnswerProgressMessage(
                        session.getCurrentRound(), answeredUserIds, session.getPlayers().size()));
    }

    private void scheduleElementTimer(InteractiveSession session, int round, DeckElement element) {
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
    private static int effectiveDisplaySeconds(DeckElement element, InteractiveSessionSettings settings) {
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

    private void validateHost(InteractiveSession session, String principalName) {
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

    private InteractiveSession loadActiveSession(String roomCode) {
        return interactiveSessionCache.get(roomCode).orElseGet(() -> getByRoomCode(roomCode));
    }

    private Object getLock(String roomCode) {
        return roundLocks.computeIfAbsent(roomCode, k -> new Object());
    }

    private InteractiveSessionPlayer playerFromUser(User user) {
        InteractiveSessionPlayer p = new InteractiveSessionPlayer();
        p.setUserId(user.getId());
        p.setUserName(user.getUserName());
        p.setPictureUrl(userImageHydrator.pictureUrlOf(user));
        p.setGuest(Boolean.TRUE.equals(user.getIsGuest()));
        p.setPrincipalName(principalNameFor(user));
        return p;
    }

    /**
     * Mirrors the inverse of {@link #resolveUserId(String)}: maps a User back to
     * the Spring Security principal name used as the STOMP routing key.
     * Guests use "guest:<userId>"; OAuth users use the googleId. Returns null
     * for an OAuth user with no googleId on record (defensive — the service
     * skips per-user broadcasts when this is null).
     */
    private static String principalNameFor(User user) {
        if (Boolean.TRUE.equals(user.getIsGuest()))
            return "guest:" + user.getId();
        return user.getGoogleId();
    }

    /**
     * Defensive copy so the deck's `defaultSettings` isn't mutated by a interactiveSession.
     */
    private static InteractiveSessionSettings copyOf(InteractiveSessionSettings src) {
        InteractiveSessionSettings out = new InteractiveSessionSettings();
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
        out.setReactionsEnabled(src.isReactionsEnabled());
        out.setChatEnabled(src.isChatEnabled());
        out.setTeamMode(src.isTeamMode());
        out.setTeamCount(src.getTeamCount());
        out.setAutoBalanceTeams(src.isAutoBalanceTeams());
        out.setShuffleQuestions(src.isShuffleQuestions());
        out.setShuffleAnswers(src.isShuffleAnswers());
        out.setAutoAdvance(src.isAutoAdvance());
        out.setPodiumDuration(src.getPodiumDuration());
        out.setLobbyCountdownSeconds(src.getLobbyCountdownSeconds());
        out.setLobbyMusicAssetId(src.getLobbyMusicAssetId());
        out.setRequireFullName(src.isRequireFullName());
        out.setSpectatorsAllowed(src.isSpectatorsAllowed());
        return out;
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (interactiveSessionRepository.findByRoomCode(code).isEmpty())
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
        if (image == null) return null;
        var largest = image.largestVariant();
        return largest == null ? null : largest.url();
    }

    // ---- Audience engagement (chunk 11) ----

    /**
     * Accept a reaction from a participant. Validates the interactiveSession + per-slide
     * gating flags, the emoji allow-list, and the per-player rate limit, then
     * persists the reaction, bumps the Redis hash counter, and broadcasts the
     * burst on {@code /topic/interactive-session/{roomCode}/reaction}.
     *
     * Returns the persisted reaction for the REST fallback path (the STOMP
     * caller doesn't need it). Reactions are accepted in any phase of an
     * in-progress interactiveSession — players react to the round result reveal as much
     * as they do to the submission phase.
     */
    public Reaction acceptReaction(String roomCode, ReactionSendRequest request, String principalName) {
        InteractiveSession session = loadActiveSession(roomCode);
        if (session.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is not in progress");
        }
        if (!session.getSettings().isReactionsEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reactions are disabled for this interactiveSession");
        }
        DeckElement current = session.getDeckSnapshot().get(session.getCurrentRound());
        if (!current.reactionsEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reactions are disabled for this slide");
        }
        if (!EmojiAllowList.isAllowed(request.emoji())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji not allowed");
        }

        String userId = resolveUserId(principalName);
        InteractiveSessionPlayer player = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));

        if (!rateLimiter.allow(InteractiveSessionRateLimiter.Kind.REACTION, roomCode, userId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Reaction rate limit exceeded");
        }

        long offsetMs = session.getRoundStartedAt() == null
                ? 0L
                : Math.max(0L, Duration.between(session.getRoundStartedAt(), LocalDateTime.now()).toMillis());

        Reaction reaction = new Reaction();
        reaction.setId(UUID.randomUUID().toString());
        reaction.setInteractiveSessionId(session.getId());
        reaction.setElementId(current.id());
        reaction.setUserId(userId);
        reaction.setUserName(player.getUserName());
        reaction.setGuest(player.isGuest());
        reaction.setEmoji(request.emoji());
        reaction.setOffsetMs(offsetMs);
        reaction.setSentAt(LocalDateTime.now());
        reactionRepository.save(reaction);

        // Engagement counter so post-game placement cards can show "23 reactions
        // sent" without re-aggregating the reaction collection.
        player.setReactionsSent(player.getReactionsSent() + 1);
        interactiveSessionRepository.save(session);
        interactiveSessionCache.put(session);

        interactiveSessionCache.incrementReactionCount(roomCode, current.id(), request.emoji());

        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + roomCode + "/reaction",
                new ReactionBroadcastMessage(
                        reaction.getId(),
                        reaction.getElementId(),
                        reaction.getUserId(),
                        reaction.getUserName(),
                        reaction.isGuest(),
                        reaction.getEmoji(),
                        reaction.getOffsetMs(),
                        reaction.getSentAt()));

        return reaction;
    }

    /**
     * Accept a chat message. Validates the interactiveSession setting, the trimmed body
     * length, and the per-player rate limit, then persists the row and
     * broadcasts it on {@code /topic/interactive-session/{roomCode}/chat}. The author's
     * {@code fromHost} flag is computed server-side so clients can't spoof a
     * host badge.
     */
    public InteractiveSessionChatMessageDTO acceptChat(String roomCode, ChatSendRequest request, String principalName) {
        InteractiveSession session = loadActiveSession(roomCode);
        // Chat is allowed before start and after finish too — lobbies and review
        // pages benefit from it — but a cancelled session is dead.
        if (session.getStatus() == GameStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession has been cancelled");
        }
        if (!session.getSettings().isChatEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chat is disabled for this interactiveSession");
        }

        String trimmed = request.body() == null ? "" : request.body().trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body is required");
        }
        if (trimmed.length() > CHAT_MAX_BODY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message body must be at most " + CHAT_MAX_BODY + " characters");
        }

        String userId = resolveUserId(principalName);
        InteractiveSessionPlayer player = session.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));

        if (!rateLimiter.allow(InteractiveSessionRateLimiter.Kind.CHAT, roomCode, userId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Chat rate limit exceeded");
        }

        InteractiveSessionChatMessage row = new InteractiveSessionChatMessage();
        row.setId(UUID.randomUUID().toString());
        row.setInteractiveSessionId(session.getId());
        row.setAuthorUserId(userId);
        row.setAuthorName(player.getUserName());
        row.setAuthorPictureUrl(player.getPictureUrl());
        row.setFromHost(session.getHostUserId().equals(userId));
        row.setGuest(player.isGuest());
        row.setBody(trimmed);
        row.setSentAt(LocalDateTime.now());
        chatRepository.save(row);

        // Host moderation hasn't happened yet → broadcast the unredacted body.
        InteractiveSessionChatMessageDTO broadcast = InteractiveSessionChatMessageDTO.redactedFor(row, true);
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + roomCode + "/chat",
                broadcast);
        return broadcast;
    }

    /**
     * Host hides a chat message. Idempotent: re-flipping is a no-op. The body
     * stays in storage so the audit trail is preserved; the moderation
     * broadcast tells subscribed clients to re-render the row as
     * {@code "(hidden by host)"}.
     */
    public InteractiveSessionChatMessageDTO moderateChatMessage(String roomCode, String messageId, String principalName) {
        InteractiveSession session = loadActiveSession(roomCode);
        validateHost(session, principalName);

        InteractiveSessionChatMessage row = chatRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat message not found"));
        if (!session.getId().equals(row.getInteractiveSessionId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat message not found");
        }
        if (!row.isModerated()) {
            row.setModerated(true);
            row.setModeratedByUserId(resolveUserId(principalName));
            row.setModeratedAt(LocalDateTime.now());
            chatRepository.save(row);
        }
        // Broadcast the redacted form so non-host clients flip to the placeholder.
        InteractiveSessionChatMessageDTO redacted = InteractiveSessionChatMessageDTO.redactedFor(row, false);
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + roomCode + "/chat",
                redacted);
        return redacted;
    }

    /**
     * Paginated chat history for late joiners or post-game review. Non-host
     * callers see moderated rows as placeholders; the host caller sees the
     * original body so they can decide whether to keep it hidden.
     */
    public List<InteractiveSessionChatMessageDTO> listChatHistory(String roomCode, int page, int size, String principalName) {
        InteractiveSession session = getByRoomCode(roomCode);
        int safeSize = Math.max(1, Math.min(size, CHAT_PAGE_MAX_SIZE));
        int safePage = Math.max(0, page);

        boolean isHost = false;
        if (principalName != null) {
            try {
                isHost = session.getHostUserId().equals(resolveUserId(principalName));
            } catch (ResponseStatusException ignored) {
                // Unauthenticated readers see the non-host projection.
            }
        }

        Page<InteractiveSessionChatMessage> rows = chatRepository.findAllByInteractiveSessionIdOrderBySentAtDesc(
                session.getId(), PageRequest.of(safePage, safeSize));

        boolean hostView = isHost;
        return rows.getContent().stream()
                .map(row -> InteractiveSessionChatMessageDTO.redactedFor(row, hostView))
                .toList();
    }

    // ---- Teams (chunk 12) ----

    /**
     * Decrement the team's memberCount when a player leaves or is booted.
     * If the captain leaves, hand the slot to another team member (or clear
     * it when the team is now empty).
     */
    private static void removePlayerFromTeam(InteractiveSession session, InteractiveSessionPlayer player) {
        String teamId = player.getTeamId();
        if (teamId == null)
            return;
        Team team = findTeam(session, teamId);
        if (team == null)
            return;
        team.setMemberCount(Math.max(0, team.getMemberCount() - 1));
        if (player.getUserId().equals(team.getCaptainUserId())) {
            String nextCaptainId = session.getPlayers().stream()
                    .filter(p -> teamId.equals(p.getTeamId()) && !player.getUserId().equals(p.getUserId()))
                    .map(InteractiveSessionPlayer::getUserId)
                    .findFirst().orElse(null);
            team.setCaptainUserId(nextCaptainId);
        }
        // Recompute score: subtracting one player's score is simpler than full
        // recompute but the latter is robust to drift, so prefer it.
        recomputeTeamScore(session, teamId);
    }

    /** Sum the scores of every player currently assigned to the team. */
    private static void recomputeTeamScore(InteractiveSession session, String teamId) {
        if (teamId == null)
            return;
        Team team = findTeam(session, teamId);
        if (team == null)
            return;
        int total = session.getPlayers().stream()
                .filter(p -> teamId.equals(p.getTeamId()))
                .mapToInt(InteractiveSessionPlayer::getScore)
                .sum();
        team.setScore(total);
    }

    private static Team findTeam(InteractiveSession session, String teamId) {
        if (session.getTeams() == null)
            return null;
        return session.getTeams().stream()
                .filter(t -> teamId.equals(t.getId()))
                .findFirst().orElse(null);
    }

    private void broadcastTeamUpdate(InteractiveSession session) {
        List<TeamUpdateMessage.TeamMembership> memberships = session.getPlayers().stream()
                .filter(p -> p.getTeamId() != null)
                .map(p -> new TeamUpdateMessage.TeamMembership(p.getUserId(), p.getTeamId()))
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/interactive-session/" + session.getRoomCode() + "/teams",
                new TeamUpdateMessage(session.getTeams(), memberships));
    }

    /** Host adds a custom team. */
    public InteractiveSession createTeam(String roomCode, String name, String color, User host) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = authorizationService.requireInteractiveSessionHost(roomCode, host);
            requireTeamMode(session);
            requireLobby(session);
            if (session.getTeams().size() >= DEFAULT_TEAM_NAMES.size())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Maximum team count reached");
            Team team = new Team();
            team.setId(UUID.randomUUID().toString());
            team.setName(blankToDefault(name, "Team " + (session.getTeams().size() + 1)));
            team.setColor(blankToDefault(color, "blue"));
            session.getTeams().add(team);
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            broadcastTeamUpdate(session);
            return session;
        }
    }

    /** Host renames or recolors a team. */
    public InteractiveSession updateTeam(String roomCode, String teamId, String name, String color, User host) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = authorizationService.requireInteractiveSessionHost(roomCode, host);
            requireTeamMode(session);
            Team team = findTeam(session, teamId);
            if (team == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
            if (name != null && !name.isBlank())
                team.setName(name.trim());
            if (color != null && !color.isBlank())
                team.setColor(color.trim());
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            broadcastTeamUpdate(session);
            return session;
        }
    }

    /**
     * Host deletes a team. Any players assigned to it are reassigned
     * round-robin into the remaining teams so no one is left without a team.
     */
    public InteractiveSession deleteTeam(String roomCode, String teamId, User host) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = authorizationService.requireInteractiveSessionHost(roomCode, host);
            requireTeamMode(session);
            requireLobby(session);
            if (session.getTeams().size() <= 2)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "At least two teams are required");
            Team team = findTeam(session, teamId);
            if (team == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");

            session.getTeams().removeIf(t -> teamId.equals(t.getId()));
            // Round-robin reassign every orphaned player.
            for (InteractiveSessionPlayer player : session.getPlayers()) {
                if (teamId.equals(player.getTeamId())) {
                    player.setTeamId(null);
                    assignPlayerToTeam(session, player, null);
                }
            }
            // Refresh every team's score after the redistribution.
            for (Team t : session.getTeams()) {
                recomputeTeamScore(session, t.getId());
            }
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            broadcastTeamUpdate(session);
            return session;
        }
    }

    /** Host moves a player into a different team. */
    public InteractiveSession movePlayerToTeam(String roomCode, String userId, String teamId, User host) {
        synchronized (getLock(roomCode)) {
            InteractiveSession session = authorizationService.requireInteractiveSessionHost(roomCode, host);
            requireTeamMode(session);
            InteractiveSessionPlayer player = session.getPlayers().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in this interactiveSession"));
            Team target = findTeam(session, teamId);
            if (target == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");

            String previousTeamId = player.getTeamId();
            if (teamId.equals(previousTeamId))
                return session;
            removePlayerFromTeam(session, player);
            player.setTeamId(null);
            assignPlayerToTeam(session, player, teamId);
            recomputeTeamScore(session, teamId);
            if (previousTeamId != null)
                recomputeTeamScore(session, previousTeamId);
            interactiveSessionRepository.save(session);
            interactiveSessionCache.put(session);
            messagingTemplate.convertAndSend("/topic/interactive-session/" + roomCode + "/lobby", new InteractiveSessionDTO(session));
            broadcastTeamUpdate(session);
            return session;
        }
    }

    private static void requireTeamMode(InteractiveSession session) {
        if (!session.isTeamMode())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "InteractiveSession is not in team mode");
    }

    private static void requireLobby(InteractiveSession session) {
        if (session.getStatus() != GameStatus.LOBBY)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teams can only be edited in the lobby");
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Read aggregated reaction counts for the current round; empty when no reactions yet. */
    public Map<String, Long> getReactionCountsForCurrentRound(String roomCode) {
        InteractiveSession session = loadActiveSession(roomCode);
        if (session.getCurrentRound() < 0
                || session.getCurrentRound() >= session.getDeckSnapshot().size()) {
            return Map.of();
        }
        DeckElement element = session.getDeckSnapshot().get(session.getCurrentRound());
        return interactiveSessionCache.getReactionCounts(roomCode, element.id());
    }
}
