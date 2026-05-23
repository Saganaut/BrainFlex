/**
 * Spring Data repository for {@link DeckFavorite} rows.
 *
 * The (userId, deckId) compound index doubles as the de-dup guarantee: a
 * second insert for the same pair returns a {@code DuplicateKeyException} that
 * {@link cephadex.brainflex.service.DeckFavoriteService} converts into an
 * idempotent no-op.
 */
package cephadex.brainflex.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.deck.DeckFavorite;
public interface DeckFavoriteRepository extends MongoRepository<DeckFavorite, String> {

    Optional<DeckFavorite> findByUserIdAndDeckId(String userId, String deckId);

    long deleteByUserIdAndDeckId(String userId, String deckId);

    Page<DeckFavorite> findAllByUserId(String userId, Pageable pageable);

    List<DeckFavorite> findAllByUserIdAndDeckIdIn(String userId, Collection<String> deckIds);

    long countByDeckId(String deckId);
}
