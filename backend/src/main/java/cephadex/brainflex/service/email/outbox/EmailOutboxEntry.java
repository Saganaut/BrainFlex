/**
 * Persistent record of an enqueued send. The outbox is the source of truth:
 * every send is a row first, dispatched second.
 *
 * Status lifecycle: PENDING → CLAIMED → (SENT | FAILED | SUPPRESSED). The
 * CLAIMED state exists so two worker instances (or one worker retrying after
 * a crash) don't both dispatch the same row — the dispatcher uses a Mongo
 * findAndModify to atomically flip PENDING→CLAIMED on a batch.
 *
 * Indexes:
 *   - {@code (status, scheduledFor)} — worker batch claim.
 *   - {@code (recipient, createdAt DESC)} — per-recipient history for support.
 *   - TTL on {@code sentAt} (90d) — keep the collection bounded for
 *     successful sends; FAILED rows have a null sentAt and stay indefinitely
 *     so an operator can review them.
 *
 * The TTL is intentionally only triggered when {@code sentAt} is set, by using
 * {@code expireAfterSeconds} on that field. Mongo's TTL monitor ignores rows
 * where the indexed field is missing.
 */
package cephadex.brainflex.service.email.outbox;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.service.email.EmailCategory;
import cephadex.brainflex.service.email.EmailTemplate;
import lombok.Data;

@Data
@Document(collection = "email_outbox")
@CompoundIndexes({
        @CompoundIndex(name = "status_scheduledFor_idx", def = "{'status': 1, 'scheduledFor': 1}"),
        @CompoundIndex(name = "recipient_created_idx", def = "{'recipient': 1, 'createdAt': -1}")
})
public class EmailOutboxEntry {

    public enum Status { PENDING, CLAIMED, SENT, FAILED, SUPPRESSED }

    @Id
    private String id;

    private String recipient;

    /** Nullable — caller may not have a user row for the address. */
    private String userId;

    private EmailCategory category;
    private EmailTemplate template;

    /** Thymeleaf model. Stored as a plain map so the worker can re-render. */
    private Map<String, Object> model = new HashMap<>();

    private Status status = Status.PENDING;

    private int attempts;

    /** Last provider/render error message; nullable. Capped at 512 chars on write. */
    private String lastError;

    /**
     * The earliest time the worker is allowed to pick this row up. Set to
     * {@code createdAt} for immediate sends; pushed forward by retry backoff.
     * Indexed via the status_scheduledFor compound index.
     */
    private Instant scheduledFor;

    private Instant createdAt = Instant.now();

    /** Stamped when status flips to CLAIMED so stuck rows can be reclaimed. */
    private Instant claimedAt;

    /**
     * Set when status flips to SENT. TTL index drops the row 90 days later;
     * FAILED / SUPPRESSED rows have a null sentAt and stick around.
     */
    @Indexed(name = "sentAt_ttl_idx", expireAfterSeconds = 60 * 60 * 24 * 90)
    private Instant sentAt;
}
