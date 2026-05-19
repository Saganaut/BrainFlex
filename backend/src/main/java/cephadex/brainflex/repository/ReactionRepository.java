/**
 * Persistent store for audience reactions sent during a showcase. Live
 * rendering reads aggregate counts from Redis; this collection backs replays
 * and analytics.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Reaction;

public interface ReactionRepository extends MongoRepository<Reaction, String> {

    Page<Reaction> findAllByShowcaseIdOrderBySentAtDesc(String showcaseId, Pageable pageable);

    long countByShowcaseIdAndElementId(String showcaseId, String elementId);

    void deleteByShowcaseId(String showcaseId);
}
