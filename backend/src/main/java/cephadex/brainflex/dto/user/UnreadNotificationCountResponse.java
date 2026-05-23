/**
 * Tiny envelope for {@code GET /api/notifications/unread-count}. The bell
 * badge polls this every 60 seconds when the STOMP connection is closed, so
 * keep the response small enough to be cheap at that cadence.
 */
package cephadex.brainflex.dto.user;

public record UnreadNotificationCountResponse(long count) {
}
