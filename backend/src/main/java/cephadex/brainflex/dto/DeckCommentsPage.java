/**
 * Page envelope for the deck-comments listing endpoints. Same shape as the
 * other paginated responses so the Discussion tab can reuse the load-more
 * logic.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record DeckCommentsPage(
        List<DeckCommentDTO> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
