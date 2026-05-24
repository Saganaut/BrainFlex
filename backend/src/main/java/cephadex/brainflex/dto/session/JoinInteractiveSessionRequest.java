/**
 * Optional body for POST /api/interactive-sessions/{roomCode}/join.
 *
 * In individual-mode interactiveSessions the body is unused. In team mode with
 * `autoBalanceTeams=false` the caller must include a `teamId` so the server
 * knows which team to assign them to. With `autoBalanceTeams=true` the body
 * may still carry a `teamId` (treated as a preference); the server will
 * honor it if the chosen team has room and assign round-robin otherwise.
 *
 * NOTE: the chunk-13 `avatarKey` / `colorTag` lobby-avatar pick was removed
 * along with the preset roster. A player keeps their real `pictureUrl` (a LINK
 * avatar); the picker will be rebuilt later and can re-add fields here then.
 */
package cephadex.brainflex.dto.session;

public record JoinInteractiveSessionRequest(String teamId) {
}
