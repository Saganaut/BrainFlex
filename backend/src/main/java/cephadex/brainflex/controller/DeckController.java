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
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final UserService userService;
    private final DeckImageHydrationService deckImageHydrationService;

    public DeckController(
            DeckService deckService,
            UserService userService,
            DeckImageHydrationService deckImageHydrationService) {
        this.deckService = deckService;
        this.userService = userService;
        this.deckImageHydrationService = deckImageHydrationService;
    }

    /** All public decks. Used by the create-showcase template picker. */
    @GetMapping
    public List<DeckDTO> listDecks() {
        return deckService.listPublic().stream().map(DeckDTO::new).toList();
    }

    /** Decks owned by the authenticated user. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public List<DeckDTO> listMyDecks(Authentication authentication) {
        User caller = resolveUser(authentication);
        return deckService.listByOwner(caller.getId()).stream().map(DeckDTO::new).toList();
    }

    @GetMapping("/{id}")
    public DeckDTO getDeck(@PathVariable String id, Authentication authentication) {
        Optional<User> caller = userService.resolveRegisteredUser(authentication);
        Deck deck = deckService.getViewable(caller, id);
        deckImageHydrationService.hydrate(deck);
        return new DeckDTO(deck);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DeckDTO(deckService.createDeck(caller, request)));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public DeckDTO updateDeck(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return new DeckDTO(deckService.updateDeck(id, caller, request));
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
                .body(new DeckDTO(deckService.addElement(id, caller, element)));
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
        return new DeckDTO(deckService.updateElement(id, elementId, caller, element));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/elements/{elementId}")
    public DeckDTO deleteElement(
            @PathVariable String id,
            @PathVariable String elementId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return new DeckDTO(deckService.deleteElement(id, elementId, caller));
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
        return new DeckDTO(deckService.moveElement(id, elementId, to, caller));
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }
}
