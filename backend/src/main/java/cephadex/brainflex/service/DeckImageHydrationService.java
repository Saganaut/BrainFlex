/**
 * Read-time hydrator that resolves `internalImgId` references on deck content
 * into fresh presigned S3 URLs across every {@link ImageSize} tier.
 *
 * Deck documents persist only the stable gallery image id for gallery-backed
 * images (so the deck doesn't rot when presigned URLs expire), but renderers
 * want one URL per size to drop into an <img>/<picture> tag. This service
 * walks every Image-bearing slot on the deck and rewrites internal images'
 * `variants` to a fresh list of presigned URLs pulled from S3. External
 * images (`useExternalImg=true`) pass through untouched.
 *
 * Hydration mutates the Deck instance in place (via DeckImageMapper.map), so
 * controllers can build the DTO from the same `deck` reference returned by
 * the repository / service. Called both on read (`GET /api/decks/{id}`) and
 * on every mutation response so the frontend never has to merge stale URLs
 * across mutation/query caches.
 *
 * Routes through DeckImageMapper for the actual schema walk — that class
 * knows every image slot in the deck, this one only knows how to map an
 * Image to its refreshed form.
 */
package cephadex.brainflex.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;
import cephadex.brainflex.repository.GalleryImageRepository;

@Service
public class DeckImageHydrationService {

    private final GalleryImageRepository galleryImageRepository;
    private final S3Service s3Service;

    public DeckImageHydrationService(GalleryImageRepository galleryImageRepository, S3Service s3Service) {
        this.galleryImageRepository = galleryImageRepository;
        this.s3Service = s3Service;
    }

    /**
     * Mutates `deck` in place so every internal Image carries a fresh list of
     * presigned variants. Images that reference a gallery id we can't find
     * (image deleted, missing variants) have their `variants` set to an empty
     * list so the renderer falls back to its placeholder rather than serving
     * a stale URL. External images are untouched.
     */
    public void hydrate(Deck deck) {
        if (deck == null) return;

        Set<String> referencedIds = collectInternalIds(deck);
        if (referencedIds.isEmpty()) return;

        Map<String, GalleryImage> byId = new HashMap<>();
        galleryImageRepository.findAllById(referencedIds).forEach(image -> byId.put(image.getId(), image));

        DeckImageMapper.map(deck, image -> refresh(image, byId));
    }

    private Image refresh(Image image, Map<String, GalleryImage> byId) {
        if (image.useExternalImg()) return image;
        String id = image.internalImgId();
        if (id == null || id.isBlank()) return image.withVariants(Map.of());
        GalleryImage record = byId.get(id);
        if (record == null || record.getVariants() == null || record.getVariants().isEmpty()) {
            return image.withVariants(Map.of());
        }
        Map<ImageSize, ImageVariant> fresh = s3Service.refreshGalleryImage(id, record.getVariants());
        return image.withVariants(fresh);
    }

    private static Set<String> collectInternalIds(Deck deck) {
        Set<String> ids = new HashSet<>();
        DeckImageMapper.forEach(deck, image -> {
            if (!image.useExternalImg() && image.internalImgId() != null && !image.internalImgId().isBlank()) {
                ids.add(image.internalImgId());
            }
        });
        return ids;
    }

    /** Convenience for callers that have a single internal Image not on a Deck
     *  (collection covers, future single-image slots). Returns the image with
     *  refreshed variants or an empty list if the gallery row is gone. */
    public Image hydrateSingle(Image image) {
        if (image == null) return null;
        if (image.useExternalImg()) return image;
        String id = image.internalImgId();
        if (id == null || id.isBlank()) return image.withVariants(Map.of());
        GalleryImage record = galleryImageRepository.findById(id).orElse(null);
        if (record == null || record.getVariants() == null || record.getVariants().isEmpty()) {
            return image.withVariants(Map.of());
        }
        Map<ImageSize, ImageVariant> fresh = s3Service.refreshGalleryImage(id, record.getVariants());
        return image.withVariants(fresh);
    }
}
