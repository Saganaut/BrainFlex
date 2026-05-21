/**
 * Single-method gateway to whatever actually transmits the bytes. SMTP today,
 * SES tomorrow, SendGrid for marketing if we split it.
 *
 * Implementations identify themselves via {@link #name()} so the dispatcher
 * can wire categories to specific providers by name (configured via
 * properties), without depending on the concrete class.
 *
 * Contract: {@link #send(RenderedEmail)} is synchronous from the worker's
 * point of view — the worker is already running off-thread. Throwing here
 * signals a transient or permanent failure; the dispatcher decides which.
 */
package cephadex.brainflex.service.email.provider;

import cephadex.brainflex.service.email.RenderedEmail;

public interface EmailProvider {

    /** Stable identifier — referenced from properties to route categories. */
    String name();

    /**
     * Transmit the rendered email. Throw on any failure; the dispatcher will
     * classify and retry per {@link EmailSendException.Kind}.
     */
    void send(RenderedEmail email) throws EmailSendException;
}
