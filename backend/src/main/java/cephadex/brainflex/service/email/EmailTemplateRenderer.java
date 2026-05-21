/**
 * Resolves an {@link EmailTemplate} + Thymeleaf model into a fully-rendered
 * subject + HTML body. The mapping from enum to template filename and subject
 * pattern lives here so adding a template touches one switch in addition to
 * the enum entry.
 *
 * Subjects are intentionally rendered through Thymeleaf as well — the subject
 * pattern uses {@code ${var}} placeholders that resolve against the same model
 * the body sees, so a caller never has to pre-format two strings.
 */
package cephadex.brainflex.service.email;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class EmailTemplateRenderer {

    /** Matches {@code ${name}} placeholders in a subject pattern. */
    private static final Pattern SUBJECT_VAR = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    private final SpringTemplateEngine bodyEngine;
    private final UnsubscribeTokenService unsubscribeTokens;

    @Value("${brainflex.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public EmailTemplateRenderer(SpringTemplateEngine bodyEngine,
                                 UnsubscribeTokenService unsubscribeTokens) {
        this.bodyEngine = bodyEngine;
        this.unsubscribeTokens = unsubscribeTokens;
    }

    public RenderedEmail render(String recipient, EmailTemplate template, Map<String, Object> model) {
        Map<String, Object> merged = new HashMap<>();
        merged.put("frontendBaseUrl", frontendBaseUrl);
        if (model != null) merged.putAll(model);
        // Marketing templates always need an unsubscribe token; inject one
        // here so call sites don't have to know about the token format and
        // we can't accidentally ship a marketing email without a footer link.
        if (template.defaultCategory() == EmailCategory.MARKETING
                && !merged.containsKey("unsubscribeToken")) {
            merged.put("unsubscribeToken", unsubscribeTokens.mint(recipient, EmailCategory.MARKETING));
        }

        String bodyTemplate = bodyTemplateName(template);
        String subjectPattern = subjectPattern(template);

        Context ctx = new Context();
        ctx.setVariables(merged);

        String html = bodyEngine.process(bodyTemplate, ctx);
        String subject = renderSubject(subjectPattern, merged);
        return new RenderedEmail(recipient, subject, html);
    }

    static String renderSubject(String pattern, Map<String, Object> model) {
        Matcher m = SUBJECT_VAR.matcher(pattern);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            Object value = model.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String bodyTemplateName(EmailTemplate template) {
        return switch (template) {
            case WELCOME            -> "email/welcome";
            case ACCOUNT_CLOSED     -> "email/account-closed";
            case PASSWORD_RESET     -> "email/password-reset";
            case INVITE_INITIAL     -> "email/initial-invite";
            case INVITE_REMINDER    -> "email/boot-reminder";
            case INVITE_CANCEL      -> "email/cancel-notice";
            case MARKETING_ANNOUNCE -> "email/marketing-announce";
        };
    }

    private static String subjectPattern(EmailTemplate template) {
        return switch (template) {
            case WELCOME            -> "Welcome to BrainFlex";
            case ACCOUNT_CLOSED     -> "Your BrainFlex account has been closed";
            case PASSWORD_RESET     -> "Reset your BrainFlex password";
            case INVITE_INITIAL     -> "${hostName} invited you to play ${deckName}";
            case INVITE_REMINDER    -> "${deckName} is starting now";
            case INVITE_CANCEL      -> "${hostName} cancelled ${deckName}";
            case MARKETING_ANNOUNCE -> "${subjectLine}";
        };
    }
}
