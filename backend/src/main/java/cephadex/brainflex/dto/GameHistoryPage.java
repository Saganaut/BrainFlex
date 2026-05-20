/**
 * Page envelope for the {@code /history} endpoints. Mirrors the shape of
 * {@link DeckFavoritesPage} so the history grid can reuse the same "load
 * more" pattern — {@code hasMore} is precomputed server-side.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record GameHistoryPage(
        List<GameHistoryDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
