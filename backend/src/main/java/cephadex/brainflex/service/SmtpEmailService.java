/**
 * SMTP implementation of {@link EmailService}. Uses Spring Boot's
 * `JavaMailSender` (configured via `spring.mail.*` properties / env vars) and
 * renders bodies through Thymeleaf templates under
 * `src/main/resources/templates/email/`.
 *
 * Activated whenever `spring.mail.host` is set; falls back to {@link
 * NoOpEmailService} when it isn't (tests, local dev without an SMTP relay).
 * Failures are logged but never thrown — outbound mail must not crash the
 * scheduler or the cancel flow.
 */
package cephadex.brainflex.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.ScheduledInteractiveSession;
import jakarta.mail.internet.MimeMessage;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    // ISO-style display: "2026-05-20 14:00 UTC". Times are always rendered UTC;
    // chunk 20 will introduce per-user timezone for localised display.
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${brainflex.email.from:no-reply@brainflex.local}")
    private String fromAddress;

    @Value("${brainflex.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public SmtpEmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendInitialInvite(ScheduledInteractiveSession schedule,
                                  InteractiveSessionInvite invite,
                                  String hostName,
                                  String deckName) {
        Map<String, Object> model = baseModel(hostName, deckName);
        model.put("scheduledFor", schedule.getScheduledStartAt().format(DISPLAY));
        model.put("acceptUrl", frontendBaseUrl + "/invite/" + invite.getInviteToken());
        send(invite.getEmail(),
                hostName + " invited you to play " + deckName,
                renderHtml("email/initial-invite", model));
    }

    @Override
    public void sendBootReminder(ScheduledInteractiveSession schedule,
                                 InteractiveSessionInvite invite,
                                 String hostName,
                                 String deckName,
                                 String roomCode) {
        Map<String, Object> model = baseModel(hostName, deckName);
        model.put("roomCode", roomCode);
        model.put("joinUrl", frontendBaseUrl + "/invite/" + invite.getInviteToken());
        model.put("customReminder", schedule.getReminderEmailTemplate());
        send(invite.getEmail(),
                deckName + " is starting now",
                renderHtml("email/boot-reminder", model));
    }

    @Override
    public void sendCancelNotice(ScheduledInteractiveSession schedule,
                                 List<InteractiveSessionInvite> invites,
                                 String hostName,
                                 String deckName) {
        Map<String, Object> model = baseModel(hostName, deckName);
        model.put("scheduledFor", schedule.getScheduledStartAt().format(DISPLAY));
        String body = renderHtml("email/cancel-notice", model);
        for (InteractiveSessionInvite invite : invites) {
            send(invite.getEmail(), hostName + " cancelled " + deckName, body);
        }
    }

    private Map<String, Object> baseModel(String hostName, String deckName) {
        Map<String, Object> m = new HashMap<>();
        m.put("hostName", hostName);
        m.put("deckName", deckName);
        m.put("frontendBaseUrl", frontendBaseUrl);
        return m;
    }

    private String renderHtml(String templateName, Map<String, Object> model) {
        Context ctx = new Context();
        ctx.setVariables(model);
        return templateEngine.process(templateName, ctx);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            // SMTP outage shouldn't bring down the scheduler. Log and move on;
            // the host can resend via the explicit endpoint.
            log.warn("Email send failed: to={}, subject={}, cause={}", to, subject, e.getMessage());
        }
    }
}
