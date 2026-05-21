/**
 * Read/write side of the suppression list. Used by the dispatcher to decide
 * whether to drop a MARKETING send, and by the unsubscribe endpoint to record
 * the user's choice.
 *
 * The {@link #suppress(String, EmailCategory, EmailSuppression.Reason)} path
 * is idempotent: a second click on the unsubscribe link simply unions the
 * category into the existing {@code blocked} set and updates the reason.
 */
package cephadex.brainflex.service.email;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.EmailSuppression;
import cephadex.brainflex.repository.EmailSuppressionRepository;

@Service
public class EmailSuppressionService {

    private static final Logger log = LoggerFactory.getLogger(EmailSuppressionService.class);

    private final EmailSuppressionRepository repository;

    public EmailSuppressionService(EmailSuppressionRepository repository) {
        this.repository = repository;
    }

    /** True if {@code email} has opted out of (or been bounce-listed from) {@code category}. */
    public boolean isSuppressed(String email, EmailCategory category) {
        if (email == null || category == null) return false;
        // TRANSACTIONAL and SYSTEM ignore suppression — the dispatcher should
        // not even ask, but defending here keeps the rule local.
        if (category == EmailCategory.TRANSACTIONAL || category == EmailCategory.SYSTEM) {
            return false;
        }
        return repository.findByEmailLower(normalise(email))
                .map(row -> row.getBlocked() != null && row.getBlocked().contains(category))
                .orElse(false);
    }

    /**
     * Add {@code category} to the suppression set for {@code email}. Creates a
     * row if none exists. Returns the persisted row.
     */
    public EmailSuppression suppress(String email, EmailCategory category, EmailSuppression.Reason reason) {
        String lower = normalise(email);
        Optional<EmailSuppression> existing = repository.findByEmailLower(lower);
        EmailSuppression row = existing.orElseGet(() -> {
            EmailSuppression r = new EmailSuppression();
            r.setEmailLower(lower);
            r.setBlocked(EnumSet.noneOf(EmailCategory.class));
            return r;
        });
        Set<EmailCategory> blocked = row.getBlocked() != null
                ? EnumSet.copyOf(row.getBlocked().isEmpty() ? EnumSet.noneOf(EmailCategory.class) : row.getBlocked())
                : EnumSet.noneOf(EmailCategory.class);
        blocked.add(category);
        row.setBlocked(blocked);
        row.setReason(reason);
        try {
            return repository.save(row);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            // Concurrent unsubscribe — re-read and union into whatever the other
            // writer landed.
            log.debug("Duplicate suppression write for {}; merging", lower);
            EmailSuppression latest = repository.findByEmailLower(lower)
                    .orElseThrow(() -> dup);
            Set<EmailCategory> merged = latest.getBlocked() != null
                    ? EnumSet.copyOf(latest.getBlocked().isEmpty()
                            ? EnumSet.noneOf(EmailCategory.class) : latest.getBlocked())
                    : EnumSet.noneOf(EmailCategory.class);
            merged.add(category);
            latest.setBlocked(merged);
            latest.setReason(reason);
            return repository.save(latest);
        }
    }

    static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
