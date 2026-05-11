/**
 * MongoDB repository for Question documents.
 * Questions are queried by deck so the showcase service can draw a set of
 * elements when a session is created. Results are ordered by `position` ASC so
 * decks play in authored order; the `_id` tiebreaker keeps ordering stable for
 * legacy documents that share a null/missing position.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.Difficulty;

public interface QuestionRepository extends MongoRepository<Question, String> {

    List<Question> findByDeckIdOrderByPositionAscIdAsc(String deckId);

    /** @deprecated Prefer findByDeckIdOrderByPositionAscIdAsc for stable ordering. */
    @Deprecated
    default List<Question> findByDeckId(String deckId) {
        return findByDeckIdOrderByPositionAscIdAsc(deckId);
    }

    List<Question> findByDeckIdAndDifficulty(String deckId, Difficulty difficulty);

    int countByDeckId(String deckId);

    void deleteByDeckId(String deckId);
}
