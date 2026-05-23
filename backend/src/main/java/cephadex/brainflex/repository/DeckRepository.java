/**
 * MongoDB repository for Deck documents. Decks shared publicly are those whose
 * visibility is PUBLIC; the create-interactiveSession template picker uses that filter.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.PublishStatus;

public interface DeckRepository extends MongoRepository<Deck, String> {

    List<Deck> findByVisibility(DeckVisibility visibility);

    List<Deck> findBySystemTrue();

    List<Deck> findByCreatorUserId(String creatorUserId);

    /** Chunk 17 — drives the DECKS_CREATED achievement trigger. */
    long countByCreatorUserId(String creatorUserId);

    /** Chunk 17 — drives the DECKS_PUBLISHED achievement trigger. */
    long countByCreatorUserIdAndPublishStatus(String creatorUserId, PublishStatus publishStatus);
}
