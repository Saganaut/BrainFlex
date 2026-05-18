/**
 * Read-time hydrator that resolves `internalImgId` references on deck content
 * into fresh presigned S3 URLs.
 *
 * Deck documents persist only the stable gallery image id for gallery-backed
 * images (so the deck doesn't rot when presigned URLs expire), but renderers
 * want a URL to drop into an <img> tag. This service walks every Image-bearing
 * slot on the deck and rewrites internal images' `imgUrl` to a fresh
 * presigned URL pulled from S3. External images (`useExternalImg=true`) pass
 * through untouched.
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
     * Mutates `deck` in place so every internal Image carries a fresh
     * presigned `imgUrl`. Images that reference a gallery id we can't find
     * (image deleted, missing s3Key) have their `imgUrl` set to null so the
     * renderer falls back to its placeholder rather than serving a stale URL.
     * External images are untouched.
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
        if (id == null || id.isBlank()) return image.withImgUrl(null);
        GalleryImage record = byId.get(id);
        String url = (record != null && record.getS3Key() != null)
                ? s3Service.refreshPresignedUrl(record.getS3Key())
                : null;
        return image.withImgUrl(url);
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
}
