/**
 * Default {@link EmailService}. Writes a PENDING row to the outbox and
 * returns. Every call site goes through this; the worker / dispatcher /
 * provider chain is invisible to callers.
 *
 * The contract — never throws on provider failure — is upheld here by
 * limiting the work to a single Mongo insert. If Mongo itself is down the
 * caller will get the propagated exception, which is the right behaviour:
 * we'd rather surface a request-time error than silently lose mail.
 */
package cephadex.brainflex.service.email;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import cephadex.brainflex.service.email.outbox.EmailOutboxEntry;
import cephadex.brainflex.service.email.outbox.EmailOutboxRepository;

@Service
public class OutboxEmailService implements EmailService {

    private final EmailOutboxRepository repository;

    public OutboxEmailService(EmailOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public String enqueue(EmailJob job) {
        EmailOutboxEntry entry = new EmailOutboxEntry();
        entry.setRecipient(normalise(job.recipient()));
        entry.setUserId(job.userId());
        entry.setCategory(job.effectiveCategory());
        entry.setTemplate(job.template());
        entry.setModel(job.model() != null ? new HashMap<>(job.model()) : new HashMap<>());
        entry.setStatus(EmailOutboxEntry.Status.PENDING);
        Instant scheduledFor = job.scheduledFor() != null ? job.scheduledFor() : entry.getCreatedAt();
        entry.setScheduledFor(scheduledFor);
        return repository.save(entry).getId();
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
