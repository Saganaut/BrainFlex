/**
 * REST endpoints for the curated tag taxonomy.
 *
 * Reads ({@code GET /api/tags}, {@code GET /api/tags/{id}}) are open to any
 * registered user — the explore UI needs them on every visit. {@code PUT} /
 * {@code DELETE} are gated by {@code @PreAuthorize("hasRole('ADMIN')")} at the
 * HTTP layer; {@code POST} is open but {@link AdminProperties#isAdmin(User)}
 * strips the curated flag for non-admins (the one inline-branching site that
 * still depends on the helper, since the gate isn't forbid-or-allow).
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
import cephadex.brainflex.dto.deck.CreateTagRequest;
import cephadex.brainflex.dto.deck.TagResponse;
import cephadex.brainflex.dto.deck.UpdateTagRequest;
import cephadex.brainflex.model.deck.Tag;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.service.TagService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

// Any signed-in user can create a tag (so authors can type custom tags /
// subjects into the deck editor), but the `curated` flag is admin-only —
// non-admins always create non-curated tags regardless of what the request
// body says. Curating an existing tag is still done by an admin via PUT.

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
    public List<TagResponse> listTags(
            @RequestParam(name = "curated", required = false) Boolean curated,
            @RequestParam(name = "parentTagId", required = false) String parentTagId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "createdByMe", required = false) Boolean createdByMe,
            Authentication authentication) {
        List<Tag> tags;
        if (search != null && !search.isBlank()) {
            tags = tagService.search(search);
        } else if (parentTagId != null && !parentTagId.isBlank()) {
            tags = tagService.listByParent(parentTagId);
        } else if (Boolean.TRUE.equals(createdByMe)) {
            // Chunk 21 — picker can show "your custom tags" without scanning
            // the full curated table. Forbids anonymous callers since the
            // filter has no useful meaning for them.
            User caller = userService.resolveRegisteredUser(authentication)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Registered account required"));
            tags = tagService.listByCreator(caller.getId());
        } else if (Boolean.TRUE.equals(curated)) {
            tags = tagService.listCurated();
        } else {
            tags = tagService.listAll();
        }
        return tags.stream().map(TagResponse::new).toList();
    }

    /**
     * Returns one tag together with its immediate children (one-level expansion).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public TagResponse getTag(@PathVariable String id) {
        Tag tag = tagService.get(id);
        List<TagResponse> children = tagService.children(id).stream()
                .map(TagResponse::new)
                .toList();
        return new TagResponse(tag, children);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TagResponse> createTag(
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
        CreateTagRequest sanitized = adminProperties.isAdmin(caller)
                ? request
                : new CreateTagRequest(
                        request.id(),
                        request.displayName(),
                        request.parentTagId(),
                        request.description(),
                        request.iconUrl(),
                        false);
        Tag created = tagService.create(sanitized, caller.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TagResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse updateTag(
            @PathVariable String id,
            @Valid @RequestBody UpdateTagRequest request) {
        return new TagResponse(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable String id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
