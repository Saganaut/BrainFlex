/**
 * Request body for POST /api/collections/{id}/decks.
 *
 * {@code position} is optional: when null the deck is appended to the end of
 * the list, mirroring the editor's "Add slide" UX. Negative or out-of-bounds
 * positions are clamped server-side so the client can pass any reasonable
 * value without pre-validating against the live list length.
 */
package cephadex.brainflex.dto.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddDeckToCollectionRequest(
        @NotBlank @Size(max = 64) String deckId,
        Integer position) {
}
