/**
 * Takes a single CLAIMED outbox entry, renders it, checks suppression, hands
 * it to the provider for the entry's category, and stamps the entry's
 * terminal state. The worker calls this once per row.
 *
 * Provider routing is configured via {@code brainflex.email.providers.*} —
 * one property per category points at an {@link EmailProvider#name()}. The
 * defaults fall through to whichever real provider is in the context
 * ({@code SmtpEmailProvider} if {@code spring.mail.host} is set, otherwise
 * {@code NoOpEmailProvider}). Tests can override the routing in
 * {@code application-test.properties} without recompiling.
 *
 * Retry policy lives in {@link cephadex.brainflex.service.email.outbox.EmailOutboxWorker},
 * not here — the dispatcher decides "this attempt succeeded / failed
 * transiently / failed permanently" and the worker translates that into a
 * scheduled re-attempt or a terminal FAILED.
 */
package cephadex.brainflex.service.email;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cephadex.brainflex.service.email.outbox.EmailOutboxEntry;
import cephadex.brainflex.service.email.outbox.EmailOutboxRepository;
import cephadex.brainflex.service.email.provider.EmailProvider;
import cephadex.brainflex.service.email.provider.EmailSendException;
import cephadex.brainflex.service.email.provider.NoOpEmailProvider;

@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    static final int LAST_ERROR_MAX = 512;

    public enum Outcome { SENT, SUPPRESSED, TRANSIENT_FAILURE, PERMANENT_FAILURE }

    private final EmailTemplateRenderer renderer;
    private final EmailSuppressionService suppression;
    private final EmailOutboxRepository outboxRepository;

    private final Map<String, EmailProvider> providersByName;
    private final Map<EmailCategory, EmailProvider> routing;

    public EmailDispatcher(
            EmailTemplateRenderer renderer,
            EmailSuppressionService suppression,
            EmailOutboxRepository outboxRepository,
            List<EmailProvider> providers,
            @Value("${brainflex.email.providers.transactional:}") String transactionalProvider,
            @Value("${brainflex.email.providers.system:}") String systemProvider,
            @Value("${brainflex.email.providers.marketing:}") String marketingProvider) {
        this.renderer = renderer;
        this.suppression = suppression;
        this.outboxRepository = outboxRepository;
        this.providersByName = new java.util.HashMap<>();
        for (EmailProvider p : providers) {
            providersByName.put(p.name(), p);
        }
        EmailProvider fallback = pickFallback(providers);
        this.routing = new EnumMap<>(EmailCategory.class);
        routing.put(EmailCategory.TRANSACTIONAL, resolveProvider(transactionalProvider, fallback));
        routing.put(EmailCategory.SYSTEM, resolveProvider(systemProvider, fallback));
        routing.put(EmailCategory.MARKETING, resolveProvider(marketingProvider, fallback));
        log.info("Email dispatcher initialised: {}", routing.entrySet().stream()
                .map(e -> e.getKey() + "→" + e.getValue().name())
                .toList());
    }

    /**
     * Dispatch one CLAIMED row to terminal state. Returns the outcome so the
     * worker can decide whether to schedule a retry.
     */
    public Outcome dispatch(EmailOutboxEntry entry) {
        EmailCategory category = entry.getCategory();
        if (suppression.isSuppressed(entry.getRecipient(), category)) {
            entry.setStatus(EmailOutboxEntry.Status.SUPPRESSED);
            entry.setLastError(null);
            entry.setSentAt(null);
            outboxRepository.save(entry);
            return Outcome.SUPPRESSED;
        }

        RenderedEmail rendered;
        try {
            rendered = renderer.render(entry.getRecipient(), entry.getTemplate(), entry.getModel());
        } catch (Exception e) {
            // A render failure is almost always a bug in the model/template —
            // permanent until somebody fixes it.
            log.warn("Render failed for outbox {} ({}): {}", entry.getId(), entry.getTemplate(), e.getMessage());
            markFailure(entry, EmailSendException.Kind.PERMANENT, "render: " + safeMessage(e));
            return Outcome.PERMANENT_FAILURE;
        }

        EmailProvider provider = routing.get(category);
        try {
            provider.send(rendered);
            entry.setStatus(EmailOutboxEntry.Status.SENT);
            entry.setLastError(null);
            entry.setSentAt(Instant.now());
            entry.setAttempts(entry.getAttempts() + 1);
            outboxRepository.save(entry);
            return Outcome.SENT;
        } catch (EmailSendException e) {
            log.warn("Send failed for outbox {} via {}: {} ({})",
                    entry.getId(), provider.name(), e.getMessage(), e.kind());
            markFailure(entry, e.kind(), e.getMessage());
            return e.kind() == EmailSendException.Kind.PERMANENT
                    ? Outcome.PERMANENT_FAILURE
                    : Outcome.TRANSIENT_FAILURE;
        }
    }

    /**
     * Updates the entry's bookkeeping after a failure. Does NOT persist —
     * the worker writes after computing the next scheduledFor so we only
     * touch the row once.
     */
    void markFailure(EmailOutboxEntry entry, EmailSendException.Kind kind, String message) {
        entry.setAttempts(entry.getAttempts() + 1);
        entry.setLastError(truncate(kind.name() + ": " + (message == null ? "" : message)));
        // Status is set by the worker based on attempts/kind — leave as CLAIMED
        // here so the worker can decide retry vs. terminal.
    }

    private EmailProvider resolveProvider(String configured, EmailProvider fallback) {
        if (configured == null || configured.isBlank()) return fallback;
        EmailProvider match = providersByName.get(configured.toLowerCase(Locale.ROOT));
        if (match == null) {
            log.warn("Email provider '{}' not in context; falling back to {}", configured, fallback.name());
            return fallback;
        }
        return match;
    }

    private static EmailProvider pickFallback(List<EmailProvider> providers) {
        // Prefer a non-NoOp implementation if one is registered.
        for (EmailProvider p : providers) {
            if (!NoOpEmailProvider.NAME.equals(p.name())) return p;
        }
        // Otherwise NoOp is the only one — tests / local-dev path.
        return providers.stream()
                .filter(p -> NoOpEmailProvider.NAME.equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No EmailProvider beans registered — at minimum NoOpEmailProvider is required"));
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }

    public static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= LAST_ERROR_MAX ? s : s.substring(0, LAST_ERROR_MAX);
    }
}
