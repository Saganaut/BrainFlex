/**
 * A pre-booked InteractiveSession. Stored independently of the live session
 * document — `createdInteractiveSessionId` is filled in once the cron sweep
 * boots it and transitions status SCHEDULED → LIVE.
 *
 * `settings` is copied to the live `InteractiveSession.settings` at boot time
 * so subsequent edits to this row don't desync the running session. The
 * separate `InteractiveSessionInvite` rows carry per-invitee tokens.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.ScheduleStatus;
import lombok.Data;

@Data
@Document(collection = "scheduled_interactive_sessions")
@CompoundIndexes({
        // The cron sweep filters on (status, scheduledStartAt) to find rows
        // ready to boot; the compound index keeps that query cheap.
        @CompoundIndex(name = "status_startAt", def = "{'status': 1, 'scheduledStartAt': 1}")
})
public class ScheduledInteractiveSession {
    @Id
    private String id;

    private String hostUserId;
    private String deckId;

    private InteractiveSessionSettings settings = new InteractiveSessionSettings();

    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;            // estimate — for calendar exports

    // Host-customised reminder body. Null = use the default Thymeleaf template.
    private String reminderEmailTemplate;

    // Raw email addresses (case-normalised) the host invited at create time.
    // The corresponding `InteractiveSessionInvite` rows carry the token + redemption state.
    private List<String> invitedEmails = new ArrayList<>();

    // Set when the cron sweep boots the session. Until then, null.
    private String createdInteractiveSessionId;

    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
