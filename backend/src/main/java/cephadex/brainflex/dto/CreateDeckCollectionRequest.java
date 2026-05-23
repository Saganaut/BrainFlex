/**
 * Request body for POST /api/collections.
 *
 * The deck list is intentionally not part of the create payload — collections
 * are created empty and decks are added via the
 * {@code POST /api/collections/{id}/decks} endpoint. Keeping the two surfaces
 * separate means the "create modal" can return a usable id immediately and
 * the deck-add flow can be reused later by the "Add to collection" menu.
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckCollectionRequest(
        // Optional client-supplied UUID so the frontend can seed its cache
        // optimistically and reference the collection before the round-trip
        // returns. Mirrors the deck-create flow.
        @Size(max = 64) String id,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 64) String organizationId,
        DeckVisibility visibility,
        Image cover) {
}
