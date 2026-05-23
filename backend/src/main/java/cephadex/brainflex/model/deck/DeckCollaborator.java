/**
 * Join document linking a user to a deck they may view or edit. Stored separately
 * from {@link Deck} so a deck doesn't grow unbounded as collaborators are added,
 * and so "decks shared with me" can be answered with a single indexed query.
 *
 * Exactly one row per deck carries {@code role = OWNER}; the legacy
 * {@code Deck.creatorUserId} is preserved as the historical first-author pointer,
 * but the *current* owner is whoever holds the OWNER row.
 *
 * Pending invites (no matching user yet) are keyed by {@code email}; the resolver
 * promotes them to a {@code userId} on the invitee's first login. {@code acceptedAt}
 * is null until the user accepts; today we auto-accept on insert and use the field
 * as an explicit log entry — the field stays nullable so a future "pending invite
 * review" UX can surface unaccepted rows.
 */
package cephadex.brainflex.model.deck;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.CollaboratorRole;
import lombok.Data;

@Data
@Document(collection = "deck_collaborators")
@CompoundIndex(name = "deck_user_unique_idx", def = "{'deckId': 1, 'userId': 1}", unique = true)
public class DeckCollaborator {

    @Id
    private String id;

    @Indexed
    private String deckId;

    // Either userId or email is set. userId is preferred; email is used for
    // invites to a not-yet-registered address.
    @Indexed
    private String userId;

    @Indexed
    private String email;

    private CollaboratorRole role;

    private String invitedByUserId;

    private Instant invitedAt = Instant.now();

    // Null = pending invite. Auto-set to invitedAt when a known user is added.
    private Instant acceptedAt;
}
