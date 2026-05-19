/**
 * Threaded comment on a deck. Top-level comments have a {@code null}
 * {@code parentCommentId}; replies point at their parent. Replies of replies
 * collapse into the same parent for now — the UI renders a two-level thread.
 *
 * Soft-delete (rather than hard-delete) preserves thread shape: removing a
 * parent that has replies would orphan the children, so deletes only set
 * {@code deleted=true}, blank {@code body} to "[removed]", and stamp
 * {@code deletedAt}. The author's name + picture are denormalized so threads
 * keep rendering even if the author is later removed.
 *
 * Upvotes are stored as a {@code Set<String>} of user ids; the integer
 * {@code upvotes} field is the denormalized count so list views can sort
 * without scanning the set. The two fields are written together in one update
 * to stay consistent.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "deck_comments")
// Listing the top-level comments for a deck is the hot path; pair it with a
// createdAt index suffix so the default newest-first sort uses the same index.
@CompoundIndex(name = "deck_parent_created_idx", def = "{'deckId': 1, 'parentCommentId': 1, 'createdAt': -1}")
public class DeckComment {

    @Id
    private String id;

    @Indexed
    private String deckId;

    private String authorUserId;

    /**
     * Denormalized author display fields so deleted users don't leave the
     * thread with empty cells. Updated on every comment write to keep the
     * snapshot reasonably current.
     */
    private String authorName;
    private String authorPictureUrl;

    /** Null for top-level comments; a comment id for replies. */
    private String parentCommentId;

    /** Markdown body, max 4000 chars (service-enforced). */
    private String body;

    private int upvotes;

    /**
     * Set of user ids that have upvoted; doubles as duplicate-prevention and
     * the source of "did I upvote?" lookups so we don't need a join table.
     */
    private Set<String> upvoterUserIds = new HashSet<>();

    /** True after at least one successful edit. */
    private boolean edited;

    /** Soft-delete flag; body is replaced with "[removed]" on delete. */
    private boolean deleted;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime deletedAt;
}
