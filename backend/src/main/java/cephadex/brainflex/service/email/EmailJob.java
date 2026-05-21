/**
 * What a caller hands to {@link EmailService#enqueue(EmailJob)}.
 *
 * Built as an immutable record + nested builder so call sites read
 * declaratively and don't need to remember positional argument order. The
 * fields are intentionally email-shaped — recipient, template, model — and
 * carry no game/session domain types; new categories add themselves to the
 * template enum without growing this record.
 *
 * Nullable contract:
 *   - {@code userId}        — null for emails to addresses we don't yet have
 *                              a user row for (initial invite to a non-member).
 *   - {@code category}      — null defaults to {@link EmailTemplate#defaultCategory()}.
 *   - {@code scheduledFor}  — null means "send as soon as the worker picks it up".
 */
package cephadex.brainflex.service.email;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record EmailJob(
        String recipient,
        String userId,
        EmailCategory category,
        EmailTemplate template,
        Map<String, Object> model,
        Instant scheduledFor) {

    public EmailCategory effectiveCategory() {
        return category != null ? category : template.defaultCategory();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String recipient;
        private String userId;
        private EmailCategory category;
        private EmailTemplate template;
        private Map<String, Object> model = new HashMap<>();
        private Instant scheduledFor;

        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder category(EmailCategory category) { this.category = category; return this; }
        public Builder template(EmailTemplate template) { this.template = template; return this; }
        public Builder model(Map<String, Object> model) {
            this.model = model != null ? new HashMap<>(model) : new HashMap<>();
            return this;
        }
        public Builder modelEntry(String key, Object value) { this.model.put(key, value); return this; }
        public Builder scheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; return this; }

        public EmailJob build() {
            if (recipient == null || recipient.isBlank())
                throw new IllegalArgumentException("EmailJob.recipient is required");
            if (template == null)
                throw new IllegalArgumentException("EmailJob.template is required");
            // Plain HashMap rather than Map.copyOf — templates legitimately
            // carry null values (e.g. an unset host-customised reminder),
            // and the immutable map factories reject null entries.
            HashMap<String, Object> snapshot = model != null ? new HashMap<>(model) : new HashMap<>();
            return new EmailJob(recipient, userId, category, template, snapshot, scheduledFor);
        }
    }
}
