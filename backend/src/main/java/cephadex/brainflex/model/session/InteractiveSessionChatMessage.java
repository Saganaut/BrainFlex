/**
 * Audience chat message sent during an active InteractiveSession.
 *
 * Lives in its own collection rather than embedded on the InteractiveSession document so
 * chat scales independently of game state — a long-running session would
 * otherwise rewrite the entire chat array on every message. Host moderation is
 * a soft-flag ({@code moderated=true}) so the original message is preserved
 * for audit; non-host clients render moderated rows as a "(hidden by host)"
 * placeholder.
 *
 * Compound index matches the hot read ("latest 50 messages for this
 * interactiveSession") and the per-host "find a message by id" path.
 */
package cephadex.brainflex.model.session;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import cephadex.brainflex.model.shared.UserSnapshot;

@Data
@Document(collection = "interactive_session_chat")
@CompoundIndex(name = "interactive_session_sent_idx", def = "{'interactiveSessionId': 1, 'sentAt': -1}")
public class InteractiveSessionChatMessage {

    @Id
    private String id;

    @Indexed
    private String interactiveSessionId;

    /** Session-scoped public handle of the author. Broadcast DTOs use this
     *  instead of the real userId. */
    private String authorPlayerId;

    /** Denormalized author display + guest flag, frozen at send time so the
     *  row still renders after the sender is removed. Server-side /
     *  persistence only — broadcast DTOs project this through
     *  {@link PublicUserSnapshot}. */
    private UserSnapshot author;

    @JsonIgnore public String getAuthorUserId()      { return author == null ? null : author.userId(); }
    @JsonIgnore public String getAuthorName()        { return author == null ? null : author.name(); }
    @JsonIgnore public String getAuthorPictureUrl()  { return author == null ? null : author.pictureUrl(); }
    @JsonIgnore public boolean isGuest()             { return author != null && author.guest(); }

    private boolean fromHost;

    /** Plain text, max 500 chars (service-enforced). */
    private String body;

    private Instant sentAt = Instant.now();

    /** Host flipped this to true: hide the body for non-hosts. Original body is preserved. */
    private boolean moderated;

    /** Session-scoped public handle of the moderating host. Broadcast in the
     *  chat DTO so non-host clients can attribute moderation without seeing the
     *  host's real userId. */
    private String moderatedByPlayerId;

    /** Real userId of the moderating host — server-side audit field only. */
    @JsonIgnore
    private String moderatedByUserId;

    private Instant moderatedAt;
}
