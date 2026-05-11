/**
 * REST endpoints for content pack discovery and CRUD.
 * Read endpoints are public. Write endpoints require ROLE_USER and enforce ownership
 * in the service layer — callers that don't own the pack receive 403.
 */
package cephadex.brainflex.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.DeckDTO;
import cephadex.brainflex.dto.CreateDeckRequest;
import cephadex.brainflex.dto.QuestionEditorDTO;
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.dto.UpsertQuestionRequest;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final UserService userService;

    public DeckController(DeckService deckService, UserService userService) {
        this.deckService = deckService;
        this.userService = userService;
    }

    /** All public packs. Used by the game creation picker. */
    @GetMapping
    public List<DeckDTO> listDecks() {
        return deckService.listPublic().stream()
                .map(DeckDTO::new)
                .toList();
    }

    /** Packs owned by the authenticated user. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public List<DeckDTO> listMyDecks(Authentication authentication) {
        User caller = resolveUser(authentication);
        return deckService.listByOwner(caller.getId()).stream()
                .map(DeckDTO::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckDTO> getDeck(@PathVariable String id) {
        return ResponseEntity.ok(new DeckDTO(deckService.getById(id)));
    }

    /** Create a new user-owned pack. Registered users only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DeckDTO(deckService.createDeck(caller, request)));
    }

    /** Update pack metadata. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public DeckDTO updateDeck(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeckRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return new DeckDTO(deckService.updateDeck(id, caller, request));
    }

    /** Delete pack and all its questions. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckService.deleteDeck(id, caller);
        return ResponseEntity.noContent().build();
    }

    /** List all questions in a pack with correct answers visible. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}/questions")
    public List<QuestionEditorDTO> listQuestions(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return deckService.listQuestions(id, caller).stream()
                .map(QuestionEditorDTO::new)
                .toList();
    }

    /** Add a question to a pack. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/questions")
    public ResponseEntity<QuestionEditorDTO> addQuestion(
            @PathVariable String id,
            @Valid @RequestBody UpsertQuestionRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new QuestionEditorDTO(deckService.addQuestion(id, caller, request)));
    }

    /** Replace a question in a pack. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/questions/{questionId}")
    public QuestionEditorDTO updateQuestion(
            @PathVariable String id,
            @PathVariable String questionId,
            @Valid @RequestBody UpsertQuestionRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return new QuestionEditorDTO(deckService.updateQuestion(id, questionId, caller, request));
    }

    /** Remove a question from a pack. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable String id,
            @PathVariable String questionId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        deckService.deleteQuestion(id, questionId, caller);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }
}
