/**
 * Payload for the host's STOMP /app/interactive-session/{roomCode}/boot message.
 * Identifies the player to remove by their session-scoped {@code playerId} —
 * never the underlying userId.
 */
package cephadex.brainflex.dto;

public record BootPlayerRequest(String playerId) {
}
