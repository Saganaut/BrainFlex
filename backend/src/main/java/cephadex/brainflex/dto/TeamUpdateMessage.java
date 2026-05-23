/**
 * Broadcast over {@code /topic/interactive-session/{roomCode}/teams} whenever a team's
 * roster, score, or shape changes (creation, rename, recolor, delete,
 * membership move, captain hand-off, or per-answer score recompute).
 *
 * Carries the full team list + the player→team mapping so clients don't have
 * to merge incremental updates: they replace local state with the broadcast
 * payload. Memberships are keyed by session-scoped {@code playerId} — never
 * the real userId — to match the identity used everywhere else in the lobby
 * and round broadcasts.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.org.Team;

public record TeamUpdateMessage(
        List<Team> teams,
        List<TeamMembership> memberships) {

    public record TeamMembership(String playerId, String teamId) {
    }
}
