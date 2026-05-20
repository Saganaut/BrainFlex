/**
 * Payload for the host's STOMP /app/interactive-session/{roomCode}/boot message.
 * Identifies the player to remove from the interactiveSession by userId.
 */
package cephadex.brainflex.dto;

public record BootPlayerRequest(String userId) {
}
