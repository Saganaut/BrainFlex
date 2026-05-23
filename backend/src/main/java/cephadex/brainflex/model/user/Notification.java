/**
 * In-app notification row owned by a single user.
 *
 * One row per delivery: the same event (e.g. a comment posted on a multi-owner
 * deck) fans out into one Notification per recipient. The row carries enough
 * denorm to render the dropdown line without a hydration round-trip — title,
 * body, actor display fields, and a target {@code link} that the client
 * navigates to on click.
 *
 * Realtime delivery rides on top of this row: after insert, the service pushes
 * the same DTO over {@code /user/{userId}/queue/notifications} so live clients
 * prepend the row without polling. Offline clients reconcile via the paginated
 * GET endpoint on next focus.
 *
 * Retention is 90 days, enforced by the {@code createdAt} TTL index on the
 * Mongo collection — old rows expire automatically rather than needing a sweep.
 *
 * Indexes:
 *   - (userId, createdAt DESC) — paginated dropdown / inbox listing
 *   - (userId, read)           — unread-count badge query
 *   - createdAt TTL (90d)      — retention
 */
package cephadex.brainflex.model.user;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cephadex.brainflex.model.enums.NotificationKind;
import lombok.Data;
import cephadex.brainflex.model.shared.UserSnapshot;

@Data
@Document(collection = "notifications")
@CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
@CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'read': 1}")
public class Notification {

    @Id
    private String id;

    /** Recipient. Indexed for the cross-collection paginated read. */
    @Indexed
    private String userId;

    private NotificationKind kind;

    /** Short — sized to fit on one dropdown row. */
    private String title;

    /** Expanded copy; markdown allowed. May be null when the title is sufficient. */
    private String body;

    /** In-app route to navigate to on click, e.g. "/decks/abc123". */
    private String link;

    /** Optional override; renderers fall back to a kind-default icon when null. */
    private String iconUrl;

    /**
     * Small key/value provenance bag: {@code interactiveSessionId}, {@code deckId},
     * {@code commentId}, {@code achievementId}, etc. Kept tiny — only fields the
     * client genuinely needs for dedupe or contextual rendering belong here.
     */
    private Map<String, String> meta = new HashMap<>();

    /** Who triggered the notification, with denormalized display fields. Null
     *  for SYSTEM notifications. Frozen at write time so the row still renders
     *  after the actor is removed; renderers fall back to a default avatar
     *  when {@link UserSnapshot#pictureUrl()} is null. */
    private UserSnapshot actor;

    @JsonIgnore public String getActorUserId()      { return actor == null ? null : actor.userId(); }
    @JsonIgnore public String getActorName()        { return actor == null ? null : actor.name(); }
    @JsonIgnore public String getActorPictureUrl()  { return actor == null ? null : actor.pictureUrl(); }

    private boolean read;

    /**
     * Mongo TTL index target — the document is removed automatically 90 days
     * after creation. {@code @Indexed(expireAfter = "90d")} attaches the TTL
     * to this field; Spring parses the duration string.
     */
    @Indexed(name = "notifications_ttl_idx", expireAfter = "90d")
    private Instant createdAt = Instant.now();

    /** Stamped when the user (or read-all) flips {@link #read} to true. */
    private Instant readAt;
}
