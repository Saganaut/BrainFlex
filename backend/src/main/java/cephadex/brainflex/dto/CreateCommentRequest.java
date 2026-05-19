/**
 * Request body for {@code POST /api/decks/{id}/comments}. {@code body} is
 * required; {@code parentCommentId} is null for top-level comments and the
 * id of a top-level comment for a reply.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String body,
        String parentCommentId) {
}
