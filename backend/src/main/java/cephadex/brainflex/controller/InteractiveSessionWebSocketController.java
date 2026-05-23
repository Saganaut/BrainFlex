/**
 * STOMP message handlers for real-time interactiveSession events.
 * Each handler delegates immediately to InteractiveSessionService, which owns all state
 * machine logic, round timing, scoring, and broadcasting.
 *
 * Client subscriptions (server → client):
 *   /topic/interactive-session/{roomCode}/lobby       — lobby state (player list, status)
 *   /topic/interactive-session/{roomCode}/round       — RoundStartMessage (question + timer)
 *   /topic/interactive-session/{roomCode}/answered    — AnswerProgressMessage (who has submitted)
 *   /topic/interactive-session/{roomCode}/votePhase   — VotePhaseStartMessage (Best Answer mode)
 *   /topic/interactive-session/{roomCode}/voted       — VoteProgressMessage (who has voted)
 *   /topic/interactive-session/{roomCode}/roundResult — RoundResultMessage (reveal; optional BestAnswerOutcome)
 *   /topic/interactive-session/{roomCode}/ended       — InteractiveSessionEndedMessage (final placements)
 *   /topic/interactive-session/{roomCode}/submissionsClosing — SubmissionsClosingMessage (flush drafts; chunk 25)
 *   /topic/interactive-session/{roomCode}/timerState  — TimerStateMessage (pause/resume; chunk 25)
 *   /user/queue/errors                     — InteractiveSessionErrorMessage, principal-specific
 *
 * Client sends (client → server via /app prefix):
 *   /app/interactive-session/{roomCode}/start
 *   /app/interactive-session/{roomCode}/answer
 *   /app/interactive-session/{roomCode}/vote
 *   /app/interactive-session/{roomCode}/nextRound
 *   /app/interactive-session/{roomCode}/leave
 *   /app/interactive-session/{roomCode}/reaction       — audience emoji burst
 *   /app/interactive-session/{roomCode}/chat           — audience chat message
 *   /app/interactive-session/{roomCode}/chat/moderate  — host hides one message
 *   /app/interactive-session/{roomCode}/endSubmit      — host ends submit phase (chunk 25)
 *   /app/interactive-session/{roomCode}/restart        — host restarts from round 1 (chunk 25)
 *   /app/interactive-session/{roomCode}/pauseTimer     — host pauses the round countdown (chunk 25)
 *   /app/interactive-session/{roomCode}/resumeTimer    — host resumes the round countdown (chunk 25)
 */
package cephadex.brainflex.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.session.AnswerSubmitRequest;
import cephadex.brainflex.dto.session.BootPlayerRequest;
import cephadex.brainflex.dto.session.ChatSendRequest;
import cephadex.brainflex.dto.session.EndSubmitRequest;
import cephadex.brainflex.dto.session.FreezeResponsesRequest;
import cephadex.brainflex.dto.session.message.InteractiveSessionErrorMessage;
import cephadex.brainflex.dto.session.ModerateChatRequest;
import cephadex.brainflex.dto.session.ReactionSendRequest;
import cephadex.brainflex.dto.session.RevealNowRequest;
import cephadex.brainflex.dto.session.VoteSubmitRequest;
import cephadex.brainflex.service.InteractiveSessionService;

