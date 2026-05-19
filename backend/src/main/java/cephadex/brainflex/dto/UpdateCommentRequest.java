/**
 * Request body for {@code PUT /api/decks/{deckId}/comments/{commentId}}.
 * The author can replace the body; everything else (parent, upvotes, author
 * identity) is immutable from this endpoint.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank String body) {
}
