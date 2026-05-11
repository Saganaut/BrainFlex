/**
 * Request body for POST /api/decks.
 * Creates a new user-owned content pack with no questions; questions are added separately.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 50) String category) {
}
