/**
 * MongoDB repository for Question documents.
 * Questions are queried by content pack so the game service can draw
 * and shuffle a set of questions when a session is created.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.Difficulty;

public interface QuestionRepository extends MongoRepository<Question, String> {

    List<Question> findByDeckId(String deckId);

    List<Question> findByDeckIdAndDifficulty(String deckId, Difficulty difficulty);

    int countByDeckId(String deckId);

    void deleteByDeckId(String deckId);
}
