/**
 * STOMP message handlers for real-time showcase events.
 * Each handler delegates immediately to ShowcaseService, which owns all state
 * machine logic, round timing, scoring, and broadcasting.
 *
 * Client subscriptions (server → client):
 *   /topic/showcase/{roomCode}/lobby       — lobby state (player list, status)
 *   /topic/showcase/{roomCode}/round       — RoundStartMessage (question + timer)
 *   /topic/showcase/{roomCode}/answered    — AnswerProgressMessage (who has submitted)
 *   /topic/showcase/{roomCode}/votePhase   — VotePhaseStartMessage (Best Answer mode)
 *   /topic/showcase/{roomCode}/voted       — VoteProgressMessage (who has voted)
 *   /topic/showcase/{roomCode}/roundResult — RoundResultMessage (reveal; optional BestAnswerOutcome)
 *   /topic/showcase/{roomCode}/gameOver    — ShowcaseEndedMessage (final placements)
 *   /user/queue/errors                     — ShowcaseErrorMessage, principal-specific
 *
 * Client sends (client → server via /app prefix):
 *   /app/showcase/{roomCode}/start
 *   /app/showcase/{roomCode}/answer
 *   /app/showcase/{roomCode}/vote
 *   /app/showcase/{roomCode}/nextRound
 *   /app/showcase/{roomCode}/leave
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

import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.dto.BootPlayerRequest;
import cephadex.brainflex.dto.ShowcaseErrorMessage;
import cephadex.brainflex.dto.VoteSubmitRequest;
import cephadex.brainflex.service.ShowcaseService;

@Controller
public class ShowcaseWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ShowcaseWebSocketController.class);

    private final ShowcaseService showcaseService;
    private final SimpMessagingTemplate messagingTemplate;

    public ShowcaseWebSocketController(
            ShowcaseService showcaseService,
            SimpMessagingTemplate messagingTemplate) {
        this.showcaseService = showcaseService;
        this.messagingTemplate = messagingTemplate;
    }

    /** Host transitions the showcase from LOBBY → IN_PROGRESS and fires the first question. */
    @MessageMapping("/showcase/{roomCode}/start")
    public void startGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        showcaseService.startGame(roomCode, principal.getName());
    }

    /** Player submits their answer for the current round. */
    @MessageMapping("/showcase/{roomCode}/answer")
    public void submitAnswer(
            @DestinationVariable String roomCode,
            @Payload AnswerSubmitRequest request,
            Principal principal) {
        showcaseService.submitAnswer(roomCode, request, principal.getName());
    }

    /** Player votes for an anonymous submission during VOTE phase (Best Answer mode). */
    @MessageMapping("/showcase/{roomCode}/vote")
    public void submitVote(
            @DestinationVariable String roomCode,
            @Payload VoteSubmitRequest request,
            Principal principal) {
        showcaseService.submitVote(roomCode, request, principal.getName());
    }

    /**
     * Host advances to the next question in TURN_BASED mode.
     * No-op in SIMULTANEOUS mode (auto-advances after BETWEEN_ROUNDS_DELAY_SECONDS).
     */
    @MessageMapping("/showcase/{roomCode}/nextRound")
    public void nextRound(
            @DestinationVariable String roomCode,
            Principal principal) {
        showcaseService.nextRound(roomCode, principal.getName());
    }

    /** Player voluntarily leaves the showcase; broadcasts updated lobby state. */
    @MessageMapping("/showcase/{roomCode}/leave")
    public void leaveGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        showcaseService.leaveGame(roomCode, principal.getName());
    }

    /** Host removes a player from the showcase. */
    @MessageMapping("/showcase/{roomCode}/boot")
    public void bootPlayer(
            @DestinationVariable String roomCode,
            @Payload BootPlayerRequest request,
            Principal principal) {
        showcaseService.bootPlayer(roomCode, principal.getName(), request.userId());
    }

    /** Host ends the showcase mid-game; computes final placements from current state. */
    @MessageMapping("/showcase/{roomCode}/end")
    public void endShowcase(
            @DestinationVariable String roomCode,
            Principal principal) {
        showcaseService.endShowcaseEarly(roomCode, principal.getName());
    }

    /**
     * Catches any exception thrown by the @MessageMapping handlers above and forwards it
     * to the caller as a ShowcaseErrorMessage on /user/queue/errors. Without this handler
     * a failed Start (e.g. "Content pack has no questions") was silently swallowed by
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
            if (rse.getReason() != null) message = rse.getReason();
        }

        String operation = parseOperation(destination);
        String roomCode = parseRoomCode(destination);

        ShowcaseErrorMessage payload = new ShowcaseErrorMessage(operation, roomCode, status, message);
        log.warn("WS handler error: op={} room={} status={} msg={}", operation, roomCode, status, message);

        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", payload);
        }
    }

    /** Extracts the trailing operation segment from /app/showcase/{code}/{operation}. */
    private static String parseOperation(String destination) {
        if (destination == null) return "unknown";
        int slash = destination.lastIndexOf('/');
        return slash >= 0 ? destination.substring(slash + 1) : destination;
    }

    /** Extracts the room code segment from /app/showcase/{code}/{operation}. */
    private static String parseRoomCode(String destination) {
        if (destination == null) return "";
        String[] parts = destination.split("/");
        // /app/showcase/{code}/{operation} → parts = ["", "app", "showcase", "{code}", "{operation}"]
        return parts.length >= 4 ? parts[parts.length - 2] : "";
    }
}
