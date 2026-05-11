/**
 * Request body for PUT /api/decks/{id}.
 * Any null field is left unchanged; only non-null fields are patched.
 * The image URLs use the empty string to mean "clear this field" (vs null = unchanged).
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 50) String category,
        @Size(max = 2000) String coverImageUrl,
        @Size(max = 2000) String backgroundImageUrl) {
}
