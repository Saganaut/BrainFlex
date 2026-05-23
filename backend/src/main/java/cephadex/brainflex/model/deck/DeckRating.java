/**
 * One-row-per-(deck, user) star rating with an optional written review.
 *
 * Stars are validated at the service layer (1..5). Re-rating the same deck
 * updates the existing row and the difference is propagated to
 * {@code Deck.averageRating} / {@code ratingCount} via incremental atomic
 * updates so list views can sort on the denorm without a join.
 *
 * The compound unique index on {@code (deckId, userId)} guarantees a single
 * row per pair; the service relies on Mongo's duplicate-key error to keep the
 * upsert path race-free.
 */
package cephadex.brainflex.model.deck;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import cephadex.brainflex.model.shared.Auditable;

@Data
@Document(collection = "deck_ratings")
@CompoundIndex(name = "deck_user_unique_idx", def = "{'deckId': 1, 'userId': 1}", unique = true)
public class DeckRating extends Auditable {

    @Id
    private String id;

    @Indexed
    private String deckId;

    @Indexed
    private String userId;

    /** 1..5 — enforced at the service layer. */
    private int stars;

    /** Optional written review body. Max 2000 chars, enforced server-side. */
    private String review;
}
