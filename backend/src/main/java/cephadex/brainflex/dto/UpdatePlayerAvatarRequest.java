/**
 * Body for PUT /api/interactive-sessions/{roomCode}/me/avatar.
 *
 * Chunk 13 — lobby avatar picker. Lets a player update their {@code avatarKey}
 * (and optionally {@code colorTag}) after joining, so the picker can live in
 * the lobby UI rather than gating the join itself. Unknown {@code avatarKey}
 * values are silently dropped on the server (forward-compatible with stale
 * client lists).
 */
package cephadex.brainflex.dto;

public record UpdatePlayerAvatarRequest(String avatarKey, String colorTag) {
}
