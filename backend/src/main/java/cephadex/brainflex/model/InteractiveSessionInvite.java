/**
 * An invite for one email address to one ScheduledInteractiveSession (or a
 * direct, on-the-fly invite to a live InteractiveSession). The `inviteToken`
 * is single-use and unique across the collection — anyone holding it can
 * redeem to surface the join link.
 *
 * `resolvedUserId` and `redeemedAt` are filled once the invitee logs in /
 * accepts the link. `expiresAt` defaults to scheduledStartAt + 2h so old
 * links can't be used to crash a finished session.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "interactive_session_invites")
@CompoundIndexes({
        // Dedupe identical invites within one ScheduledInteractiveSession.
        @CompoundIndex(name = "email_scheduledId",
                def = "{'email': 1, 'scheduledInteractiveSessionId': 1}",
                unique = true, sparse = true)
})
public class InteractiveSessionInvite {
    @Id
    private String id;

    /** Set once the parent ScheduledInteractiveSession boots into a live session. */
    @Indexed
    private String interactiveSessionId;

    /** Points back at the parent ScheduledInteractiveSession (if any). */
    @Indexed
    private String scheduledInteractiveSessionId;

    /** Case-normalised email; we lowercase on accept. */
    private String email;

    private String invitedByUserId;

    /**
     * Single-use base64url token (~24 bytes of entropy). Indexed unique so
     * /api/invites/{token}/redeem can do a single lookup without scanning.
     */
    @Indexed(unique = true)
    private String inviteToken;

    /** Populated when the invitee logs in / accepts. */
    private String resolvedUserId;

    private LocalDateTime sentAt = LocalDateTime.now();

    /** When the invitee actually joined the lobby. Null until then. */
    private LocalDateTime redeemedAt;

    /**
     * Default = scheduledStartAt + 2h for scheduled-session invites; for
     * ad-hoc live-session invites we set it to interactiveSession.createdAt + 12h.
     */
    private LocalDateTime expiresAt;
}
