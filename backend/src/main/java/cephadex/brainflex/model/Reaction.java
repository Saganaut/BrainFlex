/**
 * Audience emoji reaction sent during an active InteractiveSession round.
 *
 * Persisted in its own collection so reactions can replay during a post-game
 * review without bloating the InteractiveSession document — a single popular slide can
 * easily collect thousands of bursts. Live rendering reads aggregated counts
 * out of Redis ({@code InteractiveSessionCacheService}); this collection is the durable
 * source of truth for analytics and replays.
 *
 * The compound index on {@code (interactiveSessionId, sentAt DESC)} matches both the
 * "latest reactions for this interactiveSession" replay query and a future "per-element
 * histogram" aggregation that filters on {@code (interactiveSessionId, elementId)} and
 * then sorts.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "reactions")
@CompoundIndex(name = "interactive_session_sent_idx", def = "{'interactiveSessionId': 1, 'sentAt': -1}")
public class Reaction {

    @Id
    private String id;

    @Indexed
    private String interactiveSessionId;

    /** Element this reaction landed on; snapshot of the current round at send time. */
    private String elementId;

    private String userId;

    /** Denormalized at send time so deleted users don't blank the replay. */
    private String userName;

    private boolean guest;

    /** Single emoji codepoint, validated against an allow-list before persisting. */
    private String emoji;

    /** Milliseconds elapsed since {@code InteractiveSession.roundStartedAt} when the reaction was sent. */
    private long offsetMs;

    private LocalDateTime sentAt = LocalDateTime.now();
}
