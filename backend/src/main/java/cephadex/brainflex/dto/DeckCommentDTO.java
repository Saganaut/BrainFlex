/**
 * Public-facing view of a single {@link cephadex.brainflex.model.DeckComment}.
 *
 * The raw {@code upvoterUserIds} set is intentionally not exposed — clients
 * only need to know "did I upvote?" plus the {@code upvotes} count, both of
 * which can be derived here without leaking who voted for what.
 *
 * {@code replyCount} is populated by the controller for top-level comments
 * so the Discussion tab can render "Show N replies" without an extra count
 * request.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.DeckComment;
import cephadex.brainflex.model.UserSnapshot;

public record DeckCommentDTO(
        String id,
        String deckId,
        UserSnapshot author,
        String parentCommentId,
        String body,
        int upvotes,
        boolean upvotedByMe,
        boolean edited,
        boolean deleted,
        long replyCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DeckCommentDTO of(DeckComment row, String callerUserId, long replyCount) {
        boolean upvotedByMe = callerUserId != null
                && row.getUpvoterUserIds() != null
                && row.getUpvoterUserIds().contains(callerUserId);
        return new DeckCommentDTO(
                row.getId(),
                row.getDeckId(),
                row.getAuthor(),
                row.getParentCommentId(),
                row.getBody(),
                row.getUpvotes(),
                upvotedByMe,
                row.isEdited(),
                row.isDeleted(),
                replyCount,
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
