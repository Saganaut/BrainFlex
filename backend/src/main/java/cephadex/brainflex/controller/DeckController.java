/**
 * REST endpoints for deck discovery + CRUD, plus element-CRUD nested under a deck.
 *
 * Read endpoints are public. Write endpoints require ROLE_USER and the service layer
 * enforces ownership — callers that don't own the deck receive 403.
 *
 * Element CRUD takes the polymorphic `DeckElement` directly as the request body;
 * Jackson uses the `kind` discriminator to instantiate the right subtype.
 */
package cephadex.brainflex.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateDeckRequest;
import cephadex.brainflex.dto.DeckDTO;
import cephadex.brainflex.dto.DeckExploreRequest;
import cephadex.brainflex.dto.DeckExploreResponse;
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final UserService userService;
    private final DeckImageHydrationService deckImageHydrationService;
    private final DeckTagHydrationService deckTagHydrationService;

    public DeckController(
            DeckService deckService,
            UserService userService,
            DeckImageHydrationService deckImageHydrationService,
            DeckTagHydrationService deckTagHydrationService) {
        this.deckService = deckService;
        this.userService = userService;
        this.deckImageHydrationService = deckImageHydrationService;
        this.deckTagHydrationService = deckTagHydrationService;
    }

    /** All public decks. Used by the create-showcase template picker. */
    @GetMapping
    public List<DeckDTO> listDecks() {
        List<Deck> decks = deckService.listPublic();
        deckTagHydrationService.hydrate(decks);
        return decks.stream().map(DeckDTO::new).toList();
    }

    /**
     * Paginated discovery feed. Returns PUBLIC + PUBLISHED decks only,
     * filterable by tagId / language / difficulty, sortable by trending /
     * new / top-rated / most-played. Public — no auth required, in line
     * with the rest of the deck-read surface.
     */
    @GetMapping("/explore")
    public DeckExploreResponse exploreDecks(
            @RequestParam(name = "tagId", required = false) String tagId,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "difficulty", required = false) Difficulty difficulty,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        DeckExploreRequest request = new DeckExploreRequest(
                tagId, language, difficulty,
                DeckExploreRequest.Sort.parse(sort),
                page, size);
        DeckService.ExplorePage result = deckService.explore(request);
        deckTagHydrationService.hydrate(result.items());
        List<DeckDTO> items = result.items().stream().map(DeckDTO::new).toList();
        boolean hasMore = (long) (page + 1) * size < result.totalElements();
        return new DeckExploreResponse(items, page, size, result.totalElements(), hasMore);
    }

    /** Decks owned by the authenticated user. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public List<DeckDTO> listMyDecks(Authentication authentication) {
        User caller = resolveUser(authentication);
        List<Deck> decks = deckService.listByOwner(caller.getId());
        deckTagHydrationService.hydrate(decks);
        return decks.stream().map(DeckDTO::new).toList();
    }

    @GetMapping("/{id}")
    public DeckDTO getDeck(@PathVariable String id, Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        Deck deck = deckService.getViewable(caller, id);
        // Non-owner traffic bumps viewCount so Explore's "trending" sort
        // surfaces decks people are actually opening. Owner views and
        // anonymous owner-less queries are excluded — the former skew their
        // own numbers, the latter are most often the owner's optimistic
        // create flow. System decks also opt out: their viewCount is
        // dominated by the welcome tour and not meaningful for sorting.
        boolean isOwner = caller.map(u -> u.getId() != null && u.getId().equals(deck.getCreatorUserId()))
                .orElse(false);
        if (!isOwner && !deck.isSystem()) {
            deckService.incrementViewCount(deck.getId());
        }
        deckImageHydrationService.hydrate(deck);
        deckTagHydrationService.hydrate(deck);
        return new DeckDTO(deck);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hydrateAndWrap(deckService.createDeck(caller, request)));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public DeckDTO updateDeck(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.updateDeck(id, caller, request));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckService.deleteDeck(id, caller);
        return ResponseEntity.noContent().build();
    }

    // ---- Publish lifecycle ----

    /** Flip the deck to PUBLISHED (stamps publishedAt the first time). */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/publish")
    public DeckDTO publishDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.publish(id, caller));
    }

    /** Flip the deck back to DRAFT. publishedAt is preserved as history. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/unpublish")
    public DeckDTO unpublishDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.unpublish(id, caller));
    }

    /** Move the deck to ARCHIVED — hidden from Explore + my-decks list. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/archive")
    public DeckDTO archiveDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.archive(id, caller));
    }

    // ---- Element CRUD ----

    /**
     * Append a new element to the deck. The polymorphic body picks its subtype via
     * `kind`.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/elements")
    public ResponseEntity<DeckDTO> addElement(
            @PathVariable String id,
            @RequestBody DeckElement element,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hydrateAndWrap(deckService.addElement(id, caller, element)));
    }

    /** Replace an element by id. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/elements/{elementId}")
    public DeckDTO updateElement(
            @PathVariable String id,
            @PathVariable String elementId,
            @RequestBody DeckElement element,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.updateElement(id, elementId, caller, element));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/elements/{elementId}")
    public DeckDTO deleteElement(
            @PathVariable String id,
            @PathVariable String elementId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.deleteElement(id, elementId, caller));
    }

    /** Move an element to a new position within the deck. ?to=<index> */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/elements/{elementId}/move")
    public DeckDTO moveElement(
            @PathVariable String id,
            @PathVariable String elementId,
            @RequestParam("to") int to,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.moveElement(id, elementId, to, caller));
    }

    /**
     * Move one option inside an MCQ question to a new index. ?to=<index>
     * Avoids re-sending the entire McqQuestion just to reorder its options.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/elements/{elementId}/options/{optionId}/move")
    public DeckDTO moveMcqOption(
            @PathVariable String id,
            @PathVariable String elementId,
            @PathVariable String optionId,
            @RequestParam("to") int to,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(
                deckService.moveMcqOption(id, elementId, optionId, to, caller));
    }

    /**
     * Run every mutation response through the same hydration pipeline that
     * `getDeck` uses, so the client receives presigned `imgUrl` values on
     * internal images instead of nulls. Without this the editor would have to
     * carry old hydrated URLs forward in its cache.
     */
    private DeckDTO hydrateAndWrap(Deck deck) {
        deckImageHydrationService.hydrate(deck);
        deckTagHydrationService.hydrate(deck);
        return new DeckDTO(deck);
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }
}
