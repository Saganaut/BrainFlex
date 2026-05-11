/**
 * Payload for the host's STOMP /app/showcase/{roomCode}/boot message.
 * Identifies the player to remove from the showcase by userId.
 */
package cephadex.brainflex.dto;

public record BootPlayerRequest(String userId) {
}
