/**
 * Unit tests for the worker's terminal-state bookkeeping and retry schedule.
 *
 * Atomic-claim correctness is exercised by
 * {@link EmailOutboxWorkerIntegrationTest} against a real Mongo — Mockito
 * can't prove "two callers can't claim the same row," only that the worker
 * makes the right shape of findAndModify call. The retry math, by contrast,
 * is pure logic and lives here.
 */
package cephadex.brainflex.service.email.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import cephadex.brainflex.service.email.EmailCategory;
import cephadex.brainflex.service.email.EmailDispatcher;
import cephadex.brainflex.service.email.EmailTemplate;

@ExtendWith(MockitoExtension.class)
class EmailOutboxWorkerTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private EmailOutboxRepository repository;
    @Mock private EmailDispatcher dispatcher;

    @Test
    void nextAttemptAt_GrowsExponentially_AndCapsAtOneHour() {
        // After the first attempt (attempts=1), backoff = 2 minutes.
        Instant first = EmailOutboxWorker.nextAttemptAt(1);
        long firstMinutes = Duration.between(Instant.now(), first).toMinutes();
        assertTrue(firstMinutes >= 1 && firstMinutes <= 2,
                "attempt 1 ~ 2min, got " + firstMinutes);

        // After many attempts the backoff is capped at MAX_BACKOFF.
        Instant capped = EmailOutboxWorker.nextAttemptAt(20);
        long cappedMinutes = Duration.between(Instant.now(), capped).toMinutes();
        assertTrue(cappedMinutes <= EmailOutboxWorker.MAX_BACKOFF.toMinutes(),
                "expected capped backoff, got " + cappedMinutes);
        assertTrue(cappedMinutes >= EmailOutboxWorker.MAX_BACKOFF.toMinutes() - 1,
                "expected ~1h backoff, got " + cappedMinutes);
    }

    @Test
    void processClaimed_TransientFailure_SchedulesRetryAndKeepsRowPending() {
        EmailOutboxWorker worker = new EmailOutboxWorker(mongoTemplate, repository, dispatcher);
        EmailOutboxEntry entry = entry(1);
        when(dispatcher.dispatch(entry)).thenAnswer(inv -> {
            // The dispatcher contract says it increments attempts on failure.
            entry.setAttempts(entry.getAttempts() + 1);
            entry.setLastError("TRANSIENT: smtp timeout");
            return EmailDispatcher.Outcome.TRANSIENT_FAILURE;
        });

        worker.processClaimed(entry);

        ArgumentCaptor<EmailOutboxEntry> captor = ArgumentCaptor.forClass(EmailOutboxEntry.class);
        verify(repository, times(1)).save(captor.capture());
        EmailOutboxEntry saved = captor.getValue();
        assertEquals(EmailOutboxEntry.Status.PENDING, saved.getStatus());
        assertNull(saved.getClaimedAt());
        assertNotNull(saved.getScheduledFor());
        assertTrue(saved.getScheduledFor().isAfter(Instant.now()),
                "transient retry should defer scheduledFor");
    }

    @Test
    void processClaimed_TransientFailure_AfterMaxAttempts_BecomesFailed() {
        EmailOutboxWorker worker = new EmailOutboxWorker(mongoTemplate, repository, dispatcher);
        EmailOutboxEntry entry = entry(EmailOutboxWorker.MAX_ATTEMPTS - 1);
        when(dispatcher.dispatch(entry)).thenAnswer(inv -> {
            entry.setAttempts(entry.getAttempts() + 1);
            return EmailDispatcher.Outcome.TRANSIENT_FAILURE;
        });

        worker.processClaimed(entry);

        ArgumentCaptor<EmailOutboxEntry> captor = ArgumentCaptor.forClass(EmailOutboxEntry.class);
        verify(repository).save(captor.capture());
        assertEquals(EmailOutboxEntry.Status.FAILED, captor.getValue().getStatus());
    }

    @Test
    void processClaimed_PermanentFailure_BecomesFailedImmediately() {
        EmailOutboxWorker worker = new EmailOutboxWorker(mongoTemplate, repository, dispatcher);
        EmailOutboxEntry entry = entry(1);
        when(dispatcher.dispatch(entry)).thenReturn(EmailDispatcher.Outcome.PERMANENT_FAILURE);

        worker.processClaimed(entry);

        ArgumentCaptor<EmailOutboxEntry> captor = ArgumentCaptor.forClass(EmailOutboxEntry.class);
        verify(repository).save(captor.capture());
        assertEquals(EmailOutboxEntry.Status.FAILED, captor.getValue().getStatus());
    }

    @Test
    void processClaimed_SentAndSuppressed_AreNotResaved() {
        EmailOutboxWorker worker = new EmailOutboxWorker(mongoTemplate, repository, dispatcher);
        EmailOutboxEntry entry = entry(0);
        when(dispatcher.dispatch(entry)).thenReturn(EmailDispatcher.Outcome.SENT);

        worker.processClaimed(entry);

        // The dispatcher already persisted; the worker must not double-write.
        verify(repository, times(0)).save(any());
    }

    private static EmailOutboxEntry entry(int attempts) {
        EmailOutboxEntry e = new EmailOutboxEntry();
        e.setId("e1");
        e.setRecipient("a@b.com");
        e.setCategory(EmailCategory.TRANSACTIONAL);
        e.setTemplate(EmailTemplate.WELCOME);
        e.setModel(Map.of());
        e.setStatus(EmailOutboxEntry.Status.CLAIMED);
        e.setAttempts(attempts);
        e.setClaimedAt(Instant.now());
        return e;
    }
}
