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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import cephadex.brainflex.config.AdminProperties;
import cephadex.brainflex.dto.CreateCommentRequest;
import cephadex.brainflex.dto.CreateDeckRequest;
import cephadex.brainflex.dto.DeckCommentDTO;
import cephadex.brainflex.dto.DeckCommentsPage;
import cephadex.brainflex.dto.DeckDTO;
import cephadex.brainflex.dto.DeckExploreRequest;
import cephadex.brainflex.dto.DeckExploreResponse;
import cephadex.brainflex.dto.DeckFavoriteResponse;
import cephadex.brainflex.dto.DeckRatingDTO;
import cephadex.brainflex.dto.DeckRatingsPage;
import cephadex.brainflex.dto.RateDeckRequest;
import cephadex.brainflex.dto.UpdateCommentRequest;
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckComment;
import cephadex.brainflex.model.DeckRating;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.DeckCommentService;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckRatingService;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserImageHydrator;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final UserService userService;
    private final DeckImageHydrationService deckImageHydrationService;
    private final DeckTagHydrationService deckTagHydrationService;
    private final DeckFavoriteService deckFavoriteService;
    private final DeckRatingService deckRatingService;
    private final DeckCommentService deckCommentService;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final AdminProperties adminProperties;
    private final UserImageHydrator userImageHydrator;

    public DeckController(
            DeckService deckService,
            UserService userService,
            DeckImageHydrationService deckImageHydrationService,
            DeckTagHydrationService deckTagHydrationService,
            DeckFavoriteService deckFavoriteService,
            DeckRatingService deckRatingService,
            DeckCommentService deckCommentService,
            DeckRepository deckRepository,
            UserRepository userRepository,
            AdminProperties adminProperties,
            UserImageHydrator userImageHydrator) {
        this.deckService = deckService;
        this.userService = userService;
        this.deckImageHydrationService = deckImageHydrationService;
        this.deckTagHydrationService = deckTagHydrationService;
        this.deckFavoriteService = deckFavoriteService;
        this.deckRatingService = deckRatingService;
        this.deckCommentService = deckCommentService;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.adminProperties = adminProperties;
        this.userImageHydrator = userImageHydrator;
    }

    /** All public decks. Used by the create-showcase template picker. */
    @GetMapping
    public List<DeckDTO> listDecks(Authentication authentication) {
        List<Deck> decks = deckService.listPublic();
        deckTagHydrationService.hydrate(decks);
        Set<String> favorites = resolveFavoritedDeckIds(authentication, decks);
        return decks.stream().map(d -> new DeckDTO(d, favorites.contains(d.getId()))).toList();
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
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        DeckExploreRequest request = new DeckExploreRequest(
                tagId, language, difficulty,
                DeckExploreRequest.Sort.parse(sort),
                page, size);
        DeckService.ExplorePage result = deckService.explore(request);
        deckTagHydrationService.hydrate(result.items());
        Set<String> favorites = resolveFavoritedDeckIds(authentication, result.items());
        List<DeckDTO> items = result.items().stream()
                .map(d -> new DeckDTO(d, favorites.contains(d.getId())))
                .toList();
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
        Set<String> favorites = deckFavoriteService.favoritedDeckIds(caller.getId(), idsOf(decks));
        return decks.stream().map(d -> new DeckDTO(d, favorites.contains(d.getId()))).toList();
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
        boolean isFavorited = caller
                .map(u -> deckFavoriteService.isFavorited(u.getId(), deck.getId()))
                .orElse(false);
        Integer myRating = caller
                .flatMap(u -> deckRatingService.findMine(deck.getId(), u.getId()))
                .map(DeckRating::getStars)
                .orElse(null);
        return new DeckDTO(deck, isFavorited, myRating);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hydrateAndWrap(deckService.createDeck(caller, request), caller));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public DeckDTO updateDeck(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.updateDeck(id, caller, request), caller);
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
        return hydrateAndWrap(deckService.publish(id, caller), caller);
    }

    /** Flip the deck back to DRAFT. publishedAt is preserved as history. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/unpublish")
    public DeckDTO unpublishDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.unpublish(id, caller), caller);
    }

    /** Move the deck to ARCHIVED — hidden from Explore + my-decks list. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/archive")
    public DeckDTO archiveDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.archive(id, caller), caller);
    }

    // ---- Favorites ----

    /**
     * Toggle on: idempotent favorite of {@code id} by the caller. Returns the
     * resulting state so the optimistic-toggle cache update can reconcile
     * with the server value in one round-trip.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/favorite")
    public DeckFavoriteResponse favoriteDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        // Confirm the deck actually exists / the caller can see it before we
        // accept a favorite — otherwise we'd accumulate dangling join rows
        // pointing at nothing.
        deckService.getViewable(Optional.of(caller), id);
        long count = deckFavoriteService.favorite(caller.getId(), id);
        return new DeckFavoriteResponse(id, true, count);
    }

    /** Toggle off: idempotent unfavorite of {@code id} by the caller. */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/favorite")
    public DeckFavoriteResponse unfavoriteDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        long count = deckFavoriteService.unfavorite(caller.getId(), id);
        return new DeckFavoriteResponse(id, false, count);
    }

    /**
     * Admin-only reconciliation: recompute {@code Deck.favoriteCount} from
     * the authoritative count of join rows. Useful when the denorm drifts
     * after a manual delete or bug.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/favorite/recount")
    public DeckFavoriteResponse recountFavorites(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        if (!adminProperties.isAdmin(caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        // Validate existence first so we don't silently zero a missing deck.
        if (!deckRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found");
        }
        long count = deckFavoriteService.recountFavorites(id);
        boolean isFavorited = deckFavoriteService.isFavorited(caller.getId(), id);
        return new DeckFavoriteResponse(id, isFavorited, count);
    }

    // ---- Ratings ----

    /**
     * Upsert the caller's rating for a deck. Returns the persisted row so the
     * client can echo the new star count without a second fetch.
     */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/rating")
    public DeckRatingDTO rateDeck(
            @PathVariable String id,
            @Valid @RequestBody RateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        // Confirm the deck is viewable before we attach a rating — otherwise
        // we'd accumulate dangling rating rows pointing at decks the caller
        // shouldn't be able to see.
        deckService.getViewable(Optional.of(caller), id);
        DeckRating row = deckRatingService.upsert(id, caller.getId(), request.stars(), request.review());
        return DeckRatingDTO.of(row, caller.getUserName(), userImageHydrator.pictureUrlOf(caller));
    }

    /**
     * Remove the caller's rating, if any. 204 either way — the operation is
     * idempotent.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/rating")
    public ResponseEntity<Void> deleteMyRating(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckRatingService.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }

    /** Caller's own rating for this deck, or 404 if they haven't rated it. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}/rating/mine")
    public DeckRatingDTO getMyRating(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckRating row = deckRatingService.findMine(id, caller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No rating yet"));
        return DeckRatingDTO.of(row, caller.getUserName(), userImageHydrator.pictureUrlOf(caller));
    }

    /**
     * Paginated list of ratings (with reviews) for a deck. Public — the same
     * audience that can see the deck can see its reviews. The histogram on
     * the response makes the rating-distribution chart render without a
     * separate aggregation call.
     */
    @GetMapping("/{id}/ratings")
    public DeckRatingsPage listRatings(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        Deck deck = deckService.getViewable(caller, id);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(
                safePage, safeSize, Sort.by("createdAt").descending());
        Page<DeckRating> rows = deckRatingService.listForDeck(id, pageRequest);
        Map<String, User> userById = lookupUsers(rows.getContent().stream().map(DeckRating::getUserId).toList());
        List<DeckRatingDTO> items = new ArrayList<>(rows.getNumberOfElements());
        for (DeckRating row : rows.getContent()) {
            User author = userById.get(row.getUserId());
            String name = author == null ? null : author.getUserName();
            String picture = author == null ? null : userImageHydrator.pictureUrlOf(author);
            items.add(DeckRatingDTO.of(row, name, picture));
        }
        boolean hasMore = (long) (safePage + 1) * safeSize < rows.getTotalElements();
        int[] distribution = buildRatingDistribution(id);
        return new DeckRatingsPage(
                items, safePage, safeSize, rows.getTotalElements(), hasMore,
                deck.getAverageRating(), deck.getRatingCount(), distribution);
    }

    // ---- Comments ----

    /** Paginated top-level comments for a deck, newest first. Public. */
    @GetMapping("/{id}/comments")
    public DeckCommentsPage listComments(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        deckService.getViewable(caller, id);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(
                safePage, safeSize, Sort.by("createdAt").descending());
        Page<DeckComment> rows = deckCommentService.listTopLevel(id, pageRequest);
        String callerId = caller.map(User::getId).orElse(null);
        List<DeckCommentDTO> items = new ArrayList<>(rows.getNumberOfElements());
        for (DeckComment row : rows.getContent()) {
            long replyCount = deckCommentService.countReplies(id, row.getId());
            items.add(DeckCommentDTO.of(row, callerId, replyCount));
        }
        boolean hasMore = (long) (safePage + 1) * safeSize < rows.getTotalElements();
        return new DeckCommentsPage(items, safePage, safeSize, rows.getTotalElements(), hasMore);
    }

    /** Paginated replies to a single top-level comment, oldest first. Public. */
    @GetMapping("/{deckId}/comments/{commentId}/replies")
    public DeckCommentsPage listReplies(
            @PathVariable String deckId,
            @PathVariable String commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        deckService.getViewable(caller, deckId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(
                safePage, safeSize, Sort.by("createdAt").ascending());
        Page<DeckComment> rows = deckCommentService.listReplies(deckId, commentId, pageRequest);
        String callerId = caller.map(User::getId).orElse(null);
        List<DeckCommentDTO> items = new ArrayList<>(rows.getNumberOfElements());
        for (DeckComment row : rows.getContent()) {
            items.add(DeckCommentDTO.of(row, callerId, 0L));
        }
        boolean hasMore = (long) (safePage + 1) * safeSize < rows.getTotalElements();
        return new DeckCommentsPage(items, safePage, safeSize, rows.getTotalElements(), hasMore);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/comments")
    public ResponseEntity<DeckCommentDTO> postComment(
            @PathVariable String id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckService.getViewable(Optional.of(caller), id);
        DeckComment row = deckCommentService.create(id, caller, request.body(), request.parentCommentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DeckCommentDTO.of(row, caller.getId(), 0L));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{deckId}/comments/{commentId}")
    public DeckCommentDTO editComment(
            @PathVariable String deckId,
            @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckComment row = deckCommentService.edit(deckId, commentId, caller, request.body());
        long replyCount = row.getParentCommentId() == null
                ? deckCommentService.countReplies(deckId, row.getId())
                : 0L;
        return DeckCommentDTO.of(row, caller.getId(), replyCount);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{deckId}/comments/{commentId}")
    public DeckCommentDTO deleteComment(
            @PathVariable String deckId,
            @PathVariable String commentId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        DeckComment row = deckCommentService.softDelete(deckId, commentId, caller);
        long replyCount = row.getParentCommentId() == null
                ? deckCommentService.countReplies(deckId, row.getId())
                : 0L;
        return DeckCommentDTO.of(row, caller.getId(), replyCount);
    }

    /**
     * Toggle the caller's upvote on a comment. Idempotent in both directions
     * — repeat clicks flip the state rather than stacking. Returns the
     * comment after the toggle so optimistic clients can reconcile.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{deckId}/comments/{commentId}/upvote")
    public DeckCommentDTO toggleCommentUpvote(
            @PathVariable String deckId,
            @PathVariable String commentId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckService.getViewable(Optional.of(caller), deckId);
        DeckComment row = deckCommentService.toggleUpvote(deckId, commentId, caller.getId());
        long replyCount = row.getParentCommentId() == null
                ? deckCommentService.countReplies(deckId, row.getId())
                : 0L;
        return DeckCommentDTO.of(row, caller.getId(), replyCount);
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
                .body(hydrateAndWrap(deckService.addElement(id, caller, element), caller));
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
        return hydrateAndWrap(deckService.updateElement(id, elementId, caller, element), caller);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/elements/{elementId}")
    public DeckDTO deleteElement(
            @PathVariable String id,
            @PathVariable String elementId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return hydrateAndWrap(deckService.deleteElement(id, elementId, caller), caller);
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
        return hydrateAndWrap(deckService.moveElement(id, elementId, to, caller), caller);
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
                deckService.moveMcqOption(id, elementId, optionId, to, caller),
                caller);
    }

    /**
     * Run every mutation response through the same hydration pipeline that
     * `getDeck` uses, so the client receives presigned `imgUrl` values on
     * internal images instead of nulls. Without this the editor would have to
     * carry old hydrated URLs forward in its cache.
     */
    private DeckDTO hydrateAndWrap(Deck deck, User caller) {
        deckImageHydrationService.hydrate(deck);
        deckTagHydrationService.hydrate(deck);
        boolean isFavorited = caller != null
                && deckFavoriteService.isFavorited(caller.getId(), deck.getId());
        Integer myRating = caller == null
                ? null
                : deckRatingService.findMine(deck.getId(), caller.getId())
                        .map(DeckRating::getStars).orElse(null);
        return new DeckDTO(deck, isFavorited, myRating);
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }

    /**
     * Resolves caller-specific {@code isFavorited} state for a list of decks
     * in one query, or returns an empty set for unauthenticated callers.
     */
    private Set<String> resolveFavoritedDeckIds(Authentication authentication, List<Deck> decks) {
        if (decks == null || decks.isEmpty()) return Set.of();
        return userService.resolveRegisteredUser(authentication)
                .map(u -> deckFavoriteService.favoritedDeckIds(u.getId(), idsOf(decks)))
                .orElse(Set.of());
    }

    private static List<String> idsOf(List<Deck> decks) {
        List<String> ids = new ArrayList<>(decks.size());
        for (Deck d : decks) ids.add(d.getId());
        return ids;
    }

    /**
     * Batch lookup of users by id, returned as a map for in-loop hydration.
     * One round-trip; missing ids are simply absent so the caller can degrade
     * gracefully (the rating row carries its own author display snapshot
     * fallback once chunk 18 lands).
     */
    private Map<String, User> lookupUsers(List<String> userIds) {
        Set<String> unique = new HashSet<>(userIds);
        unique.removeIf(s -> s == null || s.isBlank());
        if (unique.isEmpty()) return Map.of();
        Map<String, User> byId = new HashMap<>(unique.size());
        for (User u : userRepository.findAllById(unique)) byId.put(u.getId(), u);
        return byId;
    }

    /**
     * Build a 5-bucket histogram (index 0 = 1-star … index 4 = 5-star) for the
     * Reviews tab. Uses {@code Pageable.unpaged()} which is fine for the
     * expected per-deck rating volumes; if a deck ever holds tens of
     * thousands of ratings this should be swapped for a Mongo {@code $group}
     * aggregation.
     */
    private int[] buildRatingDistribution(String deckId) {
        int[] buckets = new int[5];
        Page<DeckRating> all = deckRatingService.listForDeck(
                deckId, org.springframework.data.domain.Pageable.unpaged());
        for (DeckRating r : all.getContent()) {
            int stars = r.getStars();
            if (stars >= 1 && stars <= 5) buckets[stars - 1]++;
        }
        return buckets;
    }
}
