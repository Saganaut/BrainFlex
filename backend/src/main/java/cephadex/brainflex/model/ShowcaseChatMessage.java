/**
 * Audience chat message sent during an active Showcase.
 *
 * Lives in its own collection rather than embedded on the Showcase document so
 * chat scales independently of game state — a long-running session would
 * otherwise rewrite the entire chat array on every message. Host moderation is
 * a soft-flag ({@code moderated=true}) so the original message is preserved
 * for audit; non-host clients render moderated rows as a "(hidden by host)"
 * placeholder.
 *
 * Compound index matches the hot read ("latest 50 messages for this
 * showcase") and the per-host "find a message by id" path.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "showcase_chat")
@CompoundIndex(name = "showcase_sent_idx", def = "{'showcaseId': 1, 'sentAt': -1}")
public class ShowcaseChatMessage {

    @Id
    private String id;

    @Indexed
    private String showcaseId;

    private String authorUserId;

    /** Denormalized at send time so the row still renders after a user is removed. */
    private String authorName;

    private String authorPictureUrl;

    private boolean fromHost;

    private boolean guest;

    /** Plain text, max 500 chars (service-enforced). */
    private String body;

    private LocalDateTime sentAt = LocalDateTime.now();

    /** Host flipped this to true: hide the body for non-hosts. Original body is preserved. */
    private boolean moderated;

    private String moderatedByUserId;

    private LocalDateTime moderatedAt;
}
