/**
 * Broadcast on /topic/interactive-session/{roomCode}/responsesRevealed when
 * the host manually reveals an in-flight round's response distribution
 * (chunk 24, ON_CLICK show-responses mode). The round does not end — the
 * client just mounts the response panel.
 *
 * Idempotent: the server only emits this once per element per session;
 * subsequent /reveal calls for the same elementId are no-ops.
 */
package cephadex.brainflex.dto;

public record ResponsesRevealedMessage(int round, String elementId) {
}
