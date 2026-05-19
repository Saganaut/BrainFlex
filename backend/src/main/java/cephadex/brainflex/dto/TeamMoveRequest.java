/**
 * Body for {@code PUT /api/showcases/{roomCode}/players/{userId}/team}.
 * The target team is required — there is no "leave team" operation in
 * team-mode showcases.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamMoveRequest(@NotBlank String teamId) {
}
