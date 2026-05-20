/**
 * Sent to /user/queue/errors when a STOMP message handler in
 * InteractiveSessionWebSocketController throws. Lets the host (or any participant) see
 * actionable feedback for actions that don't naturally produce an HTTP response
 * — Start Game, Submit Answer, Next Round, Leave.
 *
 * The `status` mirrors the HTTP status of a ResponseStatusException so the client
 * can decide how to render (4xx is usually user error; 5xx is infrastructure).
 */
package cephadex.brainflex.dto;

public record InteractiveSessionErrorMessage(
        String operation,   // matches the /app/interactive-session/{code}/<operation> suffix (e.g. "start")
        String roomCode,
        int status,         // 4xx/5xx; 500 when the cause isn't a ResponseStatusException
        String message      // human-readable, sourced from ResponseStatusException.reason or ex.message
) {
}
