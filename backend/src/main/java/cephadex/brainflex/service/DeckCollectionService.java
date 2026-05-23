/**
 * Business logic for {@link DeckCollection} CRUD plus the four list-mutation
 * surfaces (add deck, remove deck, reorder, change visibility).
 *
 * Ownership: a collection is owned by exactly one user. Org-shared collections
 * are still owned by their creator — the {@code organizationId} only widens
 * read access. Only the owner can mutate. The owner must be a member of
 * {@code organizationId} when the collection is org-scoped; this matches the
 * Theme/Deck rule of "you can't share into an org you're not in".
 *
 * Cover image: collections re-use the {@link Image} record from the deck
 * model. Internal images carry only the gallery id at rest — the transport
 * {@code imgUrl} is stripped on write the same way Deck cover/background is,
 * and refreshed at read time. The frontend therefore never has to merge a
 * stale presigned URL into its cache.
 *
 * Missing decks: when a deck referenced by a collection is deleted or hidden
 * from the caller, it is silently dropped from the detail response — the
 * stored id stays put so a re-publish brings it back without an admin sweep.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateDeckCollectionRequest;
import cephadex.brainflex.dto.UpdateDeckCollectionRequest;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollection;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageSize;
import cephadex.brainflex.model.image.ImageVariant;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.media.GalleryImage;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckCollectionRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GalleryImageRepository;

@Service
public class DeckCollectionService {

    private final DeckCollectionRepository collectionRepository;
    private final DeckRepository deckRepository;
    private final DeckService deckService;
    private final GalleryImageRepository galleryImageRepository;
    private final S3Service s3Service;
    private final MongoTemplate mongoTemplate;

    public DeckCollectionService(
            DeckCollectionRepository collectionRepository,
            DeckRepository deckRepository,
            DeckService deckService,
            GalleryImageRepository galleryImageRepository,
            S3Service s3Service,
            MongoTemplate mongoTemplate) {
        this.collectionRepository = collectionRepository;
        this.deckRepository = deckRepository;
        this.deckService = deckService;
        this.galleryImageRepository = galleryImageRepository;
        this.s3Service = s3Service;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Mutate {@code col} in place so its {@code cover} carries a fresh
     * presigned URL. Mirrors {@link DeckImageHydrationService} but for the
     * single image slot on a collection; called from the controller before
     * the DTO is built so clients never see a stale URL.
     */
    public void hydrateCover(DeckCollection col) {
        if (col == null || col.getCover() == null)
            return;
        Image cover = col.getCover();
        if (cover.useExternalImg())
            return;
        String galleryId = cover.internalImgId();
        if (galleryId == null || galleryId.isBlank()) {
            col.setCover(cover.withVariants(Map.of()));
            return;
        }
        GalleryImage record = galleryImageRepository.findById(galleryId).orElse(null);
        Map<ImageSize, ImageVariant> fresh = (record != null && record.getVariants() != null
                && !record.getVariants().isEmpty())
                        ? s3Service.refreshGalleryImage(galleryId, record.getVariants())
                        : Map.of();
        col.setCover(cover.withVariants(fresh));
    }

    // ---- Read ----

    public Page<DeckCollection> listForOwner(String ownerUserId, Pageable pageable) {
        return collectionRepository.findAllByOwnerUserId(ownerUserId, pageable);
    }

    /**
     * Read a collection enforcing visibility rules.
     *
     * PUBLIC / UNLISTED → anyone with the id
     * ORG → registered users in the matching organization
     * PRIVATE → owner only
     *
     * Throws 401 (no caller) or 403 (caller, wrong scope) on a denied read.
     */
    public DeckCollection getViewable(Optional<User> caller, String id) {
        DeckCollection col = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
        DeckVisibility visibility = col.getVisibility();
        if (visibility == DeckVisibility.PUBLIC || visibility == DeckVisibility.UNLISTED) {
            return col;
        }
        User user = caller.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to view this collection"));
        if (visibility == DeckVisibility.ORG) {
            String orgId = col.getOrganizationId();
            List<String> memberships = user.getOrganizationIds();
            if (orgId != null && memberships != null && memberships.contains(orgId)) {
                return col;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Collection is restricted to its organization");
        }
        if (user.getId().equals(col.getOwnerUserId())) {
            return col;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this collection");
    }

    /**
     * Filter the collection's stored deck-id list down to decks the caller can
     * actually see, preserving order. Returned in the same order as
     * {@code col.getDeckIds()}. Missing / hidden decks are silently dropped.
     */
    public List<Deck> resolveDecks(DeckCollection col, Optional<User> caller) {
        List<String> ids = col.getDeckIds();
        if (ids == null || ids.isEmpty())
            return List.of();
        List<Deck> hits = deckRepository.findAllById(ids);
        Set<String> visibleIds = new HashSet<>();
        for (Deck deck : hits) {
            try {
                deckService.getViewable(caller, deck.getId());
                visibleIds.add(deck.getId());
            } catch (ResponseStatusException ignored) {
                // Caller can't see this deck — drop it from the resolved list.
            }
        }
        // Preserve the curator's order rather than Mongo's findAllById order.
        java.util.Map<String, Deck> byId = new java.util.HashMap<>();
        for (Deck deck : hits)
            byId.put(deck.getId(), deck);
        List<Deck> ordered = new ArrayList<>(ids.size());
        for (String deckId : ids) {
            Deck deck = byId.get(deckId);
            if (deck != null && visibleIds.contains(deckId))
                ordered.add(deck);
        }
        return ordered;
    }

    // ---- Write ----

    public DeckCollection create(User owner, CreateDeckCollectionRequest request) {
        DeckCollection col = new DeckCollection();
        String id = (request.id() != null && !request.id().isBlank())
                ? request.id()
                : UUID.randomUUID().toString();
        if (collectionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Collection id already exists");
        }
        col.setId(id);
        col.setOwnerUserId(owner.getId());
        col.setName(request.name());
        col.setDescription(request.description());
        col.setVisibility(request.visibility() == null ? DeckVisibility.PRIVATE : request.visibility());
        applyOrganization(col, request.organizationId(), owner);
        col.setCover(normalizeImage(request.cover()));
        return collectionRepository.save(col);
    }

    public DeckCollection update(String id, User caller, UpdateDeckCollectionRequest request) {
        DeckCollection col = requireOwned(id, caller);
        if (request.name() != null)
            col.setName(request.name());
        if (request.description() != null) {
            col.setDescription(request.description().isBlank() ? null : request.description());
        }
        if (request.visibility() != null)
            col.setVisibility(request.visibility());
        if (request.cover() != null) {
            // An Image.empty() clears the cover; a blank URL also clears it.
            col.setCover(request.cover().isBlank() && request.cover().internalImgId() == null
                    ? null
                    : normalizeImage(request.cover()));
        }
        return collectionRepository.save(col);
    }

    public void delete(String id, User caller) {
        DeckCollection col = requireOwned(id, caller);
        collectionRepository.delete(col);
    }

    /**
     * Add a deck to the collection at the requested position (clamped to
     * {@code [0, size]}; null appends). The caller must own the collection
     * and be able to view the deck — collections can curate any deck the
     * curator can see, including someone else's PUBLIC deck.
     *
     * Idempotent: if the deck is already in the collection, the existing
     * position is preserved and the document is returned unchanged.
     */
    public DeckCollection addDeck(String collectionId, User caller, String deckId, Integer position) {
        DeckCollection col = requireOwned(collectionId, caller);
        // Visibility check — uses the same rules as GET /api/decks/{id}.
        deckService.getViewable(Optional.of(caller), deckId);
        List<String> ids = new ArrayList<>(col.getDeckIds() == null ? List.of() : col.getDeckIds());
        if (ids.contains(deckId)) {
            return col;
        }
        int clamped = position == null
                ? ids.size()
                : Math.max(0, Math.min(position, ids.size()));
        ids.add(clamped, deckId);
        col.setDeckIds(ids);
        return collectionRepository.save(col);
    }

    /** Remove a deck. Idempotent — removing a deck that isn't there is a no-op. */
    public DeckCollection removeDeck(String collectionId, User caller, String deckId) {
        DeckCollection col = requireOwned(collectionId, caller);
        List<String> ids = new ArrayList<>(col.getDeckIds() == null ? List.of() : col.getDeckIds());
        if (!ids.remove(deckId)) {
            return col;
        }
        col.setDeckIds(ids);
        return collectionRepository.save(col);
    }

    /**
     * Replace the ordered deck-id list. The replacement must be a permutation
     * of the current list — same multiset of ids, in the requested order —
     * otherwise we throw 400. Adds and removes go through their own endpoints,
     * so a typo in the reorder body can't silently drop a deck or sneak a
     * different one in.
     */
    public DeckCollection reorderDecks(String collectionId, User caller, List<String> nextIds) {
        DeckCollection col = requireOwned(collectionId, caller);
        List<String> current = col.getDeckIds() == null ? List.of() : col.getDeckIds();
        if (nextIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deckIds is required");
        }
        if (nextIds.size() != current.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reorder must contain every existing deck id exactly once");
        }
        List<String> currentSorted = new ArrayList<>(current);
        List<String> nextSorted = new ArrayList<>(nextIds);
        Collections.sort(currentSorted);
        Collections.sort(nextSorted);
        if (!currentSorted.equals(nextSorted)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reorder must be a permutation of the existing deck ids");
        }
        col.setDeckIds(new ArrayList<>(nextIds));
        return collectionRepository.save(col);
    }

    /**
     * Atomically bump {@code viewCount}. Called from the controller on non-
     * owner reads only, mirroring the deck rule.
     */
    public void incrementViewCount(String collectionId) {
        if (collectionId == null || collectionId.isBlank())
            return;
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(collectionId)),
                new Update().inc("viewCount", 1),
                DeckCollection.class);
    }

    // ---- Helpers ----

    private DeckCollection requireOwned(String id, User caller) {
        DeckCollection col = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
        if (!caller.getId().equals(col.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this collection");
        }
        return col;
    }

    private void applyOrganization(DeckCollection col, String orgId, User owner) {
        if (orgId == null || orgId.isBlank()) {
            col.setOrganizationId(null);
            return;
        }
        List<String> memberships = owner.getOrganizationIds();
        if (memberships == null || !memberships.contains(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a member of that organization");
        }
        col.setOrganizationId(orgId);
    }

    /**
     * Strip the transport-only variant list on internal images so the
     * collection document doesn't persist soon-to-expire presigned URLs.
     * Read paths regenerate them via {@link DeckImageHydrationService} —
     * collection covers are hydrated through the same gallery flow.
     */
    private static Image normalizeImage(Image image) {
        if (image == null)
            return null;
        if (image.useExternalImg())
            return image;
        return image.withVariants(Map.of());
    }
}
