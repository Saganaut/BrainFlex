/**
 * MongoDB repository for InteractiveSessionResult documents.
 * Results are looked up by session ID to serve the post-game results
 * screen and verify that stat updates have been applied.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.session.InteractiveSessionResult;

public interface InteractiveSessionResultRepository extends MongoRepository<InteractiveSessionResult, String> {

    Optional<InteractiveSessionResult> findByInteractiveSessionId(String interactiveSessionId);
}
