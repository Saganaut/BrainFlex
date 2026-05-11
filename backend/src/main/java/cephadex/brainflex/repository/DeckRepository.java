/**
 * MongoDB repository for Deck documents.
 * Exposes queries for public packs and system-seeded packs so the
 * game creation UI can display available content to users.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Deck;

public interface DeckRepository extends MongoRepository<Deck, String> {

    List<Deck> findByIsPublicTrue();

    List<Deck> findByIsSystemTrue();

    List<Deck> findByCreatorUserId(String creatorUserId);
}
