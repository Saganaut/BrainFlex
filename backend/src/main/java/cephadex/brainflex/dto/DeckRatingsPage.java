/**
 * Page envelope for {@code GET /api/decks/{id}/ratings}. Mirrors the shape of
 * {@link DeckExploreResponse} so the Reviews tab can reuse the same
 * "load more" logic; {@code averageRating} + {@code ratingCount} are echoed
 * for convenience so the histogram can render without a second deck fetch.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record DeckRatingsPage(
        List<DeckRatingDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore,
        double averageRating,
        int ratingCount,
        int[] starDistribution) {
}
