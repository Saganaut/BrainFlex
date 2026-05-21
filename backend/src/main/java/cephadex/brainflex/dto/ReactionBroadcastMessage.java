/**
 * Server → topic broadcast for a single reaction the host view animates. The
 * client uses {@code offsetMs} to time the burst relative to round start and
 * {@code emoji} as the displayed glyph.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.UserSnapshot;

public record ReactionBroadcastMessage(
        String id,
        String elementId,
        UserSnapshot user,
        String emoji,
        long offsetMs,
        LocalDateTime sentAt) {
}
