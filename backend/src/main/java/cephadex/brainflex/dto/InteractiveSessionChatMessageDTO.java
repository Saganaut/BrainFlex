/**
 * Wire shape for a interactiveSession chat message — what both the REST history endpoint
 * and the STOMP broadcast send to clients. Moderated messages keep
 * {@code moderated=true} so the client can render the "(hidden by host)"
 * placeholder; the original {@code body} is blanked in that case.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.InteractiveSessionChatMessage;

public record InteractiveSessionChatMessageDTO(
        String id,
        String authorUserId,
        String authorName,
        String authorPictureUrl,
        boolean fromHost,
        boolean guest,
        String body,
        LocalDateTime sentAt,
        boolean moderated,
        String moderatedByUserId,
        LocalDateTime moderatedAt) {

    /**
     * Render-safe projection. When viewing the chat history, moderated rows
     * have their body replaced with the placeholder unless the caller is the
     * host (which is enforced at the controller layer — this helper assumes
     * the caller is not privileged).
     */
    public static InteractiveSessionChatMessageDTO redactedFor(InteractiveSessionChatMessage row, boolean isHost) {
        boolean hideBody = row.isModerated() && !isHost;
        return new InteractiveSessionChatMessageDTO(
                row.getId(),
                row.getAuthorUserId(),
                row.getAuthorName(),
                row.getAuthorPictureUrl(),
                row.isFromHost(),
                row.isGuest(),
                hideBody ? "(hidden by host)" : row.getBody(),
                row.getSentAt(),
                row.isModerated(),
                row.getModeratedByUserId(),
                row.getModeratedAt());
    }
}
