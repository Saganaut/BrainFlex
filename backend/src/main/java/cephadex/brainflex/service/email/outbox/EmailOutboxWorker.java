/**
 * Polls the {@code email_outbox} collection on a fixed cadence, atomically
 * claims a batch of PENDING rows whose {@code scheduledFor} has arrived, and
 * hands each to {@link EmailDispatcher} for terminal processing.
 *
 * Atomic claim semantics:
 *   - {@link #claimOne()} uses {@code findAndModify} to flip a single row
 *     from PENDING to CLAIMED in one operation. Two workers (or two replicas)
 *     can never grab the same row — the modifier wins, the other gets the
 *     next match.
 *   - {@link #reclaimStuck()} flips CLAIMED rows whose {@code claimedAt} is
 *     older than {@link #CLAIM_LEASE} back to PENDING. This is the safety net
 *     for "worker crashed mid-dispatch" — without it, those rows would sit
 *     CLAIMED forever.
 *
 * Retry policy (transient failures): exponential backoff capped at one hour,
 * up to {@link #MAX_ATTEMPTS} total attempts before the row goes FAILED.
 * Permanent failures (bad recipient, 5xx, render bug) skip retry and go
 * FAILED immediately.
 *
 * Why polling and not a Mongo change stream: the change-stream variant is the
 * SQS swap (see feature doc). Polling is fine for v1 — Mongo handles the
 * query cheaply via the {@code (status, scheduledFor)} index, and the worker
 * is the *one* place we'll replace with an SQS consumer when we deploy.
 */
package cephadex.brainflex.service.email.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cephadex.brainflex.service.email.EmailDispatcher;

@Component
public class EmailOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxWorker.class);

    /** A CLAIMED row older than this is presumed dead and reclaimed. */
    static final Duration CLAIM_LEASE = Duration.ofMinutes(5);

    /** Hard ceiling on attempt count before transient failures go FAILED. */
    static final int MAX_ATTEMPTS = 5;

    /** Backoff cap — never wait longer than this between retries. */
    static final Duration MAX_BACKOFF = Duration.ofHours(1);

    /** Per-tick safety valve so a backed-up queue can't hog the worker thread. */
    static final int MAX_PER_TICK = 50;

    private final MongoTemplate mongoTemplate;
    private final EmailOutboxRepository repository;
    private final EmailDispatcher dispatcher;

    @Value("${brainflex.email.worker.enabled:true}")
    private boolean enabled;

    public EmailOutboxWorker(MongoTemplate mongoTemplate,
                             EmailOutboxRepository repository,
                             EmailDispatcher dispatcher) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${brainflex.email.worker.poll-interval-ms:2000}",
               initialDelayString = "${brainflex.email.worker.initial-delay-ms:5000}")
    public void scheduledTick() {
        if (!enabled) return;
        tick();
    }

    /**
     * Drains the outbox once. Public so integration tests can drive the
     * worker explicitly instead of racing the scheduler. The
     * {@code enabled} flag gates only the scheduled invocation — explicit
     * calls always run.
     */
    public void tick() {
        try {
            reclaimStuck();
            int dispatched = 0;
            while (dispatched < MAX_PER_TICK) {
                EmailOutboxEntry claimed = claimOne();
                if (claimed == null) break;
                processClaimed(claimed);
                dispatched++;
            }
        } catch (Exception e) {
            // The poll loop must never die — log and let the next tick retry.
            log.error("Outbox worker tick failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Atomically claim a single PENDING row whose scheduledFor has arrived.
     * Returns null when the queue is empty. Public so integration tests can
     * exercise the atomic-claim contract directly.
     */
    public EmailOutboxEntry claimOne() {
        Instant now = Instant.now();
        Query q = new Query(new Criteria().andOperator(
                Criteria.where("status").is(EmailOutboxEntry.Status.PENDING),
                new Criteria().orOperator(
                        Criteria.where("scheduledFor").lte(now),
                        Criteria.where("scheduledFor").is(null))))
                .with(Sort.by(Sort.Direction.ASC, "scheduledFor", "createdAt"));
        Update update = new Update()
                .set("status", EmailOutboxEntry.Status.CLAIMED)
                .set("claimedAt", now);
        FindAndModifyOptions opts = FindAndModifyOptions.options().returnNew(true);
        return mongoTemplate.findAndModify(q, update, opts, EmailOutboxEntry.class);
    }

    /**
     * Flip CLAIMED rows older than the lease back to PENDING. Bounded by a
     * findAndModify per row so we don't blast all of them in one bulk update —
     * keeps the retry traffic spread out across ticks.
     */
    void reclaimStuck() {
        Instant cutoff = Instant.now().minus(CLAIM_LEASE);
        List<EmailOutboxEntry> stuck = repository.findByStatusAndClaimedAtBefore(
                EmailOutboxEntry.Status.CLAIMED, cutoff);
        for (EmailOutboxEntry entry : stuck) {
            log.warn("Reclaiming stuck outbox {} (claimed at {})", entry.getId(), entry.getClaimedAt());
            Query q = new Query(Criteria.where("_id").is(entry.getId())
                    .and("status").is(EmailOutboxEntry.Status.CLAIMED));
            Update u = new Update()
                    .set("status", EmailOutboxEntry.Status.PENDING)
                    .unset("claimedAt");
            mongoTemplate.updateFirst(q, u, EmailOutboxEntry.class);
        }
    }

    void processClaimed(EmailOutboxEntry entry) {
        EmailDispatcher.Outcome outcome;
        try {
            outcome = dispatcher.dispatch(entry);
        } catch (Exception e) {
            // Should not happen — dispatcher swallows provider exceptions — but
            // we don't want a programming bug to leak the row in CLAIMED.
            log.error("Dispatcher threw for outbox {}: {}", entry.getId(), e.getMessage(), e);
            entry.setLastError(EmailDispatcher.truncate("worker: " + e.getMessage()));
            entry.setStatus(EmailOutboxEntry.Status.FAILED);
            repository.save(entry);
            return;
        }

        switch (outcome) {
            case SENT, SUPPRESSED -> { /* dispatcher already persisted */ }
            case PERMANENT_FAILURE -> {
                entry.setStatus(EmailOutboxEntry.Status.FAILED);
                entry.setClaimedAt(null);
                repository.save(entry);
            }
            case TRANSIENT_FAILURE -> {
                if (entry.getAttempts() >= MAX_ATTEMPTS) {
                    entry.setStatus(EmailOutboxEntry.Status.FAILED);
                    entry.setClaimedAt(null);
                    repository.save(entry);
                } else {
                    entry.setStatus(EmailOutboxEntry.Status.PENDING);
                    entry.setClaimedAt(null);
                    entry.setScheduledFor(nextAttemptAt(entry.getAttempts()));
                    repository.save(entry);
                }
            }
        }
    }

    /**
     * Exponential backoff: 2^n minutes after the n-th attempt, capped at
     * {@link #MAX_BACKOFF}. {@code attempts} is post-increment, so the first
     * retry (after the initial failed attempt) waits 2 minutes.
     */
    static Instant nextAttemptAt(int attempts) {
        long minutes = 1L << Math.min(attempts, 6); // 1<<6 = 64, well past MAX_BACKOFF
        Duration backoff = Duration.ofMinutes(minutes);
        if (backoff.compareTo(MAX_BACKOFF) > 0) backoff = MAX_BACKOFF;
        return Instant.now().plus(backoff);
    }
}
