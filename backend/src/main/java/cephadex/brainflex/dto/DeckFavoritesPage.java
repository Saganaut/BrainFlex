/**
 * Page envelope for {@code GET /api/users/me/favorites}. Mirrors the shape of
 * {@link DeckExploreResponse} so the favorites grid can reuse the same
 * "load more" logic — {@code hasMore} is precomputed server-side.
 *
 * Items are full {@link DeckDTO}s (not the underlying join rows) so the card
 * can render without a second lookup; {@code isFavorited} is always
 * {@code true} here by definition.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record DeckFavoritesPage(
        List<DeckDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
