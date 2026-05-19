/**
 * Request body for PATCH /api/collections/{id}/decks.
 *
 * Replaces the entire ordered list of {@code deckIds}. The server validates
 * that the new list is a permutation of the current list — adds and removes
 * still go through the dedicated add / remove endpoints so a botched reorder
 * cannot silently drop or duplicate ids.
 */
package cephadex.brainflex.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReorderCollectionDecksRequest(
        @NotNull List<@Size(max = 64) String> deckIds) {
}
