/**
 * Client → server payload at /app/showcase/{roomCode}/vote.
 *
 * `elementId` is included so a stale vote (player still on the previous round
 * after the server has advanced) can be safely ignored without crediting it
 * against the current round.
 */
package cephadex.brainflex.dto;

public record VoteSubmitRequest(
        String elementId,
        String submissionId) {
}
