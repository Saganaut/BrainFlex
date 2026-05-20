/**
 * MongoDB repository for Deck documents. Decks shared publicly are those whose
 * visibility is PUBLIC; the create-interactiveSession template picker uses that filter.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.enums.DeckVisibility;

public interface DeckRepository extends MongoRepository<Deck, String> {

    List<Deck> findByVisibility(DeckVisibility visibility);

    List<Deck> findBySystemTrue();

    List<Deck> findByCreatorUserId(String creatorUserId);
}
