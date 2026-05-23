/**
 * Persistent store for player Q&A and Best-Answer-mode submissions during a interactiveSession.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.session.AudienceSubmission;
public interface AudienceSubmissionRepository extends MongoRepository<AudienceSubmission, String> {

    List<AudienceSubmission> findByInteractiveSessionIdAndElementId(String interactiveSessionId, String elementId);

    void deleteByInteractiveSessionId(String interactiveSessionId);
}
