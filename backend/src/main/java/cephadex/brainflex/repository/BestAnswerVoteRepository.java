/**
 * Persistent store for Best-Answer-mode votes during a showcase.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.BestAnswerVote;

public interface BestAnswerVoteRepository extends MongoRepository<BestAnswerVote, String> {

    List<BestAnswerVote> findByShowcaseIdAndElementId(String showcaseId, String elementId);

    Optional<BestAnswerVote> findByShowcaseIdAndElementIdAndVoterUserId(
            String showcaseId, String elementId, String voterUserId);

    void deleteByShowcaseId(String showcaseId);
}
