/**
 * Unit tests for the dispatcher's suppression and provider-routing behaviour.
 *
 * The dispatcher is the place where the "TRANSACTIONAL / SYSTEM ignore
 * suppression, MARKETING honours it" rule lives. These tests pin that rule
 * down so a refactor can't accidentally drop a password reset because a row
 * with the user's email landed in the suppression list.
 */
package cephadex.brainflex.service.email;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import cephadex.brainflex.service.email.outbox.EmailOutboxEntry;
import cephadex.brainflex.service.email.outbox.EmailOutboxRepository;
import cephadex.brainflex.service.email.provider.EmailProvider;
import cephadex.brainflex.service.email.provider.EmailSendException;
import cephadex.brainflex.service.email.provider.NoOpEmailProvider;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock
    private EmailTemplateRenderer renderer;
    @Mock
    private EmailSuppressionService suppression;
    @Mock
    private EmailOutboxRepository outboxRepository;

    private EmailProvider realProvider;
    private NoOpEmailProvider noop;
    private EmailDispatcher dispatcher;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        realProvider = new RecordingProvider("smtp-fake");
        noop = new NoOpEmailProvider();
        dispatcher = new EmailDispatcher(renderer, suppression, outboxRepository,
                List.of(realProvider, noop), "", "", "");
        // Lenient because suppressed-marketing tests short-circuit before render.
        lenient().when(renderer.render(any(), any(), any()))
                .thenAnswer(inv -> new RenderedEmail(
                        inv.getArgument(0), "subject", "<p>body</p>"));
    }

    @Test
    void marketing_IsSuppressedAndDoesNotHitProvider() {
        EmailOutboxEntry entry = entry("a@b.com", EmailCategory.MARKETING, EmailTemplate.MARKETING_ANNOUNCE);
        when(suppression.isSuppressed("a@b.com", EmailCategory.MARKETING)).thenReturn(true);

        EmailDispatcher.Outcome outcome = dispatcher.dispatch(entry);

        assertEquals(EmailDispatcher.Outcome.SUPPRESSED, outcome);
        assertEquals(EmailOutboxEntry.Status.SUPPRESSED, entry.getStatus());
        verify(outboxRepository, times(1)).save(entry);
        assertEquals(0, ((RecordingProvider) realProvider).sent);
    }

    @Test
    void transactional_IgnoresSuppressionList() {
        EmailOutboxEntry entry = entry("a@b.com", EmailCategory.TRANSACTIONAL, EmailTemplate.WELCOME);
        // Even if the suppression service was consulted with MARKETING it would
        // say yes — the dispatcher must not even ask for TRANSACTIONAL.
        when(suppression.isSuppressed(eq("a@b.com"), eq(EmailCategory.TRANSACTIONAL))).thenReturn(false);

        EmailDispatcher.Outcome outcome = dispatcher.dispatch(entry);

        assertEquals(EmailDispatcher.Outcome.SENT, outcome);
        assertEquals(EmailOutboxEntry.Status.SENT, entry.getStatus());
        assertNotNull(entry.getSentAt());
        assertEquals(1, ((RecordingProvider) realProvider).sent);
    }

    @Test
    void system_IgnoresSuppressionList() {
        // Even if a row landed in the suppression list for SYSTEM, the
        // dispatcher must still send — account-close confirmations and admin
        // alerts are not opt-out.
        when(suppression.isSuppressed("a@b.com", EmailCategory.SYSTEM)).thenReturn(false);
        EmailOutboxEntry entry = entry("a@b.com", EmailCategory.SYSTEM, EmailTemplate.ACCOUNT_CLOSED);

        EmailDispatcher.Outcome outcome = dispatcher.dispatch(entry);

        assertEquals(EmailDispatcher.Outcome.SENT, outcome);
        assertEquals(EmailOutboxEntry.Status.SENT, entry.getStatus());
    }

    @Test
    void permanentFailure_IsClassifiedNotRetried() {
        RecordingProvider permanent = new RecordingProvider("smtp-fake");
        permanent.failWith = new EmailSendException(
                EmailSendException.Kind.PERMANENT, "bad recipient");
        dispatcher = new EmailDispatcher(renderer, suppression, outboxRepository,
                List.of(permanent, noop), "", "", "");

        EmailOutboxEntry entry = entry("a@b.com", EmailCategory.TRANSACTIONAL, EmailTemplate.WELCOME);

        EmailDispatcher.Outcome outcome = dispatcher.dispatch(entry);

        assertEquals(EmailDispatcher.Outcome.PERMANENT_FAILURE, outcome);
        assertEquals(1, entry.getAttempts());
        assertNotNull(entry.getLastError());
    }

    @Test
    void renderFailure_IsPermanent() {
        when(renderer.render(any(), any(), any())).thenThrow(new IllegalStateException("missing var"));
        EmailOutboxEntry entry = entry("a@b.com", EmailCategory.TRANSACTIONAL, EmailTemplate.WELCOME);

        EmailDispatcher.Outcome outcome = dispatcher.dispatch(entry);

        assertEquals(EmailDispatcher.Outcome.PERMANENT_FAILURE, outcome);
        assertNotNull(entry.getLastError());
    }

    // ---- helpers ----

    private static EmailOutboxEntry entry(String recipient, EmailCategory category, EmailTemplate template) {
        EmailOutboxEntry entry = new EmailOutboxEntry();
        entry.setId("e1");
        entry.setRecipient(recipient);
        entry.setCategory(category);
        entry.setTemplate(template);
        entry.setModel(Map.of());
        entry.setStatus(EmailOutboxEntry.Status.CLAIMED);
        return entry;
    }

    /** Tracks sends and optionally fails — beats Mockito for this many calls. */
    private static final class RecordingProvider implements EmailProvider {
        private final String name;
        int sent;
        EmailSendException failWith;

        RecordingProvider(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void send(RenderedEmail email) throws EmailSendException {
            if (failWith != null)
                throw failWith;
            sent++;
        }
    }
}
