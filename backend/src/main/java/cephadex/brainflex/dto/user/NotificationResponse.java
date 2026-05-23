/**
 * Wire-format projection of {@link cephadex.brainflex.model.user.Notification}.
 *
 * Field-for-field — the dropdown renderer keys off {@code kind} for icon and
 * copy selection, then falls back to {@code title}/{@code body} when no
 * kind-specific template applies. {@code meta} stays a flat string map so the
 * generated OpenAPI client doesn't need a discriminated union per kind.
 */
package cephadex.brainflex.dto.user;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.model.user.Notification;
import cephadex.brainflex.model.shared.UserSnapshot;

public record NotificationResponse(
        String id,
        String userId,
        NotificationKind kind,
        String title,
        String body,
        String link,
        String iconUrl,
        Map<String, String> meta,
        UserSnapshot actor,
        boolean read,
        Instant createdAt,
        Instant readAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getKind(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.getIconUrl(),
                n.getMeta() == null ? new HashMap<>() : new HashMap<>(n.getMeta()),
                n.getActor(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt());
    }
}
