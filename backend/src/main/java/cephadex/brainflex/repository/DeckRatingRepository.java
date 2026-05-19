/**
 * Spring Data repository for {@link DeckRating} rows. The compound unique
 * index on {@code (deckId, userId)} doubles as the de-dup guarantee — a
 * second insert for the same pair returns a {@code DuplicateKeyException}
 * that the service converts into an in-place update.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.DeckRating;

public interface DeckRatingRepository extends MongoRepository<DeckRating, String> {

    Optional<DeckRating> findByDeckIdAndUserId(String deckId, String userId);

    long deleteByDeckIdAndUserId(String deckId, String userId);

    Page<DeckRating> findAllByDeckId(String deckId, Pageable pageable);

    long countByDeckId(String deckId);
}
