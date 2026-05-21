/**
 * Thrown by an {@link EmailProvider} to communicate a send failure plus its
 * retry-ability. The dispatcher reads {@link Kind} to choose between an
 * immediate FAILED terminal and a backoff retry.
 *
 *   - TRANSIENT — SMTP timeout, 4xx provider, network blip. Worth retrying.
 *   - PERMANENT — 5xx provider, invalid recipient. Don't retry; the bounce
 *                 listener may also add the address to the suppression list
 *                 when SES webhooks land.
 */
package cephadex.brainflex.service.email.provider;

public class EmailSendException extends Exception {

    public enum Kind { TRANSIENT, PERMANENT }

    private final Kind kind;

    public EmailSendException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public EmailSendException(Kind kind, String message) {
        this(kind, message, null);
    }

    public Kind kind() {
        return kind;
    }
}
