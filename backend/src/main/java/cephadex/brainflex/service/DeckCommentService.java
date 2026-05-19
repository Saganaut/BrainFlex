/**
 * Business logic for {@link cephadex.brainflex.model.DeckComment} threads.
 *
 * Two-level thread model: top-level comments (parent=null) and replies (parent
 * pointing at a top-level id). Replies of replies are not modelled — the
 * controller layer rejects them so the UI never has to render a deeper tree.
 *
 * Soft-delete preserves thread shape: a deleted parent still serves replies,
 * and the body is replaced with {@code "[removed]"} so the redacted text is
 * server-side, not a client-side render hint.
 *
 * Upvotes are toggled via {@link #toggleUpvote(String, String, String)};
 * idempotent under repeat clicks, and the {@code upvotes} counter is rewritten
 * from {@code upvoterUserIds.size()} every time so the two fields can't drift.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.DeckComment;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckCommentRepository;

@Service
public class DeckCommentService {

    static final int MAX_BODY_CHARS = 4000;
    static final String REMOVED_BODY = "[removed]";

    private final DeckCommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;
    private final UserImageHydrator userImageHydrator;

    public DeckCommentService(
            DeckCommentRepository commentRepository,
            MongoTemplate mongoTemplate,
            UserImageHydrator userImageHydrator) {
        this.commentRepository = commentRepository;
        this.mongoTemplate = mongoTemplate;
        this.userImageHydrator = userImageHydrator;
    }

    /**
     * Create a top-level comment ({@code parentCommentId == null}) or a reply
     * on an existing top-level comment. Replies of replies are rejected so the
     * thread stays two levels deep.
     */
    public DeckComment create(String deckId, User author, String body, String parentCommentId) {
        String normalizedBody = normalizeBody(body);
        String resolvedParent = null;
        if (parentCommentId != null && !parentCommentId.isBlank()) {
            DeckComment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent comment not found"));
            if (!deckId.equals(parent.getDeckId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Parent comment belongs to a different deck");
            }
            if (parent.getParentCommentId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Comment threads are limited to one level of replies");
            }
            resolvedParent = parent.getId();
        }
        DeckComment row = new DeckComment();
        row.setId(UUID.randomUUID().toString());
        row.setDeckId(deckId);
        row.setAuthorUserId(author.getId());
        row.setAuthorName(author.getUserName());
        row.setAuthorPictureUrl(userImageHydrator.pictureUrlOf(author));
        row.setParentCommentId(resolvedParent);
        row.setBody(normalizedBody);
        row.setUpvotes(0);
        row.setUpvoterUserIds(new HashSet<>());
        row.setEdited(false);
        row.setDeleted(false);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(row);
    }

    /**
     * Replace the body of an existing comment. Only the author can edit. The
     * author display snapshot is refreshed in case the user has since renamed
     * themselves or changed their picture.
     */
    public DeckComment edit(String deckId, String commentId, User caller, String body) {
        DeckComment row = requireComment(deckId, commentId);
        if (!caller.getId().equals(row.getAuthorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can edit this comment");
        }
        if (row.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Comment has been deleted");
        }
        row.setBody(normalizeBody(body));
        row.setAuthorName(caller.getUserName());
        row.setAuthorPictureUrl(userImageHydrator.pictureUrlOf(caller));
        row.setEdited(true);
        row.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(row);
    }

    /**
     * Soft-delete: the row stays so replies don't orphan, but the body is
     * blanked to {@code "[removed]"}. Idempotent — repeating it is a no-op.
     */
    public DeckComment softDelete(String deckId, String commentId, User caller) {
        DeckComment row = requireComment(deckId, commentId);
        if (!caller.getId().equals(row.getAuthorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this comment");
        }
        if (row.isDeleted()) return row;
        row.setDeleted(true);
        row.setBody(REMOVED_BODY);
        row.setDeletedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(row);
    }

    /**
     * Toggle an upvote: idempotent in both directions. Returns the row after
     * the toggle so callers can echo the new state in one round-trip.
     */
    public DeckComment toggleUpvote(String deckId, String commentId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to upvote");
        }
        DeckComment row = requireComment(deckId, commentId);
        Set<String> voters = row.getUpvoterUserIds() == null
                ? new HashSet<>()
                : new HashSet<>(row.getUpvoterUserIds());
        if (voters.contains(userId)) {
            voters.remove(userId);
        } else {
            voters.add(userId);
        }
        Update update = new Update()
                .set("upvoterUserIds", voters)
                .set("upvotes", voters.size())
                .set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(commentId)),
                update,
                DeckComment.class);
        row.setUpvoterUserIds(voters);
        row.setUpvotes(voters.size());
        row.setUpdatedAt(LocalDateTime.now());
        return row;
    }

    /** Page of top-level comments on a deck, newest first. */
    public Page<DeckComment> listTopLevel(String deckId, Pageable pageable) {
        return commentRepository.findAllByDeckIdAndParentCommentIdIsNull(deckId, pageable);
    }

    /** Page of replies to a top-level comment, oldest first (natural thread order). */
    public Page<DeckComment> listReplies(String deckId, String parentCommentId, Pageable pageable) {
        return commentRepository.findAllByDeckIdAndParentCommentId(deckId, parentCommentId, pageable);
    }

    public long countReplies(String deckId, String parentCommentId) {
        return commentRepository.countByDeckIdAndParentCommentId(deckId, parentCommentId);
    }

    private DeckComment requireComment(String deckId, String commentId) {
        DeckComment row = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!deckId.equals(row.getDeckId())) {
            // 404 not 400: from the caller's perspective the comment doesn't
            // exist under this deck even though the id exists elsewhere.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        return row;
    }

    private static String normalizeBody(String body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body must not be blank");
        }
        if (trimmed.length() > MAX_BODY_CHARS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "body must be at most " + MAX_BODY_CHARS + " characters");
        }
        return trimmed;
    }
}
