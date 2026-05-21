/**
 * REST endpoints for the {@link cephadex.brainflex.model.DeckCollection}
 * surface — owner-scoped CRUD plus the four list-mutation paths (add deck,
 * remove deck, reorder, soft-update metadata).
 *
 * Read endpoints follow the same shape as the deck controller: anonymous
 * callers can fetch PUBLIC / UNLISTED collections, authenticated callers
 * additionally see their own PRIVATE ones plus any ORG collection scoped to
 * an org they're in. Writes always require {@code ROLE_USER} and the service
 * layer enforces ownership.
 *
 * Cover images are hydrated by {@link DeckCollectionService#hydrateCover} so
 * every response carries a fresh presigned URL on internal images. Detail
 * responses also hydrate each embedded deck via
 * {@link DeckImageHydrationService} — collections are routinely linked from
 * deck-card grids, and stale URLs would make the grid render broken
 * thumbnails on the second visit.
 */
package cephadex.brainflex.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.AddDeckToCollectionRequest;
import cephadex.brainflex.dto.CreateDeckCollectionRequest;
import cephadex.brainflex.dto.DeckCollectionDTO;
import cephadex.brainflex.dto.DeckDTO;
import cephadex.brainflex.dto.Page;
import cephadex.brainflex.dto.ReorderCollectionDecksRequest;
import cephadex.brainflex.dto.UpdateDeckCollectionRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckCollection;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.DeckCollectionService;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/collections")
public class DeckCollectionController {

    private final DeckCollectionService collectionService;
    private final UserService userService;
    private final DeckImageHydrationService deckImageHydrationService;
    private final DeckTagHydrationService deckTagHydrationService;
    private final DeckFavoriteService deckFavoriteService;

    public DeckCollectionController(
            DeckCollectionService collectionService,
            UserService userService,
            DeckImageHydrationService deckImageHydrationService,
            DeckTagHydrationService deckTagHydrationService,
            DeckFavoriteService deckFavoriteService) {
        this.collectionService = collectionService;
        this.userService = userService;
        this.deckImageHydrationService = deckImageHydrationService;
        this.deckTagHydrationService = deckTagHydrationService;
        this.deckFavoriteService = deckFavoriteService;
    }

    /** Paginated list of collections owned by the caller, newest-first. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public Page<DeckCollectionDTO> listMyCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(
                safePage, safeSize, Sort.by("updatedAt").descending());
        org.springframework.data.domain.Page<DeckCollection> rows = collectionService.listForOwner(caller.getId(), pageRequest);
        List<DeckCollectionDTO> items = new ArrayList<>(rows.getNumberOfElements());
        for (DeckCollection col : rows.getContent()) {
            collectionService.hydrateCover(col);
            items.add(DeckCollectionDTO.summary(col));
        }
        boolean hasMore = (long) (safePage + 1) * safeSize < rows.getTotalElements();
        return new Page<>(items, safePage, safeSize, rows.getTotalElements(), hasMore);
    }

    /**
     * Detail view: collection metadata + embedded deck DTOs for every deck in
     * the ordered list the caller can see. Non-owner reads bump the view
     * counter so a future "popular collections" sort has data to work with.
     */
    @GetMapping("/{id}")
    public DeckCollectionDTO getCollection(@PathVariable String id, Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        DeckCollection col = collectionService.getViewable(caller, id);
        boolean isOwner = caller.map(u -> u.getId().equals(col.getOwnerUserId())).orElse(false);
        if (!isOwner) {
            collectionService.incrementViewCount(col.getId());
        }
        collectionService.hydrateCover(col);
        List<Deck> decks = collectionService.resolveDecks(col, caller);
        for (Deck deck : decks) {
            deckImageHydrationService.hydrate(deck);
        }
        deckTagHydrationService.hydrate(decks);
        Set<String> favorites = caller
                .map(u -> deckFavoriteService.favoritedDeckIds(u.getId(), idsOf(decks)))
                .orElse(Set.of());
        List<DeckDTO> deckDtos = new ArrayList<>(decks.size());
        for (Deck deck : decks) {
            deckDtos.add(new DeckDTO(deck, favorites.contains(deck.getId())));
        }
        return DeckCollectionDTO.detail(col, deckDtos);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<DeckCollectionDTO> createCollection(
            @Valid @RequestBody CreateDeckCollectionRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckCollection col = collectionService.create(caller, request);
        collectionService.hydrateCover(col);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeckCollectionDTO.summary(col));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public DeckCollectionDTO updateCollection(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeckCollectionRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckCollection col = collectionService.update(id, caller, request);
        collectionService.hydrateCover(col);
        return DeckCollectionDTO.summary(col);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        collectionService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/decks")
    public DeckCollectionDTO addDeckToCollection(
            @PathVariable String id,
            @Valid @RequestBody AddDeckToCollectionRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckCollection col = collectionService.addDeck(id, caller, request.deckId(), request.position());
        collectionService.hydrateCover(col);
        return DeckCollectionDTO.summary(col);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/decks/{deckId}")
    public DeckCollectionDTO removeDeckFromCollection(
            @PathVariable String id,
            @PathVariable String deckId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckCollection col = collectionService.removeDeck(id, caller, deckId);
        collectionService.hydrateCover(col);
        return DeckCollectionDTO.summary(col);
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{id}/decks")
    public DeckCollectionDTO reorderCollectionDecks(
            @PathVariable String id,
            @Valid @RequestBody ReorderCollectionDecksRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckCollection col = collectionService.reorderDecks(id, caller, request.deckIds());
        collectionService.hydrateCover(col);
        return DeckCollectionDTO.summary(col);
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }

    private static List<String> idsOf(List<Deck> decks) {
        List<String> ids = new ArrayList<>(decks.size());
        for (Deck d : decks) ids.add(d.getId());
        return ids;
    }
}
