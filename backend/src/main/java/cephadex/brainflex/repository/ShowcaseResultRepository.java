/**
 * MongoDB repository for ShowcaseResult documents.
 * Results are looked up by session ID to serve the post-game results
 * screen and verify that stat updates have been applied.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.ShowcaseResult;

public interface ShowcaseResultRepository extends MongoRepository<ShowcaseResult, String> {

    Optional<ShowcaseResult> findByShowcaseId(String showcaseId);
}
