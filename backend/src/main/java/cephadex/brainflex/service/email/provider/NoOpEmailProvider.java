/**
 * Fallback provider used when no real provider bean is configured. Logs the
 * recipient + subject at DEBUG so local-dev / test runs don't spam the
 * console, and lets the outbox flow run end-to-end without an SMTP relay.
 *
 * Registered as the {@code "noop"} provider so the dispatcher's category →
 * provider routing always has something to fall back to.
 */
package cephadex.brainflex.service.email.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cephadex.brainflex.service.email.RenderedEmail;

@Component
public class NoOpEmailProvider implements EmailProvider {

    public static final String NAME = "noop";

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailProvider.class);

    @Override
    public String name() { return NAME; }

    @Override
    public void send(RenderedEmail email) {
        log.debug("NoOp email send: to={}, subject={}", email.recipient(), email.subject());
    }
}
