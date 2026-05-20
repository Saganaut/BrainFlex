/**
 * Drives the lifecycle of a ScheduledInteractiveSession:
 *   schedule()  → SCHEDULED + initial invite emails
 *   addInvite() → adds one more invitee + sends the initial email
 *   cancel()    → SCHEDULED → CANCELLED (host-only) + cancel notices
 *   boot()      → SCHEDULED → LIVE; constructs a CreateInteractiveSessionRequest
 *                 from the stored settings, calls InteractiveSessionService, and
 *                 mails the boot reminder with the new room code.
 *   complete()  → LIVE → COMPLETED; invoked when the live session reaches FINISHED.
 *   redeem()    → marks an InteractiveSessionInvite redeemed and returns the
 *                 join target (roomCode if already booted, otherwise null).
 *
 * Tokens are base64url-encoded 24-byte randoms — long enough that lookup-by-token
 * is effectively unguessable. Expiry defaults to scheduledStartAt + 2h.
 *
 * Email sends are best-effort — {@link EmailService} swallows failures so a
 * down SMTP relay doesn't block the lifecycle or fail user requests.
 */
package cephadex.brainflex.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateInteractiveSessionRequest;
import cephadex.brainflex.dto.CreateScheduledInteractiveSessionRequest;
import cephadex.brainflex.dto.RedeemInviteResponse;
import cephadex.brainflex.dto.UpdateScheduledInteractiveSessionRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.InteractiveSessionInviteRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.ScheduledInteractiveSessionRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class ScheduledInteractiveSessionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledInteractiveSessionService.class);

    private static final int TOKEN_BYTES = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ScheduledInteractiveSessionRepository scheduleRepository;
    private final InteractiveSessionInviteRepository inviteRepository;
    private final InteractiveSessionRepository interactiveSessionRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final InteractiveSessionService interactiveSessionService;
    private final EmailService emailService;

    public ScheduledInteractiveSessionService(ScheduledInteractiveSessionRepository scheduleRepository,
                                              InteractiveSessionInviteRepository inviteRepository,
                                              InteractiveSessionRepository interactiveSessionRepository,
                                              DeckRepository deckRepository,
                                              UserRepository userRepository,
                                              InteractiveSessionService interactiveSessionService,
                                              EmailService emailService) {
        this.scheduleRepository = scheduleRepository;
        this.inviteRepository = inviteRepository;
        this.interactiveSessionRepository = interactiveSessionRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.interactiveSessionService = interactiveSessionService;
        this.emailService = emailService;
    }

    // ---- Read ----

    public ScheduledInteractiveSession getById(String id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    }

    public List<ScheduledInteractiveSession> listMine(String hostUserId) {
        return scheduleRepository.findByHostUserIdOrderByScheduledStartAtAsc(hostUserId);
    }

    public List<InteractiveSessionInvite> listInvites(String scheduledInteractiveSessionId) {
        return inviteRepository.findByScheduledInteractiveSessionId(scheduledInteractiveSessionId);
    }

    // ---- Write ----

    public ScheduledInteractiveSession schedule(User host, CreateScheduledInteractiveSessionRequest request) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        ScheduledInteractiveSession schedule = new ScheduledInteractiveSession();
        schedule.setHostUserId(host.getId());
        schedule.setDeckId(request.deckId());
        schedule.setScheduledStartAt(request.scheduledStartAt());
        schedule.setScheduledEndAt(request.scheduledEndAt());
        schedule.setReminderEmailTemplate(request.reminderEmailTemplate());
        schedule.setSettings(request.settings() != null
                ? request.settings()
                : copyOf(deck.getDefaultSettings()));
        schedule.setInvitedEmails(normaliseEmails(request.invitedEmails()));
        schedule = scheduleRepository.save(schedule);

        // Mint one invite row per email, mail the initial invite, and dedupe.
        for (String email : schedule.getInvitedEmails()) {
            createAndSendInvite(schedule, email, host, deck);
        }
        return schedule;
    }

    public ScheduledInteractiveSession update(String id, User host,
                                              UpdateScheduledInteractiveSessionRequest request) {
        ScheduledInteractiveSession schedule = requireOwnedAndSchedulable(id, host);
        if (request.scheduledStartAt() != null) schedule.setScheduledStartAt(request.scheduledStartAt());
        if (request.scheduledEndAt() != null) schedule.setScheduledEndAt(request.scheduledEndAt());
        if (request.reminderEmailTemplate() != null) schedule.setReminderEmailTemplate(request.reminderEmailTemplate());
        if (request.settings() != null) schedule.setSettings(request.settings());
        schedule.setUpdatedAt(LocalDateTime.now());
        return scheduleRepository.save(schedule);
    }

    public InteractiveSessionInvite addInvite(String id, User host, String email) {
        ScheduledInteractiveSession schedule = requireOwnedAndSchedulable(id, host);
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        if (inviteRepository.existsByEmailAndScheduledInteractiveSessionId(normalised, schedule.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already invited");
        }
        // Reflect on the schedule for snapshotting.
        if (!schedule.getInvitedEmails().contains(normalised)) {
            schedule.getInvitedEmails().add(normalised);
            schedule.setUpdatedAt(LocalDateTime.now());
            scheduleRepository.save(schedule);
        }
        Deck deck = deckRepository.findById(schedule.getDeckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        return createAndSendInvite(schedule, normalised, host, deck);
    }

    public ScheduledInteractiveSession cancel(String id, User host) {
        ScheduledInteractiveSession schedule = requireOwned(id, host);
        if (schedule.getStatus() != ScheduleStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only SCHEDULED rows can be cancelled");
        }
        schedule.setStatus(ScheduleStatus.CANCELLED);
        schedule.setUpdatedAt(LocalDateTime.now());
        ScheduledInteractiveSession saved = scheduleRepository.save(schedule);

        // Best-effort cancel notice.
        Deck deck = deckRepository.findById(schedule.getDeckId()).orElse(null);
        String hostName = host.getName() != null ? host.getName() : host.getUserName();
        String deckName = deck != null ? deck.getName() : "Session";
        List<InteractiveSessionInvite> invites = inviteRepository.findByScheduledInteractiveSessionId(schedule.getId());
        try {
            emailService.sendCancelNotice(saved, invites, hostName, deckName);
        } catch (Exception e) {
            log.warn("Cancel notice send failed for {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    /**
     * Boot a scheduled row into a live InteractiveSession. Called by the cron
     * sweep. Idempotent — returns the existing live session if already booted.
     */
    public InteractiveSession boot(String scheduleId) {
        ScheduledInteractiveSession schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        if (schedule.getStatus() == ScheduleStatus.LIVE && schedule.getCreatedInteractiveSessionId() != null) {
            return interactiveSessionRepository.findById(schedule.getCreatedInteractiveSessionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Schedule " + scheduleId + " claims a missing live session"));
        }
        if (schedule.getStatus() != ScheduleStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Schedule is not bootable (status=" + schedule.getStatus() + ")");
        }

        User host = userRepository.findById(schedule.getHostUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host user not found"));
        Deck deck = deckRepository.findById(schedule.getDeckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        InteractiveSession session = interactiveSessionService.createInteractiveSession(
                host, asCreateRequest(schedule));

        schedule.setCreatedInteractiveSessionId(session.getId());
        schedule.setStatus(ScheduleStatus.LIVE);
        schedule.setUpdatedAt(LocalDateTime.now());
        ScheduledInteractiveSession saved = scheduleRepository.save(schedule);

        // Link existing invites to the freshly-minted live session id so the
        // redeem endpoint can hand back roomCode without a second lookup.
        List<InteractiveSessionInvite> invites = inviteRepository.findByScheduledInteractiveSessionId(schedule.getId());
        String hostName = host.getName() != null ? host.getName() : host.getUserName();
        for (InteractiveSessionInvite invite : invites) {
            invite.setInteractiveSessionId(session.getId());
            inviteRepository.save(invite);
            try {
                emailService.sendBootReminder(saved, invite, hostName, deck.getName(), session.getRoomCode());
            } catch (Exception e) {
                log.warn("Boot reminder send failed for {}: {}", invite.getEmail(), e.getMessage());
            }
        }
        return session;
    }

    /** Called via {@link InteractiveSessionEndedListener} when a live session finishes. */
    public void markComplete(String interactiveSessionId) {
        scheduleRepository.findByCreatedInteractiveSessionId(interactiveSessionId)
                .ifPresent(s -> {
                    if (s.getStatus() == ScheduleStatus.LIVE) {
                        s.setStatus(ScheduleStatus.COMPLETED);
                        s.setUpdatedAt(LocalDateTime.now());
                        scheduleRepository.save(s);
                    }
                });
    }

    public RedeemInviteResponse redeem(String token, String resolvedUserId) {
        InteractiveSessionInvite invite = inviteRepository.findByInviteToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        LocalDateTime now = LocalDateTime.now();
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");
        }
        invite.setRedeemedAt(now);
        if (resolvedUserId != null) invite.setResolvedUserId(resolvedUserId);
        inviteRepository.save(invite);

        String roomCode = null;
        if (invite.getInteractiveSessionId() != null) {
            roomCode = interactiveSessionRepository.findById(invite.getInteractiveSessionId())
                    .map(InteractiveSession::getRoomCode)
                    .orElse(null);
        }
        String deckName = null;
        String hostName = null;
        LocalDateTime scheduledStartAt = null;
        if (invite.getScheduledInteractiveSessionId() != null) {
            Optional<ScheduledInteractiveSession> schedule =
                    scheduleRepository.findById(invite.getScheduledInteractiveSessionId());
            if (schedule.isPresent()) {
                ScheduledInteractiveSession s = schedule.get();
                scheduledStartAt = s.getScheduledStartAt();
                Deck deck = deckRepository.findById(s.getDeckId()).orElse(null);
                if (deck != null) deckName = deck.getName();
                User host = userRepository.findById(s.getHostUserId()).orElse(null);
                if (host != null) hostName = host.getName() != null ? host.getName() : host.getUserName();
            }
        }
        return new RedeemInviteResponse(
                invite.getScheduledInteractiveSessionId(),
                invite.getInteractiveSessionId(),
                roomCode,
                deckName,
                hostName,
                scheduledStartAt);
    }

    // ---- Helpers ----

    private ScheduledInteractiveSession requireOwned(String id, User host) {
        ScheduledInteractiveSession schedule = getById(id);
        if (!schedule.getHostUserId().equals(host.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the host");
        }
        return schedule;
    }

    private ScheduledInteractiveSession requireOwnedAndSchedulable(String id, User host) {
        ScheduledInteractiveSession schedule = requireOwned(id, host);
        if (schedule.getStatus() != ScheduleStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Schedule is no longer editable (status=" + schedule.getStatus() + ")");
        }
        return schedule;
    }

    private InteractiveSessionInvite createAndSendInvite(ScheduledInteractiveSession schedule,
                                                         String email,
                                                         User host,
                                                         Deck deck) {
        if (inviteRepository.existsByEmailAndScheduledInteractiveSessionId(email, schedule.getId())) {
            // Idempotent — return the existing row instead of duplicating.
            return inviteRepository.findByScheduledInteractiveSessionId(schedule.getId()).stream()
                    .filter(i -> email.equals(i.getEmail()))
                    .findFirst()
                    .orElseThrow();
        }
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setScheduledInteractiveSessionId(schedule.getId());
        invite.setEmail(email);
        invite.setInvitedByUserId(host.getId());
        invite.setInviteToken(generateToken());
        invite.setExpiresAt(schedule.getScheduledStartAt().plusHours(2));
        InteractiveSessionInvite saved = inviteRepository.save(invite);

        String hostName = host.getName() != null ? host.getName() : host.getUserName();
        try {
            emailService.sendInitialInvite(schedule, saved, hostName, deck.getName());
        } catch (Exception e) {
            log.warn("Initial invite send failed for {}: {}", email, e.getMessage());
        }
        return saved;
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static List<String> normaliseEmails(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        return raw.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static CreateInteractiveSessionRequest asCreateRequest(ScheduledInteractiveSession s) {
        InteractiveSessionSettings cfg = s.getSettings();
        return new CreateInteractiveSessionRequest(
                s.getDeckId(),
                cfg.getMode(),
                cfg.getTotalRounds(),
                cfg.getTimePerQuestion(),
                cfg.isSpeedBonus(),
                cfg.isAllowGuests(),
                cfg.getMaxPlayers(),
                cfg.isAllowLateJoin(),
                cfg.isShowScoresImmediately(),
                cfg.isScoringEnabled(),
                cfg.isReactionsEnabled(),
                cfg.isChatEnabled(),
                cfg.isTeamMode(),
                cfg.getTeamCount(),
                cfg.isAutoBalanceTeams(),
                null,                          // customRoomCode — let the live service generate one
                null,                          // anonymousMode — defaulted by settings copy in InteractiveSessionService
                cfg.isShuffleQuestions(),
                cfg.isShuffleAnswers(),
                cfg.isAutoAdvance(),
                cfg.getPodiumDuration(),
                cfg.getLobbyCountdownSeconds(),
                cfg.isRequireFullName(),
                cfg.isSpectatorsAllowed());
    }

    private static InteractiveSessionSettings copyOf(InteractiveSessionSettings src) {
        if (src == null) return new InteractiveSessionSettings();
        InteractiveSessionSettings out = new InteractiveSessionSettings();
        out.setMaxPlayers(src.getMaxPlayers());
        out.setTotalRounds(src.getTotalRounds());
        out.setTimePerQuestion(src.getTimePerQuestion());
        out.setSpeedBonus(src.isSpeedBonus());
        out.setAllowGuests(src.isAllowGuests());
        out.setMode(src.getMode());
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
}
