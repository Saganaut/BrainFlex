/**
 * Tiny envelope returned by the favorite / unfavorite endpoints. Carries just
 * enough state for the client to confirm the optimistic flip: which deck, the
 * caller's resulting {@code isFavorited} flag, and the post-mutation
 * {@code favoriteCount} so the UI doesn't have to recompute it.
 */
package cephadex.brainflex.dto.deck;

public record DeckFavoriteResponse(
        String deckId,
        boolean isFavorited,
        long favoriteCount) {
}
