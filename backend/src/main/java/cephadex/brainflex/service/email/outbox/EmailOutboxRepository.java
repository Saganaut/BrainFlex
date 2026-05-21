/**
 * Spring Data repository for {@link EmailOutboxEntry}. The atomic batch claim
 * that powers the worker poll is hand-rolled via {@code MongoTemplate} in
 * {@link EmailOutboxWorker} — this interface only exposes the lookups that
 * are safe to do through derived queries.
 */
package cephadex.brainflex.service.email.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailOutboxRepository extends MongoRepository<EmailOutboxEntry, String> {

    List<EmailOutboxEntry> findByRecipientOrderByCreatedAtDesc(String recipient);

    long countByStatus(EmailOutboxEntry.Status status);

    /**
     * Used by the stuck-row sweep: any row that has been CLAIMED for longer
     * than the lease window is presumed to have died mid-dispatch.
     */
    List<EmailOutboxEntry> findByStatusAndClaimedAtBefore(
            EmailOutboxEntry.Status status, Instant cutoff);
}
