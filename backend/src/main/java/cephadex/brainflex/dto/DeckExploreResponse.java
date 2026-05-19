/**
 * Page envelope for {@code GET /api/decks/explore}. Carries the requested page
 * of decks plus enough pagination signal for the Explore grid to render a
 * "load more" affordance without a separate count request.
 *
 * {@code hasMore} is computed server-side from the requested {@code size}
 * vs. {@code totalElements} so the client doesn't have to guess.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record DeckExploreResponse(
        List<DeckDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
