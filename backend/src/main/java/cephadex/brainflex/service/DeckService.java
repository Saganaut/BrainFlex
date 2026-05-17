/**
 * Business logic for browsing, creating, and editing decks + their embedded elements.
 *
 * Elements live inside the deck document, so every "question CRUD" operation is a
 * targeted edit on Deck.elements followed by a single save. Element order is the
 * natural list order — reordering is just moving an item to a new index.
 *
 * All mutation methods enforce ownership; system decks (`isSystem=true`) and decks
 * owned by another user are off-limits.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateDeckRequest;
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.ImageChoiceQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.repository.DeckRepository;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final AuthorizationService authorizationService;

    public DeckService(DeckRepository deckRepository, AuthorizationService authorizationService) {
        this.deckRepository = deckRepository;
        this.authorizationService = authorizationService;
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
     *   PUBLIC, UNLISTED  → anyone with the id
     *   ORG               → registered users in the same organization
     *   PRIVATE           → the owner only
     * Unmet visibility rules throw 401 (no caller) or 403 (caller, wrong scope).
     */
    public Deck getViewable(Optional<User> caller, String id) {
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        DeckVisibility visibility = deck.getVisibility();
        if (visibility == DeckVisibility.PUBLIC || visibility == DeckVisibility.UNLISTED) {
            return deck;
        }
        User user = caller.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to view this deck"));
        if (visibility == DeckVisibility.ORG) {
            String deckOrgId = deck.getOrganizationId();
            var memberships = user.getOrganizationIds();
            if (deckOrgId != null && memberships != null && memberships.contains(deckOrgId)) {
                return deck;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deck is restricted to its organization");
        }
        if (user.getId().equals(deck.getCreatorUserId())) {
            return deck;
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
        deck.setName(request.name());
        deck.setDescription(request.description());
        deck.setTags(request.tags() == null ? new ArrayList<>() : request.tags());
        deck.setVisibility(request.visibility() == null ? DeckVisibility.PRIVATE : request.visibility());
        deck.setRecommendedPreset(request.recommendedPreset() == null ? DeckPreset.GAME : request.recommendedPreset());
        deck.setCoverImageUrl(request.coverImageUrl());
        deck.setBackgroundImageUrl(request.backgroundImageUrl());
        deck.setThemeId(request.themeId());
        deck.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        deck.setSystem(false);

        deck.setCreatorUserId(creator.getId());
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    public Deck updateDeck(String id, User caller, UpdateDeckRequest request) {
        Deck deck = requireOwned(id, caller);
        if (request.name() != null)
            deck.setName(request.name());
        if (request.description() != null)
            deck.setDescription(request.description());
        if (request.tags() != null)
            deck.setTags(request.tags());
        if (request.visibility() != null)
            deck.setVisibility(request.visibility());
        if (request.recommendedPreset() != null)
            deck.setRecommendedPreset(request.recommendedPreset());
        // empty string clears; null leaves alone
        if (request.coverImageUrl() != null) {
            deck.setCoverImageUrl(request.coverImageUrl().isEmpty() ? null : request.coverImageUrl());
        }
        if (request.backgroundImageUrl() != null) {
            deck.setBackgroundImageUrl(request.backgroundImageUrl().isEmpty() ? null : request.backgroundImageUrl());
        }
        if (request.themeId() != null) {
            deck.setThemeId(request.themeId().isEmpty() ? null : request.themeId());
        }
        if (request.estimatedDurationMinutes() != null) {
            deck.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        }
        deck.setUpdatedAt(LocalDateTime.now());
        deck.setVersion(deck.getVersion() + 1);
        return deckRepository.save(deck);
    }

    public void deleteDeck(String id, User caller) {
        Deck deck = requireOwned(id, caller);
        deckRepository.delete(deck);
    }

    // ---- Element CRUD (operates on Deck.elements directly) ----

    /**
     * Append an element to the end of the deck. Assigns a server-side id if
     * missing.
     */
    public Deck addElement(String deckId, User caller, DeckElement incoming) {
        Deck deck = requireOwned(deckId, caller);
        validateOptionImages(incoming);
        DeckElement withId = ensureElementId(incoming);
        deck.getElements().add(withId);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    /**
     * Replace the element with matching id; throws 404 if not found in the deck.
     */
    public Deck updateElement(String deckId, String elementId, User caller, DeckElement incoming) {
        Deck deck = requireOwned(deckId, caller);
        validateOptionImages(incoming);
        int idx = indexOfElement(deck, elementId);
        // Preserve the id even if the client omits it on update.
        DeckElement withId = ensureElementId(incoming);
        if (!elementId.equals(withId.id())) {
            // Different ids — caller is trying to swap one element for another; treat as
            // PUT semantics.
        }
        deck.getElements().set(idx, withId);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    /**
     * `McqOption.galleryImageId` and `McqOption.imageUrl` are mutually exclusive
     * on write — the renderer treats `galleryImageId` as the source of truth
     * (hydrated at read time) and `imageUrl` as the external/paste-link path.
     * Carrying both is a smell that we'd silently resolve one direction or the
     * other; reject it at the boundary instead.
     */
    private static void validateOptionImages(DeckElement element) {
        List<McqOption> options = switch (element) {
            case McqQuestion q -> q.options();
            case ImageChoiceQuestion q -> q.options();
            default -> null;
        };
        if (options == null) return;
        for (McqOption option : options) {
            boolean hasGallery = option.galleryImageId() != null && !option.galleryImageId().isBlank();
            boolean hasUrl = option.imageUrl() != null && !option.imageUrl().isBlank();
            if (hasGallery && hasUrl) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Option may set either galleryImageId or imageUrl, not both");
            }
        }
    }

    public Deck deleteElement(String deckId, String elementId, User caller) {
        Deck deck = requireOwned(deckId, caller);
        int idx = indexOfElement(deck, elementId);
        deck.getElements().remove(idx);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    /**
     * Move the element with `elementId` to position `targetIndex` (clamped to deck
     * size).
     */
    public Deck moveElement(String deckId, String elementId, int targetIndex, User caller) {
        Deck deck = requireOwned(deckId, caller);
        int currentIdx = indexOfElement(deck, elementId);
        int clamped = Math.max(0, Math.min(targetIndex, deck.getElements().size() - 1));
        if (currentIdx == clamped)
            return deck;
        DeckElement element = deck.getElements().remove(currentIdx);
        deck.getElements().add(clamped, element);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    // ---- Helpers ----

    private Deck requireOwned(String deckId, User caller) {
        return authorizationService.requireDeckEditable(deckId, caller);
    }

    private int indexOfElement(Deck deck, String elementId) {
        Optional<Integer> idx = findIndex(deck.getElements(), elementId);
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
