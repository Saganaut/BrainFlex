/**
 * STOMP message handlers for real-time game events.
 * Each handler delegates immediately to GameService, which owns all state
 * machine logic, round timing, scoring, and broadcasting.
 *
 * Client subscriptions (server → client):
 *   /topic/game/{roomCode}/lobby       — lobby state (player list, status)
 *   /topic/game/{roomCode}/round       — RoundStartMessage (question + timer)
 *   /topic/game/{roomCode}/roundResult — RoundResultMessage (correct answer + scores)
 *   /topic/game/{roomCode}/gameOver    — GameOverMessage (final placements)
 *
 * Client sends (client → server via /app prefix):
 *   /app/game/{roomCode}/start
 *   /app/game/{roomCode}/answer
 *   /app/game/{roomCode}/nextRound
 *   /app/game/{roomCode}/leave
 */
package cephadex.brainflex.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import cephadex.brainflex.dto.AnswerSubmitRequest;
import cephadex.brainflex.service.GameService;

@Controller
public class GameWebSocketController {

    private final GameService gameService;

    public GameWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Host transitions the session from LOBBY → IN_PROGRESS and fires the first question. */
    @MessageMapping("/game/{roomCode}/start")
    public void startGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        gameService.startGame(roomCode, principal.getName());
    }

    /** Player submits their answer for the current round. */
    @MessageMapping("/game/{roomCode}/answer")
    public void submitAnswer(
            @DestinationVariable String roomCode,
            @Payload AnswerSubmitRequest request,
            Principal principal) {
        gameService.submitAnswer(roomCode, request, principal.getName());
    }

    /**
     * Host advances to the next question in TURN_BASED mode.
     * No-op in SIMULTANEOUS mode (auto-advances after BETWEEN_ROUNDS_DELAY_SECONDS).
     */
    @MessageMapping("/game/{roomCode}/nextRound")
    public void nextRound(
            @DestinationVariable String roomCode,
            Principal principal) {
        gameService.nextRound(roomCode, principal.getName());
    }

    /** Player voluntarily leaves the session; broadcasts updated lobby state. */
    @MessageMapping("/game/{roomCode}/leave")
    public void leaveGame(
            @DestinationVariable String roomCode,
            Principal principal) {
        gameService.leaveGame(roomCode, principal.getName());
    }
}
