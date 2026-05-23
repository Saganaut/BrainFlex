/**
 * Business logic for browsing, creating, and editing decks + their embedded elements.
 *
 * Elements live inside the deck document, so every "question CRUD" operation is a
 * targeted edit on Deck.elements followed by a single save. Element order is the
 * natural list order — reordering is just moving an item to a new index.
 *
 * All mutation methods enforce ownership; system decks (`isSystem=true`) and decks
 * owned by another user are off-limits.
 *
 * Image writes are normalized through DeckImageMapper: for any internal image
 * (`useExternalImg=false`) the transport-only `imgUrl` is dropped before save,
 * because the read-time hydrator always regenerates it. External images pass
 * through unchanged.
 */
package cephadex.brainflex.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.deck.CreateDeckRequest;
import cephadex.brainflex.dto.deck.DeckExploreRequest;
import cephadex.brainflex.dto.deck.UpdateDeckRequest;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.parts.McqOption;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckRepository;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final AuthorizationService authorizationService;
    private final TagService tagService;
    private final MongoTemplate mongoTemplate;
    private final DeckCollaboratorService deckCollaboratorService;
    private final AchievementService achievementService;

    public DeckService(
            DeckRepository deckRepository,
            AuthorizationService authorizationService,
            TagService tagService,
            MongoTemplate mongoTemplate,
            DeckCollaboratorService deckCollaboratorService,
            AchievementService achievementService) {
        this.deckRepository = deckRepository;
        this.authorizationService = authorizationService;
        this.tagService = tagService;
        this.mongoTemplate = mongoTemplate;
        this.deckCollaboratorService = deckCollaboratorService;
        this.achievementService = achievementService;
    }

    // ---- Read ----

    public List<Deck> listPublic() {
        return deckRepository.findByVisibility(DeckVisibility.PUBLIC);
    }

    public List<Deck> listByOwner(String userId) {

        return deckRepository.findByCreatorUserId(userId);
    }

    /**
     * Fetches a deck for read-only viewing. Caller may be empty (anonymous
     * visitor / guest); the visibility matrix decides what they can see:
     * PUBLIC, UNLISTED → anyone with the id
     * ORG → registered users in the same organization, or
     * any collaborator (OWNER/EDITOR/VIEWER)
     * PRIVATE → owner + invited collaborators only
     * Unmet visibility rules throw 401 (no caller) or 403 (caller, wrong scope).
     */
    public Deck getViewable(Optional<User> caller, String id) {
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        DeckVisibility visibility = deck.getVisibility();
        if (visibility == DeckVisibility.PUBLIC || visibility == DeckVisibility.UNLISTED) {
            return deck;
        }
        User user = caller
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to view this deck"));
        if (authorizationService.canViewDeck(deck, user)) {
            return deck;
        }
        if (visibility == DeckVisibility.ORG) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deck is restricted to its organization");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this deck");
    }

    // ---- Deck CRUD ----

    public Deck createDeck(User creator, CreateDeckRequest request) {
        Deck deck = new Deck();
        // Accept the caller-provided id (frontend pre-generates a UUID so the
        // optimistic editor can reference the deck before the roundtrip).
        // Fall back to a server-generated UUID otherwise.
        String id = (request.id() != null && !request.id().isBlank())
                ? request.id()
                : UUID.randomUUID().toString();
        if (deckRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deck id already exists");
        }
        deck.setId(id);
        deck.getContent().setName(request.name());
        deck.getContent().setDescription(request.description());
        deck.setTags(request.tags() == null ? new java.util.LinkedHashSet<>()
                : new java.util.LinkedHashSet<>(request.tags()));
        java.util.Set<String> tagIds = request.tagIds() == null ? new java.util.LinkedHashSet<>()
                : new java.util.LinkedHashSet<>(request.tagIds());
        tagService.requireAllExist(new ArrayList<>(tagIds));
        deck.setTagIds(tagIds);
        if (request.subjectTagId() != null && !request.subjectTagId().isBlank()) {
            tagService.requireAllExist(List.of(request.subjectTagId()));
            deck.setSubjectTagId(request.subjectTagId());
        }
        deck.setVisibility(request.visibility() == null ? DeckVisibility.PRIVATE : request.visibility());
        deck.getContent().setFormat(
                request.defaultSessionFormat() == null ? SessionFormat.GAME : request.defaultSessionFormat());
        deck.getContent().setShowResponses(
                request.defaultShowResponses() == null ? ShowResponsesMode.INHERIT : request.defaultShowResponses());
        deck.getContent().setCover(normalizeImage(request.cover()));
        deck.getContent().setBackground(normalizeImage(request.background()));
        deck.getContent().setThemeId(request.themeId());
        deck.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        deck.setSystem(false);

        if (request.language() != null && !request.language().isBlank()) {
            deck.setLanguage(request.language());
        }
        if (request.difficulty() != null) {
            deck.setDifficulty(request.difficulty());
        }
        if (request.ageRange() != null && !request.ageRange().isBlank()) {
            deck.setAgeRange(request.ageRange());
        }
        if (request.license() != null) {
            deck.setLicense(request.license());
        }

        deck.setCreatorUserId(creator.getId());
        // First-author credit defaults to the creator — copy-on-fork updates
        // it explicitly later when fork support lands.
        deck.setOriginalAuthorUserId(creator.getId());
        Deck saved = deckRepository.save(deck);
        // Seed the OWNER collaborator row so authorization checks and the
        // "decks shared with me" query both have a consistent source of truth
        // from day one — no special-case for freshly-created decks.
        deckCollaboratorService.addInitialOwner(saved, creator);
        // Chunk 17 — DECKS_CREATED trigger fires on every create (counter is
        // the user's deck count after the save). Achievement service is
        // fire-and-forget so failures never break create.
        long ownedCount = deckRepository.countByCreatorUserId(creator.getId());
        achievementService.evaluate(creator.getId(), AchievementTrigger.DECKS_CREATED,
                (int) Math.min(ownedCount, Integer.MAX_VALUE), null, saved.getId());
        return saved;
    }

    public Deck updateDeck(String id, User caller, UpdateDeckRequest request) {
        Deck deck = requireOwned(id, caller);
        if (request.name() != null)
            deck.getContent().setName(request.name());
        if (request.description() != null)
            deck.getContent().setDescription(request.description());
        if (request.tags() != null)
            deck.setTags(new java.util.LinkedHashSet<>(request.tags()));
        if (request.tagIds() != null) {
            java.util.Set<String> nextIds = new java.util.LinkedHashSet<>(request.tagIds());
            tagService.requireAllExist(new ArrayList<>(nextIds));
            deck.setTagIds(nextIds);
        }
        if (request.subjectTagId() != null) {
            if (request.subjectTagId().isBlank()) {
                deck.setSubjectTagId(null);
            } else {
                tagService.requireAllExist(List.of(request.subjectTagId()));
                deck.setSubjectTagId(request.subjectTagId());
            }
        }
        if (request.visibility() != null)
            deck.setVisibility(request.visibility());
        if (request.defaultSessionFormat() != null)
            deck.getContent().setFormat(request.defaultSessionFormat());
        if (request.defaultShowResponses() != null)
            deck.getContent().setShowResponses(request.defaultShowResponses());
        if (request.cover() != null) {
            deck.getContent().setCover(normalizeImage(request.cover()));
        }
        if (request.background() != null) {
            deck.getContent().setBackground(normalizeImage(request.background()));
        }
        if (request.themeId() != null) {
            deck.getContent().setThemeId(request.themeId().isEmpty() ? null : request.themeId());
        }
        if (request.estimatedDurationMinutes() != null) {
            deck.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        }
        if (request.language() != null && !request.language().isBlank()) {
            deck.setLanguage(request.language());
        }
        if (request.difficulty() != null) {
            deck.setDifficulty(request.difficulty());
        }
        if (request.ageRange() != null) {
            // Empty string clears the audience range.
            deck.setAgeRange(request.ageRange().isBlank() ? null : request.ageRange());
        }
        if (request.license() != null) {
            deck.setLicense(request.license());
        }
        deck.setVersion(deck.getVersion() + 1);
        return deckRepository.save(deck);
    }

    public void deleteDeck(String id, User caller) {
        Deck deck = requireOwned(id, caller);
        deckRepository.delete(deck);
        deckCollaboratorService.onDeckDeleted(deck.getId());
    }

    /**
     * Decks the user can edit — owned (OWNER) plus EDITOR-shared. Returned as
     * full {@link Deck} documents in insertion order; callers handle hydration.
     */
    public List<Deck> listEditableByUser(String userId) {
        List<String> editableIds = deckCollaboratorService.editableDeckIdsFor(userId);
        if (editableIds.isEmpty())
            return List.of();
        List<Deck> decks = new ArrayList<>();
        for (Deck deck : deckRepository.findAllById(editableIds))
            decks.add(deck);
        return decks;
    }

    /**
     * Decks shared with the user — all roles except OWNER. Used by the
     * "Shared with me" tab in My Decks.
     */
    public List<Deck> listSharedWithUser(String userId) {
        List<cephadex.brainflex.model.deck.DeckCollaborator> rows = deckCollaboratorService.findAllByUser(userId);
        List<String> sharedIds = new ArrayList<>();
        for (cephadex.brainflex.model.deck.DeckCollaborator row : rows) {
            if (row.getRole() != cephadex.brainflex.model.enums.CollaboratorRole.OWNER) {
                sharedIds.add(row.getDeckId());
            }
        }
        if (sharedIds.isEmpty())
            return List.of();
        List<Deck> decks = new ArrayList<>();
        for (Deck deck : deckRepository.findAllById(sharedIds))
            decks.add(deck);
        return decks;
    }

    // ---- Publish lifecycle ----

    /**
     * Flip a deck from DRAFT/ARCHIVED to PUBLISHED. Stamps {@code publishedAt}
     * only on the first DRAFT → PUBLISHED transition so the original ship
     * date survives later unpublish/republish cycles.
     */
    public Deck publish(String id, User caller) {
        Deck deck = requireOwned(id, caller);
        if (deck.getPublishStatus() == PublishStatus.PUBLISHED)
            return deck;
        deck.setPublishStatus(PublishStatus.PUBLISHED);
        if (deck.getPublishedAt() == null) {
            deck.setPublishedAt(Instant.now());
        }
        Deck saved = deckRepository.save(deck);
        // Chunk 17 — DECKS_PUBLISHED counter advances every time a draft
        // crosses into PUBLISHED. Re-publishing an already-published deck is
        // a no-op above so we don't double-count.
        long publishedCount = deckRepository.countByCreatorUserIdAndPublishStatus(
                caller.getId(), PublishStatus.PUBLISHED);
        achievementService.evaluate(caller.getId(), AchievementTrigger.DECKS_PUBLISHED,
                (int) Math.min(publishedCount, Integer.MAX_VALUE), null, saved.getId());
        return saved;
    }

    /** Move a deck back to DRAFT. {@code publishedAt} is preserved as history. */
    public Deck unpublish(String id, User caller) {
        Deck deck = requireOwned(id, caller);
        if (deck.getPublishStatus() == PublishStatus.DRAFT)
            return deck;
        deck.setPublishStatus(PublishStatus.DRAFT);
        return deckRepository.save(deck);
    }

    /**
     * Archive removes a deck from Explore and the owner's primary list without
     * deleting it. Past InteractiveSessions and ratings still resolve by id.
     */
    public Deck archive(String id, User caller) {
        Deck deck = requireOwned(id, caller);
        if (deck.getPublishStatus() == PublishStatus.ARCHIVED)
            return deck;
        deck.setPublishStatus(PublishStatus.ARCHIVED);
        return deckRepository.save(deck);
    }

    // ---- Explore ----

    public record ExplorePage(List<Deck> items, long totalElements) {
    }

    /**
     * Server-side filtered + sorted page of decks for the Explore grid. Only
     * PUBLIC + PUBLISHED decks are eligible — visibility=ORG/UNLISTED/PRIVATE
     * never leak here even if their owner has marked them published.
     *
     * Sort options route to the matching index suffix; {@code TRENDING} ranks
     * by {@code lastPlayedAt} (recently active) and falls back to playCount
     * for ties so a freshly published deck with no plays still surfaces.
     */
    public ExplorePage explore(DeckExploreRequest request) {
        Criteria criteria = Criteria.where("visibility").is(DeckVisibility.PUBLIC)
                .and("publishStatus").is(PublishStatus.PUBLISHED);
        if (request.tagId() != null && !request.tagId().isBlank()) {
            // Match a tag in either the multi-select list or the primary subject.
            criteria = criteria.orOperator(
                    Criteria.where("tagIds").is(request.tagId()),
                    Criteria.where("subjectTagId").is(request.tagId()));
        }
        if (request.language() != null && !request.language().isBlank()) {
            criteria = criteria.and("language").is(request.language());
        }
        if (request.difficulty() != null) {
            criteria = criteria.and("difficulty").is(request.difficulty());
        }
        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, Deck.class);

        Query query = new Query(criteria);
        query.with(sortFor(request.sort()));
        int page = Math.max(0, request.page());
        int size = Math.max(1, Math.min(50, request.size()));
        query.skip((long) page * size).limit(size);

        List<Deck> items = mongoTemplate.find(query, Deck.class);
        return new ExplorePage(items, total);
    }

    private static org.springframework.data.domain.Sort sortFor(DeckExploreRequest.Sort sort) {
        DeckExploreRequest.Sort resolved = sort == null ? DeckExploreRequest.Sort.TRENDING : sort;
        return switch (resolved) {
            case NEW -> org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Order.desc("publishedAt"),
                            org.springframework.data.domain.Sort.Order.desc("createdAt"));
            case TOP_RATED -> org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Order.desc("averageRating"),
                            org.springframework.data.domain.Sort.Order.desc("ratingCount"));
            case MOST_PLAYED -> org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Order.desc("playCount"));
            case TRENDING -> org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Order.desc("lastPlayedAt"),
                            org.springframework.data.domain.Sort.Order.desc("playCount"));
        };
    }

    // ---- Denormalized counter writes (atomic) ----

    /**
     * Atomically bump {@code playCount} and stamp {@code lastPlayedAt}. Called
     * from InteractiveSessionService.endGame so two interactiveSessions finishing
     * on the same
     * deck simultaneously don't lose a count via read-modify-write.
     */
    public void incrementPlayCount(String deckId) {
        if (deckId == null || deckId.isBlank())
            return;
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(deckId)),
                new Update()
                        .inc("playCount", 1)
                        .set("lastPlayedAt", Instant.now()),
                Deck.class);
    }

    /**
     * Atomically bump {@code viewCount}. Called from DeckController.getDeck
     * for non-owner reads only — owner traffic would skew the counter and
     * isn't what the Explore "trending" sort wants to surface.
     */
    public void incrementViewCount(String deckId) {
        if (deckId == null || deckId.isBlank())
            return;
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(deckId)),
                new Update().inc("viewCount", 1),
                Deck.class);
    }

    // ---- Element CRUD (operates on Deck.elements directly) ----

    /**
     * Append an element to the end of the deck. Assigns a server-side id if
     * missing.
     */
    public Deck addElement(String deckId, User caller, DeckElement incoming) {
        Deck deck = requireOwned(deckId, caller);
        DeckElement withId = ensureElementId(incoming);
        DeckElement normalized = DeckImageMapper.mapElement(withId, DeckService::stripTransportUrl);
        Instant now = Instant.now();
        // Backend is the authority for provenance — overwrite whatever the
        // client sent so a misbehaving payload can't lie about createdBy.
        DeckElement stamped = DeckElementCloner.withMetadata(
                normalized,
                caller.getId(), caller.getId(), now, now,
                tagIdsOrEmpty(normalized.tagIds()),
                normalized.mediaCaption(), normalized.altText(),
                normalized.reactionsEnabled(), 1);
        deck.getContent().getElements().add(stamped);
        return deckRepository.save(deck);
    }

    /**
     * Replace the element with matching id; throws 404 if not found in the deck.
     */
    public Deck updateElement(String deckId, String elementId, User caller, DeckElement incoming) {
        Deck deck = requireOwned(deckId, caller);
        int idx = indexOfElement(deck, elementId);
        // Preserve the id even if the client omits it on update.
        DeckElement withId = ensureElementId(incoming);
        DeckElement normalized = DeckImageMapper.mapElement(withId, DeckService::stripTransportUrl);
        DeckElement existing = deck.getContent().getElements().get(idx);
        Instant now = Instant.now();
        // Preserve original creator + createdAt; bump version off the stored
        // record so concurrent edits land at sequential versions even if the
        // client lagged behind by one.
        String createdBy = existing.createdByUserId() != null
                ? existing.createdByUserId()
                : caller.getId();
        Instant createdAt = existing.createdAt() != null
                ? existing.createdAt()
                : now;
        Integer nextVersion = (existing.version() == null ? 0 : existing.version()) + 1;
        DeckElement stamped = DeckElementCloner.withMetadata(
                normalized,
                createdBy, caller.getId(), createdAt, now,
                tagIdsOrEmpty(normalized.tagIds()),
                normalized.mediaCaption(), normalized.altText(),
                normalized.reactionsEnabled(), nextVersion);
        deck.getContent().getElements().set(idx, stamped);
        return deckRepository.save(deck);
    }

    private static List<String> tagIdsOrEmpty(List<String> tagIds) {
        return tagIds == null ? List.of() : tagIds;
    }

    public Deck deleteElement(String deckId, String elementId, User caller) {
        Deck deck = requireOwned(deckId, caller);
        int idx = indexOfElement(deck, elementId);
        deck.getContent().getElements().remove(idx);
        return deckRepository.save(deck);
    }

    /**
     * Move the element with `elementId` to position `targetIndex` (clamped to deck
     * size).
     */
    public Deck moveElement(String deckId, String elementId, int targetIndex, User caller) {
        Deck deck = requireOwned(deckId, caller);
        int currentIdx = indexOfElement(deck, elementId);
        int clamped = Math.max(0, Math.min(targetIndex, deck.getContent().getElements().size() - 1));
        if (currentIdx == clamped)
            return deck;
        DeckElement element = deck.getContent().getElements().remove(currentIdx);
        deck.getContent().getElements().add(clamped, element);
        return deckRepository.save(deck);
    }

    /**
     * Move one option inside an MCQ question to position `targetIndex` (clamped to
     * the option-list size). The targeted element must be an `McqQuestion`; any
     * other kind throws 400. `correctOptionIds` is untouched — option ids are the
     * scoring key, so reordering never affects which options are correct.
     */
    public Deck moveMcqOption(
            String deckId, String elementId, String optionId, int targetIndex, User caller) {
        Deck deck = requireOwned(deckId, caller);
        int elementIdx = indexOfElement(deck, elementId);
        DeckElement element = deck.getContent().getElements().get(elementIdx);
        if (!(element instanceof McqQuestion mcq)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Element is not an MCQ question");
        }
        List<McqOption> options = new ArrayList<>(
                mcq.options() == null ? List.of() : mcq.options());
        int currentIdx = -1;
        for (int i = 0; i < options.size(); i++) {
            if (optionId.equals(options.get(i).id())) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Option not in this question");
        }
        int clamped = Math.max(0, Math.min(targetIndex, options.size() - 1));
        if (currentIdx == clamped)
            return deck;
        McqOption moved = options.remove(currentIdx);
        options.add(clamped, moved);
        deck.getContent().getElements().set(elementIdx, DeckElementCloner.withOptions(mcq, options));
        return deckRepository.save(deck);
    }

    // ---- Helpers ----

    /**
     * For internal images the variant list is a transient transport field that
     * the hydrator regenerates on read — never persist whatever the client
     * sent for it. External images are passed through untouched.
     */
    private static Image stripTransportUrl(Image image) {
        if (image.useExternalImg())
            return image;
        return image.withVariants(java.util.Map.of());
    }

    /** Same rule for top-level Deck.cover / Deck.background. */
    private static Image normalizeImage(Image image) {
        if (image == null)
            return null;
        return stripTransportUrl(image);
    }

    private Deck requireOwned(String deckId, User caller) {
        return authorizationService.requireDeckEditable(deckId, caller);
    }

    private int indexOfElement(Deck deck, String elementId) {
        Optional<Integer> idx = findIndex(deck.getContent().getElements(), elementId);
        return idx.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not in this deck"));
    }

    private static Optional<Integer> findIndex(List<DeckElement> elements, String elementId) {
        for (int i = 0; i < elements.size(); i++) {
            if (elementId.equals(elements.get(i).id()))
                return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * Ensures the element has a stable id — generates one if the client omitted it.
     */
    private static DeckElement ensureElementId(DeckElement element) {
        if (element.id() != null && !element.id().isBlank())
            return element;
        String newId = UUID.randomUUID().toString();
        return DeckElementCloner.withId(element, newId);
    }
}
