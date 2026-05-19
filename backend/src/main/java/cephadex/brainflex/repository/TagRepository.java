/**
 * Spring Data repository for {@link Tag}. Tag ids are slugs (kebab-case)
 * rather than ObjectIds.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import cephadex.brainflex.model.Tag;

public interface TagRepository extends MongoRepository<Tag, String> {

    List<Tag> findByCurated(boolean curated);

    List<Tag> findByParentTagId(String parentTagId);

    /**
     * Case-insensitive prefix match against id or displayName, used by the
     * TagPicker typeahead.
     */
    @Query("{ '$or': [ { '_id': { '$regex': ?0, '$options': 'i' } }, { 'displayName': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Tag> searchByText(String regex);
}
