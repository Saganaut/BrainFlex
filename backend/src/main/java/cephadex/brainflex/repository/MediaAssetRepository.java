// Lookups for the media picker: list a user's own assets, list every asset
// shared with a given org, filtered optionally by kind. Visibility filtering
// and dedupe across personal + org lists happen in the controller (mirrors
// GalleryImageRepository / ThemeRepository) so the queries stay simple.
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.media.MediaAsset;
import cephadex.brainflex.model.enums.MediaKind;

public interface MediaAssetRepository extends MongoRepository<MediaAsset, String> {

    List<MediaAsset> findByOwnerId(String ownerId);

    List<MediaAsset> findByOwnerIdAndKind(String ownerId, MediaKind kind);

    List<MediaAsset> findByOrganizationId(String organizationId);

    List<MediaAsset> findByOrganizationIdAndKind(String organizationId, MediaKind kind);
}
