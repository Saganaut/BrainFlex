/**
 * Page envelope for {@code GET /api/decks/{id}/ratings}. Extends the standard
 * {@link Page} shape with {@code averageRating} / {@code ratingCount} /
 * {@code starDistribution} so the histogram can render without a second deck
 * fetch — those extras are why this endpoint keeps its own record instead of
 * returning the generic envelope.
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
