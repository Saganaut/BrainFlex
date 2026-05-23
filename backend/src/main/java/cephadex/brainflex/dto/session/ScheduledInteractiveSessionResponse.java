/**
 * Wire representation of a ScheduledInteractiveSession. Includes denormalised
 * `hostName` + `deckName` so the host's "/scheduled" list page doesn't need
 * a second round-trip per row. Invite rows are loaded separately.
 */
package cephadex.brainflex.dto.session;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.model.session.InteractiveSessionSettings;
import cephadex.brainflex.model.session.ScheduledInteractiveSession;

public record ScheduledInteractiveSessionResponse(
        String id,
        String hostUserId,
        String hostName,
        String deckId,
        String deckName,
        InteractiveSessionSettings settings,
        Instant scheduledStartAt,
        Instant scheduledEndAt,
        String reminderEmailTemplate,
        List<String> invitedEmails,
        String createdInteractiveSessionId,
        ScheduleStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ScheduledInteractiveSessionResponse of(ScheduledInteractiveSession s,
            String hostName,
            String deckName) {
        return new ScheduledInteractiveSessionResponse(
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