@Controller
public class InteractiveSessionWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(InteractiveSessionWebSocketController.class);

    private final InteractiveSessionService interactiveSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    public InteractiveSessionWebSocketController(
            InteractiveSessionService interactiveSessionService,
            SimpMessagingTemplate messagingTemplate) {
        this.interactiveSessionService = interactiveSessionService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Host transitions the interactiveSession from LOBBY → IN_PROGRESS and fires
     * the first question.
     */
    @MessageMapping("/interactive-session/{roomCode}/start")
    public void startGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.startGame(roomCode, principal.getName());
    }

    /** Player submits their answer for the current round. */
    @MessageMapping("/interactive-session/{roomCode}/answer")
    public void submitAnswer(
            @DestinationVariable String roomCode,
            @Payload AnswerSubmitRequest request,
            Principal principal) {
        interactiveSessionService.submitAnswer(roomCode, request, principal.getName());
    }

    /**
     * Player votes for an anonymous submission during VOTE phase (Best Answer
     * mode).
     */
    @MessageMapping("/interactive-session/{roomCode}/vote")
    public void submitVote(
            @DestinationVariable String roomCode,
            @Payload VoteSubmitRequest request,
            Principal principal) {
        interactiveSessionService.submitVote(roomCode, request, principal.getName());
    }

    /**
     * Host advances to the next question in TURN_BASED mode.
     * No-op in SIMULTANEOUS mode (auto-advances after
     * BETWEEN_ROUNDS_DELAY_SECONDS).
     */
    @MessageMapping("/interactive-session/{roomCode}/nextRound")
    public void nextRound(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.nextRound(roomCode, principal.getName());
    }

    /**
     * Player voluntarily leaves the interactiveSession; broadcasts updated lobby
     * state.
     */
    @MessageMapping("/interactive-session/{roomCode}/leave")
    public void leaveGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.leaveGame(roomCode, principal.getName());
    }

    /** Host removes a player from the interactiveSession. */
    @MessageMapping("/interactive-session/{roomCode}/boot")
    public void bootPlayer(
            @DestinationVariable String roomCode,
            @Payload BootPlayerRequest request,
            Principal principal) {
        interactiveSessionService.bootPlayer(roomCode, principal.getName(), request.playerId());
    }

    /**
     * Host ends the interactiveSession mid-game; computes final placements from
     * current state.
     */
    @MessageMapping("/interactive-session/{roomCode}/end")
    public void endInteractiveSession(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.endInteractiveSessionEarly(roomCode, principal.getName());
    }

    /**
     * Audience emoji burst; broadcast to
     * /topic/interactive-session/{roomCode}/reaction.
     */
    @MessageMapping("/interactive-session/{roomCode}/reaction")
    public void sendReaction(
            @DestinationVariable String roomCode,
            @Payload ReactionSendRequest request,
            Principal principal) {
        interactiveSessionService.acceptReaction(roomCode, request, principal.getName());
    }

    /**
     * Audience chat message; broadcast to
     * /topic/interactive-session/{roomCode}/chat.
     */
    @MessageMapping("/interactive-session/{roomCode}/chat")
    public void sendChat(
            @DestinationVariable String roomCode,
            @Payload ChatSendRequest request,
            Principal principal) {
        interactiveSessionService.acceptChat(roomCode, request, principal.getName());
    }

    /**
     * Host moderates (hides) one chat message; rebroadcast on
     * /topic/interactive-session/{roomCode}/chat.
     */
    @MessageMapping("/interactive-session/{roomCode}/chat/moderate")
    public void moderateChat(
            @DestinationVariable String roomCode,
            @Payload ModerateChatRequest request,
            Principal principal) {
        interactiveSessionService.moderateChatMessage(roomCode, request.messageId(), principal.getName());
    }

    /**
     * Chunk 24 — host manually reveals the response distribution for the
     * current ON_CLICK round. No-op (idempotent) when the resolved
     * showResponses isn't ON_CLICK or when the round isn't in SUBMIT.
     */
    @MessageMapping("/interactive-session/{roomCode}/reveal")
    public void revealNow(
            @DestinationVariable String roomCode,
            @Payload RevealNowRequest request,
            Principal principal) {
        interactiveSessionService.revealNow(roomCode, request.elementId(), principal.getName());
    }

    /**
     * Chunk 24 — host flips the current round's responseMode (ACCEPTING /
     * NOT_ACCEPTING). Affects only the current run — never mutates the
     * source element on the deck. Late submissions are rejected with 409.
     */
    @MessageMapping("/interactive-session/{roomCode}/freeze")
    public void freezeResponses(
            @DestinationVariable String roomCode,
            @Payload FreezeResponsesRequest request,
            Principal principal) {
        interactiveSessionService.freezeResponses(roomCode, request.elementId(),
                request.mode(), principal.getName());
    }

    /**
     * Chunk 25 — host ends the submit phase for the current round. Broadcasts a
     * SubmissionsClosingMessage so devices flush typed-but-unsubmitted drafts,
     * then (after a grace window) freezes the round and reveals results.
     */
    @MessageMapping("/interactive-session/{roomCode}/endSubmit")
    public void endSubmit(
            @DestinationVariable String roomCode,
            @Payload EndSubmitRequest request,
            Principal principal) {
        interactiveSessionService.endSubmitPhase(roomCode, request.elementId(), principal.getName());
    }

    /**
     * Chunk 25 — host restarts the session from round 1, keeping the roster but
     * clearing scores/answers and the reveal/freeze overlays.
     */
    @MessageMapping("/interactive-session/{roomCode}/restart")
    public void restart(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.restart(roomCode, principal.getName());
    }

    /** Chunk 25 — host pauses the current round's countdown. */
    @MessageMapping("/interactive-session/{roomCode}/pauseTimer")
    public void pauseTimer(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.pauseTimer(roomCode, principal.getName());
    }

    /** Chunk 25 — host resumes a paused round countdown. */
    @MessageMapping("/interactive-session/{roomCode}/resumeTimer")
    public void resumeTimer(
            @DestinationVariable String roomCode,
            Principal principal) {
        interactiveSessionService.resumeTimer(roomCode, principal.getName());
    }

    /**
     * Catches any exception thrown by the @MessageMapping handlers above and
     * forwards it
     * to the caller as a InteractiveSessionErrorMessage on /user/queue/errors.
     * Without this handler
     * a failed Start (e.g. "Content deck has no questions") was silently swallowed
     * by
     * STOMP — clients had no way to surface the failure.
     */
    @MessageExceptionHandler(Throwable.class)
    public void handleException(
            Throwable ex,
            Principal principal,
            SimpMessageHeaderAccessor headers,
            @Header(value = "simpDestination", required = false) String destination) {
        int status = 500;
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal error";
        if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode().value();
            if (rse.getReason() != null)
                message = rse.getReason();
        }

        String operation = parseOperation(destination);
        String roomCode = parseRoomCode(destination);

        InteractiveSessionErrorMessage payload = new InteractiveSessionErrorMessage(operation, roomCode, status,
                message);
        log.warn("WS handler error: op={} room={} status={} msg={}", operation, roomCode, status, message);

        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", payload);
        }
    }

    /**
     * Extracts the trailing operation segment from
     * /app/interactive-session/{code}/{operation}.
     */
    private static String parseOperation(String destination) {
        if (destination == null)
            return "unknown";
        int slash = destination.lastIndexOf('/');
        return slash >= 0 ? destination.substring(slash + 1) : destination;
    }

    /**
     * Extracts the room code segment from
     * /app/interactive-session/{code}/{operation}.
     */
    private static String parseRoomCode(String destination) {
        if (destination == null)
            return "";
        String[] parts = destination.split("/");
        // /app/interactive-session/{code}/{operation} → parts = ["", "app",
        // "interactiveSession", "{code}", "{operation}"]
        return parts.length >= 4 ? parts[parts.length - 2] : "";
    }
}
