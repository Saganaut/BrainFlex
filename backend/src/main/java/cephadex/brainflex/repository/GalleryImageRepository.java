// Lookups for the gallery picker: list a user's own images, list every image
// shared with a given org. Visibility filtering and dedupe happen in the
// controller (mirroring ThemeController) so the queries stay simple.
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.GalleryImage;

public interface GalleryImageRepository extends MongoRepository<GalleryImage, String> {

    List<GalleryImage> findByOwnerId(String ownerId);

    List<GalleryImage> findByOrganizationId(String organizationId);
}
