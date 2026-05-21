/**
 * The set of renderable email templates. Each value maps to a Thymeleaf
 * template name and an inline subject pattern via
 * {@link EmailTemplateRenderer}; the enum identity is what callers and the
 * outbox row store.
 *
 * Adding a new email type is a three-step change here: add an enum value,
 * pair it with template + subject in the renderer, drop the .html under
 * {@code src/main/resources/templates/email/}. Call sites grow no surface.
 *
 * The category encoded on each value is the *default* for an enqueued job;
 * callers can still override on {@link EmailJob}. The default exists so a
 * "we forgot to set category" mistake doesn't accidentally tag a password
 * reset as MARKETING (and get it dropped by suppression).
 */
package cephadex.brainflex.service.email;

public enum EmailTemplate {
    WELCOME(EmailCategory.TRANSACTIONAL),
    ACCOUNT_CLOSED(EmailCategory.TRANSACTIONAL),
    PASSWORD_RESET(EmailCategory.TRANSACTIONAL),

    INVITE_INITIAL(EmailCategory.TRANSACTIONAL),
    INVITE_REMINDER(EmailCategory.TRANSACTIONAL),
    INVITE_CANCEL(EmailCategory.TRANSACTIONAL),

    MARKETING_ANNOUNCE(EmailCategory.MARKETING);

    private final EmailCategory defaultCategory;

    EmailTemplate(EmailCategory defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public EmailCategory defaultCategory() {
        return defaultCategory;
    }
}
