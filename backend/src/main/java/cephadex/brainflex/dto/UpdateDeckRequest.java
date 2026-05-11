/**
 * Request body for PUT /api/decks/{id}.
 * Any null field is left unchanged; only non-null fields are patched.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 50) String category) {
}
