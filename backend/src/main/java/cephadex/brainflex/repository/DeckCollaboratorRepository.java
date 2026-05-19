/**
 * Spring Data repository for {@link DeckCollaborator} rows.
 *
 * The {@code (deckId, userId)} unique index doubles as the de-dup guarantee: a
 * second insert for the same pair returns a {@code DuplicateKeyException} that
 * {@link cephadex.brainflex.service.DeckCollaboratorService} converts into an
 * idempotent role update.
 */
package cephadex.brainflex.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.DeckCollaborator;
import cephadex.brainflex.model.enums.CollaboratorRole;

public interface DeckCollaboratorRepository extends MongoRepository<DeckCollaborator, String> {

    List<DeckCollaborator> findByDeckId(String deckId);

    Optional<DeckCollaborator> findByDeckIdAndUserId(String deckId, String userId);

    Optional<DeckCollaborator> findByDeckIdAndRole(String deckId, CollaboratorRole role);

    Optional<DeckCollaborator> findByDeckIdAndEmail(String deckId, String email);

    List<DeckCollaborator> findByUserId(String userId);

    List<DeckCollaborator> findByUserIdAndRoleIn(String userId, Collection<CollaboratorRole> roles);

    List<DeckCollaborator> findByEmail(String email);

    long deleteByDeckIdAndUserId(String deckId, String userId);

    long deleteByDeckId(String deckId);
}
