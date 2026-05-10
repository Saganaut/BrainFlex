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

import cephadex.brainflex.dto.ContentPackDTO;
import cephadex.brainflex.dto.CreateContentPackRequest;
import cephadex.brainflex.dto.QuestionEditorDTO;
import cephadex.brainflex.dto.UpdateContentPackRequest;
import cephadex.brainflex.dto.UpsertQuestionRequest;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.ContentPackService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/content-packs")
public class ContentPackController {

    private final ContentPackService contentPackService;
    private final UserService userService;

    public ContentPackController(ContentPackService contentPackService, UserService userService) {
        this.contentPackService = contentPackService;
        this.userService = userService;
    }

    /** All public packs. Used by the game creation picker. */
    @GetMapping
    public List<ContentPackDTO> listPacks() {
        return contentPackService.listPublic().stream()
                .map(ContentPackDTO::new)
                .toList();
    }

    /** Packs owned by the authenticated user. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public List<ContentPackDTO> listMyPacks(Authentication authentication) {
        User caller = resolveUser(authentication);
        return contentPackService.listByOwner(caller.getId()).stream()
                .map(ContentPackDTO::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentPackDTO> getPack(@PathVariable String id) {
        return ResponseEntity.ok(new ContentPackDTO(contentPackService.getById(id)));
    }

    /** Create a new user-owned pack. Registered users only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ContentPackDTO> createPack(
            @Valid @RequestBody CreateContentPackRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ContentPackDTO(contentPackService.createPack(caller, request)));
    }

    /** Update pack metadata. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public ContentPackDTO updatePack(
            @PathVariable String id,
            @Valid @RequestBody UpdateContentPackRequest request,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return new ContentPackDTO(contentPackService.updatePack(id, caller, request));
    }

    /** Delete pack and all its questions. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePack(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        contentPackService.deletePack(id, caller);
        return ResponseEntity.noContent().build();
    }

    /** List all questions in a pack with correct answers visible. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}/questions")
    public List<QuestionEditorDTO> listQuestions(
            @PathVariable String id,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        return contentPackService.listQuestions(id, caller).stream()
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
                .body(new QuestionEditorDTO(contentPackService.addQuestion(id, caller, request)));
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
        return new QuestionEditorDTO(contentPackService.updateQuestion(id, questionId, caller, request));
    }

    /** Remove a question from a pack. Owner only. */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable String id,
            @PathVariable String questionId,
            Authentication authentication) {
        User caller = resolveUser(authentication);
        contentPackService.deleteQuestion(id, questionId, caller);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
    }
}
