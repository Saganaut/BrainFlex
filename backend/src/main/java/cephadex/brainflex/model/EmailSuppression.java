/**
 * Per-address opt-out list. One row per email address; the {@code blocked}
 * set says which categories are suppressed (MARKETING is the common case).
 * TRANSACTIONAL and SYSTEM ignore this table by design — see the dispatcher.
 *
 * The address is stored lower-cased and trimmed to keep equality comparisons
 * boring; {@code emailLower} carries a unique index so a double-write from
 * concurrent unsubscribe clicks collapses to a single row.
 *
 * Reasons:
 *   - UNSUBSCRIBE — user clicked the link.
 *   - BOUNCE      — provider reported a permanent failure.
 *   - COMPLAINT   — provider reported a spam complaint.
 *   - MANUAL      — operator-added (support / runbook).
 */
package cephadex.brainflex.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.service.email.EmailCategory;
import lombok.Data;

@Data
@Document(collection = "email_suppression")
public class EmailSuppression {

    public enum Reason { UNSUBSCRIBE, BOUNCE, COMPLAINT, MANUAL }

    @Id
    private String id;

    @Indexed(unique = true)
    private String emailLower;

    private Set<EmailCategory> blocked = EnumSet.noneOf(EmailCategory.class);

    private Reason reason;

    private Instant createdAt = Instant.now();
}
