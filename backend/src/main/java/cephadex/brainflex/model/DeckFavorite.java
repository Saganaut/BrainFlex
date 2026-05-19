/**
 * Join document linking a user to a deck they have favorited. Stored separately
 * from {@link User} so the user document does not grow unbounded as users star
 * thousands of decks; reads of "my favorites" are a single indexed query on
 * this collection.
 *
 * A compound unique index on {@code (userId, deckId)} guarantees there can
 * only ever be one favorite row per (user, deck) pair — the service relies on
 * Mongo's duplicate-key error to make {@code favorite()} idempotent without an
 * extra read.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "deck_favorites")
@CompoundIndex(name = "user_deck_unique_idx", def = "{'userId': 1, 'deckId': 1}", unique = true)
public class DeckFavorite {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String deckId;

    private LocalDateTime favoritedAt = LocalDateTime.now();
}
