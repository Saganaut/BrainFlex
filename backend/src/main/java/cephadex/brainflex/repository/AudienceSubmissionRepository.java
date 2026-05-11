/**
 * Persistent store for player Q&A and Best-Answer-mode submissions during a showcase.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.AudienceSubmission;

public interface AudienceSubmissionRepository extends MongoRepository<AudienceSubmission, String> {

    List<AudienceSubmission> findByShowcaseIdAndElementId(String showcaseId, String elementId);

    void deleteByShowcaseId(String showcaseId);
}
