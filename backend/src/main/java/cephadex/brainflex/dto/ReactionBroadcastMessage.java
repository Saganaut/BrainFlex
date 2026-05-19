/**
 * Server → topic broadcast for a single reaction the host view animates. The
 * client uses {@code offsetMs} to time the burst relative to round start and
 * {@code emoji} as the displayed glyph.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

public record ReactionBroadcastMessage(
        String id,
        String elementId,
        String userId,
        String userName,
        boolean guest,
        String emoji,
        long offsetMs,
        LocalDateTime sentAt) {
}
