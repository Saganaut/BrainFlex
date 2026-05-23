/**
 * Spring Data repository for {@link DeckComment} rows.
 *
 * Top-level vs. reply listings split here so the controller picks the right
 * query for the panel it is rendering. Soft-deleted comments are still
 * returned — the service replaces the body with "[removed]" at write time so
 * thread shape stays intact for replies.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.deck.DeckComment;
public interface DeckCommentRepository extends MongoRepository<DeckComment, String> {

    Page<DeckComment> findAllByDeckIdAndParentCommentIdIsNull(String deckId, Pageable pageable);

    Page<DeckComment> findAllByDeckIdAndParentCommentId(
            String deckId, String parentCommentId, Pageable pageable);

    long countByDeckIdAndParentCommentId(String deckId, String parentCommentId);
}
