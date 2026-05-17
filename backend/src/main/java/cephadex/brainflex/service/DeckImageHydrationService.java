/**
 * Read-time hydrator that resolves `galleryImageId` references on deck content
 * into fresh presigned S3 URLs.
 *
 * Deck documents persist only the stable gallery image id (so the deck doesn't
 * rot when presigned URLs expire), but renderers want a URL to drop into an
 * <img> tag. This service walks every embedded option, looks each id up in the
 * gallery_images collection, and rewrites the option's `imageUrl` to a fresh
 * presigned URL pulled from S3. The on-disk deck is untouched — hydration
 * produces a transformed copy used only for the response body.
 *
 * Scope today: McqOption (used by both McqQuestion and ImageChoiceQuestion).
 * When `imageUrl` on the DeckElement interface (slide media, place-on-image
 * target, element background) also gains a gallery-id companion, extend this
 * service rather than duplicating the lookup logic per call site.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.ImageChoiceQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
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
     * Mutates `deck.elements` in place so each McqOption with a `galleryImageId`
     * carries a fresh presigned `imageUrl`. Options that reference a gallery id
     * we can't find (image deleted, missing s3Key) have their `imageUrl` set to
     * null so the renderer falls back to its placeholder rather than rendering a
     * stale URL.
     */
    public void hydrate(Deck deck) {
        if (deck == null || deck.getElements() == null) return;

        Set<String> referencedIds = collectReferencedGalleryIds(deck.getElements());
        if (referencedIds.isEmpty()) return;

        Map<String, GalleryImage> byId = new HashMap<>();
        galleryImageRepository.findAllById(referencedIds).forEach(image -> byId.put(image.getId(), image));

        List<DeckElement> hydrated = new ArrayList<>(deck.getElements().size());
        for (DeckElement element : deck.getElements()) {
            hydrated.add(switch (element) {
                case McqQuestion q -> DeckElementCloner.withOptions(q, hydrateOptions(q.options(), byId));
                case ImageChoiceQuestion q -> DeckElementCloner.withOptions(q, hydrateOptions(q.options(), byId));
                default -> element;
            });
        }
        deck.setElements(hydrated);
    }

    private List<McqOption> hydrateOptions(List<McqOption> options, Map<String, GalleryImage> byId) {
        if (options == null || options.isEmpty()) return options;
        List<McqOption> result = new ArrayList<>(options.size());
        for (McqOption option : options) {
            String galleryImageId = option.galleryImageId();
            if (galleryImageId == null || galleryImageId.isBlank()) {
                result.add(option);
                continue;
            }
            GalleryImage image = byId.get(galleryImageId);
            String refreshed = (image != null && image.getS3Key() != null)
                    ? s3Service.refreshPresignedUrl(image.getS3Key())
                    : null;
            result.add(option.withImageUrl(refreshed));
        }
        return result;
    }

    private static Set<String> collectReferencedGalleryIds(List<DeckElement> elements) {
        Set<String> ids = new HashSet<>();
        for (DeckElement element : elements) {
            List<McqOption> options = switch (element) {
                case McqQuestion q -> q.options();
                case ImageChoiceQuestion q -> q.options();
                default -> null;
            };
            if (options == null) continue;
            for (McqOption option : options) {
                String id = option.galleryImageId();
                if (id != null && !id.isBlank()) ids.add(id);
            }
        }
        return ids;
    }
}
