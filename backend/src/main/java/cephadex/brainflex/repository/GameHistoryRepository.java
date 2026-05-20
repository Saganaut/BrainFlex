/**
 * Spring Data repository for {@link cephadex.brainflex.model.GameHistoryEntry} rows.
 *
 * The {@code (userId, interactiveSessionId)} compound unique index doubles as
 * the de-dup guarantee: a second insert for the same pair throws
 * {@code DuplicateKeyException} that {@link cephadex.brainflex.service.GameHistoryService}
 * swallows so {@code recordFinish} is idempotent under retries / replays.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.GameHistoryEntry;

public interface GameHistoryRepository extends MongoRepository<GameHistoryEntry, String> {

    Page<GameHistoryEntry> findAllByUserId(String userId, Pageable pageable);

    Page<GameHistoryEntry> findAllByUserIdAndDeckId(String userId, String deckId, Pageable pageable);

    Optional<GameHistoryEntry> findByUserIdAndInteractiveSessionId(String userId, String interactiveSessionId);

    boolean existsByUserIdAndInteractiveSessionId(String userId, String interactiveSessionId);
}
