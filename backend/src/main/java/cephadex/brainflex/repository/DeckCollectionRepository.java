/**
 * Spring Data repository for {@link DeckCollection} documents.
 *
 * Reads are simple owner / org / id lookups — pagination of "my collections"
 * uses the index on {@code ownerUserId}; public discovery (when chunk 16+
 * exposes a collections feed) uses the compound visibility / updatedAt index
 * on the model.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.DeckCollection;

public interface DeckCollectionRepository extends MongoRepository<DeckCollection, String> {

    Page<DeckCollection> findAllByOwnerUserId(String ownerUserId, Pageable pageable);

    Page<DeckCollection> findAllByOrganizationId(String organizationId, Pageable pageable);
}
