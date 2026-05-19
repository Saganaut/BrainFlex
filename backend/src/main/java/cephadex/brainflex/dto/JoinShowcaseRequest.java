/**
 * Optional body for POST /api/showcases/{roomCode}/join.
 *
 * In individual-mode showcases the body is unused. In team mode with
 * `autoBalanceTeams=false` the caller must include a `teamId` so the server
 * knows which team to assign them to. With `autoBalanceTeams=true` the body
 * may still carry a `teamId` (treated as a preference); the server will
 * honor it if the chosen team has room and assign round-robin otherwise.
 *
 * Chunk 13 — {@code avatarKey} and {@code colorTag} are the lobby's Kahoot-
 * style avatar pick. Both are optional; an unknown key is silently dropped
 * (the player falls back to their real {@code pictureUrl}). The server
 * validates the key against {@link cephadex.brainflex.service.AvatarService}
 * before storing it on the player record.
 */
package cephadex.brainflex.dto;

public record JoinShowcaseRequest(String teamId, String avatarKey, String colorTag) {
}
