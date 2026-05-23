/**
 * Wire shape for a interactiveSession chat message — what both the REST history endpoint
 * and the STOMP broadcast send to clients.
 *
 * Identity: both the author and the moderating host are identified by their
 * session-scoped {@code playerId}. Display fields are projected through
 * {@link PublicUserSnapshot} so the underlying account userId never crosses
 * the wire. (The audit-side {@code moderatedByUserId} on the model stays
 * server-only via {@code @JsonIgnore}.)
 *
 * Moderated messages keep {@code moderated=true} so the client can render the
 * "(hidden by host)" placeholder; the original {@code body} is blanked in
 * that case.
 */
package cephadex.brainflex.dto;

import java.time.Instant;

import cephadex.brainflex.model.session.InteractiveSessionChatMessage;
import cephadex.brainflex.model.shared.PublicUserSnapshot;

public record InteractiveSessionChatMessageResponse(
        String id,
        String authorPlayerId,
        PublicUserSnapshot author,
        boolean fromHost,
        String body,
        Instant sentAt,
        boolean moderated,
        String moderatedByPlayerId,
        Instant moderatedAt) {

    /**
     * Render-safe projection. When viewing the chat history, moderated rows
     * have their body replaced with the placeholder unless the caller is the
     * host (which is enforced at the controller layer — this helper assumes
     * the caller is not privileged).
     */
    public static InteractiveSessionChatMessageResponse redactedFor(InteractiveSessionChatMessage row, boolean isHost) {
        boolean hideBody = row.isModerated() && !isHost;
        return new InteractiveSessionChatMessageResponse(
                row.getId(),
                row.getAuthorPlayerId(),
                PublicUserSnapshot.from(row.getAuthor()),
                row.isFromHost(),
                hideBody ? "(hidden by host)" : row.getBody(),
                row.getSentAt(),
                row.isModerated(),
                row.getModeratedByPlayerId(),
                row.getModeratedAt());
    }
}
