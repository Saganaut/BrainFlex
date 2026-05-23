/**
 * Server-side filter + sort parameters for {@code GET /api/decks/explore}.
 *
 * Lives as a record so the controller can collect query params with the
 * `@RequestParam`-bound constructor and pass a single object down to
 * {@link cephadex.brainflex.service.DeckService}. {@code sort} is matched
 * case-insensitively against {@link Sort}; an unknown value falls back to
 * {@code TRENDING}.
 */
package cephadex.brainflex.dto.deck;

import cephadex.brainflex.model.enums.Difficulty;

public record DeckExploreRequest(
        String tagId,
        String language,
        Difficulty difficulty,
        Sort sort,
        int page,
        int size) {

    public enum Sort {
        TRENDING,
        NEW,
        TOP_RATED,
        MOST_PLAYED;

        public static Sort parse(String raw) {
            if (raw == null || raw.isBlank()) return TRENDING;
            return switch (raw.trim().toLowerCase()) {
                case "new" -> NEW;
                case "top-rated", "top_rated", "toprated" -> TOP_RATED;
                case "most-played", "most_played", "mostplayed" -> MOST_PLAYED;
                default -> TRENDING;
            };
        }
    }
}
