/**
 * SMTP implementation of {@link EmailProvider}. Lifted from the v0
 * {@code SmtpEmailService}: configured via Spring Boot's {@code spring.mail.*}
 * properties, renders the MIME message, hands it to {@link JavaMailSender}.
 *
 * Activated only when {@code spring.mail.host} is configured. Tests and local
 * dev without an SMTP relay fall through to {@link NoOpEmailProvider}.
 *
 * Errors are classified into {@link EmailSendException.Kind} so the
 * dispatcher's retry policy can distinguish a transient timeout from a bad
 * recipient. The classification is heuristic — Jakarta Mail's exception
 * hierarchy is shallow and the same {@link jakarta.mail.SendFailedException}
 * is thrown for both 4xx and 5xx — so unrecognised errors default to
 * TRANSIENT (errs on the side of retrying).
 */
package cephadex.brainflex.service.email.provider;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import cephadex.brainflex.service.email.RenderedEmail;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;

@Component
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailProvider implements EmailProvider {

    public static final String NAME = "smtp";

    private final JavaMailSender mailSender;

    @Value("${brainflex.email.from:no-reply@brainflex.local}")
    private String fromAddress;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String name() { return NAME; }

    @Override
    public void send(RenderedEmail email) throws EmailSendException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email.recipient());
            helper.setSubject(email.subject());
            helper.setText(email.htmlBody(), true);
            mailSender.send(message);
        } catch (MailParseException e) {
            throw new EmailSendException(EmailSendException.Kind.PERMANENT,
                    "Mail parse failed: " + e.getMessage(), e);
        } catch (MailAuthenticationException e) {
            // Misconfigured credentials are not the user's fault — retry until
            // the operator fixes it.
            throw new EmailSendException(EmailSendException.Kind.TRANSIENT,
                    "SMTP auth failed: " + e.getMessage(), e);
        } catch (MailSendException e) {
            EmailSendException.Kind kind = classify(e);
            throw new EmailSendException(kind, "SMTP send failed: " + e.getMessage(), e);
        } catch (MailException e) {
            throw new EmailSendException(EmailSendException.Kind.TRANSIENT,
                    "SMTP error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailSendException(EmailSendException.Kind.TRANSIENT,
                    "Unexpected SMTP failure: " + e.getMessage(), e);
        }
    }

    private static EmailSendException.Kind classify(MailSendException e) {
        Throwable cause = e.getCause();
        // Invalid recipient address — permanent.
        if (cause instanceof AddressException) return EmailSendException.Kind.PERMANENT;
        String msg = (e.getMessage() == null ? "" : e.getMessage()).toLowerCase();
        // 5xx status codes from the SMTP server are permanent rejections.
        if (msg.contains(" 550") || msg.contains(" 551") || msg.contains(" 552")
                || msg.contains(" 553") || msg.contains(" 554")) {
            return EmailSendException.Kind.PERMANENT;
        }
        return EmailSendException.Kind.TRANSIENT;
    }
}
