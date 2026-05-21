/**
 * The output of {@link EmailTemplateRenderer}: everything a provider needs to
 * actually transmit, with no Thymeleaf or template-engine concepts left in
 * the type. Providers (SMTP, SES, ...) consume this record and nothing else.
 */
package cephadex.brainflex.service.email;

public record RenderedEmail(
        String recipient,
        String subject,
        String htmlBody) {
}
