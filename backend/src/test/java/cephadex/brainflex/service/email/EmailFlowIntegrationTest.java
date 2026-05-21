/**
 * End-to-end integration test that drives the full chain: caller →
 * {@link EmailService#enqueue(EmailJob)} → outbox row → worker claim →
 * dispatcher → {@link CapturingEmailProvider} → terminal SENT.
 *
 * Doubles as the atomic-claim correctness test. Two parallel claim attempts
 * against the same PENDING row must not both succeed — the second returns
 * null. The findAndModify in {@link EmailOutboxWorker#claimOne()} guarantees
 * this at the database level; this test pins it down.
 *
 * Runs against the real Mongo at localhost:27017 (configured in
 * application-test.properties) — same pattern as the other @SpringBootTest
 * suites in this codebase. Each test cleans the email_outbox collection on
 * entry so reruns are deterministic.
 */
package cephadex.brainflex.service.email;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import cephadex.brainflex.service.email.outbox.EmailOutboxEntry;
import cephadex.brainflex.service.email.outbox.EmailOutboxRepository;
import cephadex.brainflex.service.email.outbox.EmailOutboxWorker;
import cephadex.brainflex.service.email.provider.EmailProvider;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmailFlowIntegrationTest.TestBeans.class)
// Disable the @Scheduled worker so this test drives ticks manually — keeps the
// observed send count deterministic instead of racing the poll loop.
@TestPropertySource(properties = {
        "brainflex.email.worker.enabled=false",
        "brainflex.email.providers.transactional=capturing",
        "brainflex.email.providers.system=capturing",
        "brainflex.email.providers.marketing=capturing"
})
class EmailFlowIntegrationTest {

    @Autowired private EmailService emailService;
    @Autowired private EmailOutboxWorker worker;
    @Autowired private EmailOutboxRepository repository;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private CapturingEmailProvider capturing;

    @BeforeEach
    void resetCollection() {
        mongoTemplate.dropCollection(EmailOutboxEntry.class);
        capturing.reset();
    }

    @Test
    void enqueueThenTick_RendersAndSendsViaProvider_AndMarksSent() {
        String id = emailService.enqueue(EmailJob.builder()
                .recipient("alice@example.com")
                .template(EmailTemplate.WELCOME)
                .modelEntry("displayName", "Alice")
                .build());
        assertNotNull(id);

        worker.tick();

        EmailOutboxEntry persisted = repository.findById(id).orElseThrow();
        assertEquals(EmailOutboxEntry.Status.SENT, persisted.getStatus());
        assertEquals(1, capturing.sent.size());
        assertEquals("alice@example.com", capturing.sent.get(0).recipient());
        assertEquals("Welcome to BrainFlex", capturing.sent.get(0).subject());
        assertTrue(capturing.sent.get(0).htmlBody().contains("Alice"));
    }

    @Test
    void claimOne_IsAtomic_TwoCallersCannotBothClaimTheSameRow() throws Exception {
        // Insert one PENDING row and try to claim from two threads at once.
        emailService.enqueue(EmailJob.builder()
                .recipient("a@b.com")
                .template(EmailTemplate.WELCOME)
                .modelEntry("displayName", "A")
                .build());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<EmailOutboxEntry> claim = worker::claimOne;
            Future<EmailOutboxEntry> f1 = pool.submit(claim);
            Future<EmailOutboxEntry> f2 = pool.submit(claim);
            EmailOutboxEntry c1 = f1.get();
            EmailOutboxEntry c2 = f2.get();

            int nonNull = (c1 != null ? 1 : 0) + (c2 != null ? 1 : 0);
            assertEquals(1, nonNull, "exactly one caller must have claimed the row");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void scheduledFor_InTheFuture_IsNotClaimedYet() {
        EmailOutboxEntry entry = new EmailOutboxEntry();
        entry.setRecipient("future@example.com");
        entry.setCategory(EmailCategory.TRANSACTIONAL);
        entry.setTemplate(EmailTemplate.WELCOME);
        entry.setModel(Map.of("displayName", "Future"));
        entry.setStatus(EmailOutboxEntry.Status.PENDING);
        entry.setScheduledFor(Instant.now().plus(Duration.ofMinutes(10)));
        repository.save(entry);

        assertEquals(null, worker.claimOne());
        EmailOutboxEntry persisted = repository.findById(entry.getId()).orElseThrow();
        assertEquals(EmailOutboxEntry.Status.PENDING, persisted.getStatus());
    }

    /**
     * Registers an in-process capturing provider so the test can inspect the
     * rendered bytes without standing up an SMTP relay. The dispatcher picks
     * this provider via the {@code brainflex.email.providers.*} overrides
     * supplied by {@link TestPropertySource}.
     */
    @TestConfiguration
    static class TestBeans {

        @Bean
        CapturingEmailProvider capturingEmailProvider() {
            return new CapturingEmailProvider();
        }
    }

    /** Visible at the package level so {@link TestBeans} can hand it back. */
    static class CapturingEmailProvider implements EmailProvider {

        final List<RenderedEmail> sent = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override public String name() { return "capturing"; }

        @Override public void send(RenderedEmail email) {
            sent.add(email);
        }

        void reset() { sent.clear(); }
    }
}
