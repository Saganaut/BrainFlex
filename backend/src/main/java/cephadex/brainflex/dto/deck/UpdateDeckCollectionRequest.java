/**
 * Request body for PUT /api/collections/{id}. Null fields leave the
 * collection unchanged; non-null fields are patched. Pass an empty-string
 * {@code description} to clear it; pass {@link Image#empty()} to clear the
 * cover.
 *
 * The deck list is mutated through the dedicated add / remove / reorder
 * endpoints, not this body — collections-with-100-decks would otherwise re-
 * send the entire list on every metadata edit.
 */
package cephadex.brainflex.dto.deck;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.Size;

public record UpdateDeckCollectionRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        DeckVisibility visibility,
        Image cover) {
}
