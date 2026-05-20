/**
 * Wire representation of a ScheduledInteractiveSession. Includes denormalised
 * `hostName` + `deckName` so the host's "/scheduled" list page doesn't need
 * a second round-trip per row. Invite rows are loaded separately.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.enums.ScheduleStatus;

public record ScheduledInteractiveSessionDTO(
        String id,
        String hostUserId,
        String hostName,
        String deckId,
        String deckName,
        InteractiveSessionSettings settings,
        LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        String reminderEmailTemplate,
        List<String> invitedEmails,
        String createdInteractiveSessionId,
        ScheduleStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ScheduledInteractiveSessionDTO of(ScheduledInteractiveSession s,
                                                    String hostName,
                                                    String deckName) {
        return new ScheduledInteractiveSessionDTO(
                s.getId(),
                s.getHostUserId(),
                hostName,
                s.getDeckId(),
                deckName,
                s.getSettings(),
                s.getScheduledStartAt(),
                s.getScheduledEndAt(),
                s.getReminderEmailTemplate(),
                s.getInvitedEmails(),
                s.getCreatedInteractiveSessionId(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
