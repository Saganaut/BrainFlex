/**
 * Paginated payload for GET /api/collections/mine. Same shape as
 * {@link DeckFavoritesPage} / {@link DeckRatingsPage} — keeps cursor /
 * has-more handling consistent across the discovery surface.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record DeckCollectionsPage(
        List<DeckCollectionDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
