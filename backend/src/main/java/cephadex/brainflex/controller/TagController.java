/**
 * REST endpoints for the curated tag taxonomy.
 *
 * Reads ({@code GET /api/tags}, {@code GET /api/tags/{id}}) are open to any
 * registered user — the explore UI needs them on every visit. Writes
 * ({@code POST}, {@code PUT}, {@code DELETE}) require the caller to be on
 * the admin allowlist ({@link AdminProperties}). A user-facing
 * {@code ROLE_ADMIN} replaces the allowlist later (chunk 20).
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.config.AdminProperties;
import cephadex.brainflex.dto.TagDTO;
import cephadex.brainflex.model.Tag;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.TagService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final UserService userService;
    private final AdminProperties adminProperties;

    public TagController(TagService tagService, UserService userService, AdminProperties adminProperties) {
        this.tagService = tagService;
        this.userService = userService;
        this.adminProperties = adminProperties;
    }

    /**
     * Optional filters compose: {@code ?curated=true} restricts to curated
     * roots; {@code ?parentTagId=...} restricts to children of one root;
     * {@code ?search=...} runs the typeahead. Pass none to list everything
     * (used by the picker when the user starts blank).
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<TagDTO.TagResponse> listTags(
            @RequestParam(name = "curated", required = false) Boolean curated,
            @RequestParam(name = "parentTagId", required = false) String parentTagId,
            @RequestParam(name = "search", required = false) String search) {
        List<Tag> tags;
        if (search != null && !search.isBlank()) {
            tags = tagService.search(search);
        } else if (parentTagId != null && !parentTagId.isBlank()) {
            tags = tagService.listByParent(parentTagId);
        } else if (Boolean.TRUE.equals(curated)) {
            tags = tagService.listCurated();
        } else {
            tags = tagService.listAll();
        }
        return tags.stream().map(TagDTO.TagResponse::new).toList();
    }

    /** Returns one tag together with its immediate children (one-level expansion). */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public TagDTO.TagResponse getTag(@PathVariable String id) {
        Tag tag = tagService.get(id);
        List<TagDTO.TagResponse> children = tagService.children(id).stream()
                .map(TagDTO.TagResponse::new)
                .toList();
        return new TagDTO.TagResponse(tag, children);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TagDTO.TagResponse> createTag(
            @Valid @RequestBody TagDTO.CreateTagRequest request,
            Authentication authentication) {
        requireAdmin(authentication);
        Tag created = tagService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TagDTO.TagResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public TagDTO.TagResponse updateTag(
            @PathVariable String id,
            @Valid @RequestBody TagDTO.UpdateTagRequest request,
            Authentication authentication) {
        requireAdmin(authentication);
        return new TagDTO.TagResponse(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteTag(
            @PathVariable String id,
            Authentication authentication) {
        requireAdmin(authentication);
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
        if (!adminProperties.isAdmin(caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
