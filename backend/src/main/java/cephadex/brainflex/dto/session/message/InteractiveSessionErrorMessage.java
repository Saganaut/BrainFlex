/**
 * Sent to /user/queue/errors when a STOMP message handler in
 * InteractiveSessionWebSocketController throws. Lets the host (or any participant) see
 * actionable feedback for actions that don't naturally produce an HTTP response
 * — Start Game, Submit Answer, Next Round, Leave.
 *
 * Mirrors the REST ProblemDetail contract (see z-docs/features/exceptions.md):
 * `status` is the HTTP status (4xx user error / 5xx infrastructure) and `code`
 * is the same machine-readable identifier the frontend branches on. On a 5xx
 * the `message` is the generic, non-leaking string.
 */
package cephadex.brainflex.dto.session.message;

public record InteractiveSessionErrorMessage(
        String operation,   // matches the /app/interactive-session/{code}/<operation> suffix (e.g. "start")
        String roomCode,
        int status,         // 4xx/5xx; 500 when the cause is neither ApiException nor ResponseStatusException
        String message,     // human-readable; generic on 5xx (never ex.getMessage())
        String code         // machine-readable, e.g. SESSION_NOT_FOUND / INTERNAL_ERROR (REST parity)
) {
}
