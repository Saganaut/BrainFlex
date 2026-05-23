/**
 * Body for {@code PUT /api/interactive-sessions/{roomCode}/players/{playerId}/team}.
 * The target team is required — there is no "leave team" operation in
 * team-mode interactiveSessions.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamMoveRequest(@NotBlank String teamId) {
}
