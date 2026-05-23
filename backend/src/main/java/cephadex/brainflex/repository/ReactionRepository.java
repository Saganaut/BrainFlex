/**
 * Persistent store for audience reactions sent during a interactiveSession. Live
 * rendering reads aggregate counts from Redis; this collection backs replays
 * and analytics.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.session.Reaction;
public interface ReactionRepository extends MongoRepository<Reaction, String> {

    Page<Reaction> findAllByInteractiveSessionIdOrderBySentAtDesc(String interactiveSessionId, Pageable pageable);

    long countByInteractiveSessionIdAndElementId(String interactiveSessionId, String elementId);

    void deleteByInteractiveSessionId(String interactiveSessionId);
}
