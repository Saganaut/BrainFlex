/**
 * Persistent store for Best-Answer-mode votes during a interactiveSession.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.BestAnswerVote;

public interface BestAnswerVoteRepository extends MongoRepository<BestAnswerVote, String> {

    List<BestAnswerVote> findByInteractiveSessionIdAndElementId(String interactiveSessionId, String elementId);

    Optional<BestAnswerVote> findByInteractiveSessionIdAndElementIdAndVoterUserId(
            String interactiveSessionId, String elementId, String voterUserId);

    void deleteByInteractiveSessionId(String interactiveSessionId);
}
