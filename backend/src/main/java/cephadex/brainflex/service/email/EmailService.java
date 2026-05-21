/**
 * The only email surface call sites should depend on. Hands an {@link EmailJob}
 * to the outbox; never blocks on a provider; never throws on send failure.
 *
 * Implementation in this package writes a PENDING row to Mongo and returns
 * immediately. {@link cephadex.brainflex.service.email.outbox.EmailOutboxWorker}
 * picks the row up, renders it, dispatches it via the configured
 * {@link cephadex.brainflex.service.email.provider.EmailProvider}, and updates
 * the row's terminal state.
 *
 * Why an interface here when there is only one implementation: tests can swap
 * a capturing fake without dragging in Mongo, and the doc's planned SQS-fronted
 * variant (Option A in the feature doc) will be a second impl that publishes
 * to SQS after the Mongo insert.
 */
package cephadex.brainflex.service.email;

public interface EmailService {

    /**
     * Enqueue a job for asynchronous dispatch. Returns the outbox row id.
     * Never throws on provider failure — see the worker for retry semantics.
     */
    String enqueue(EmailJob job);

    /** Convenience: enqueue and ignore the id. */
    default void send(EmailJob job) {
        enqueue(job);
    }
}
