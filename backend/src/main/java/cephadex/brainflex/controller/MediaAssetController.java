// REST surface for the generalised media library: images, audio, video files,
// and external video embeds. Mirrors GalleryController for ownership rules
// (owner-only edit/delete, organizationId widens visibility only) and the
// multipart auth dance — @PreAuthorize is intentionally omitted on the upload
// endpoint because Spring Session + multipart drops the security context, so
// the inline resolveRegisteredUser + requireMediaAssetEditable pair performs
// both authn and authz on those paths.
package cephadex.brainflex.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.MediaAssetDTO;
import cephadex.brainflex.model.MediaAsset;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.MediaKind;
import cephadex.brainflex.service.AuthorizationService;
import cephadex.brainflex.service.MediaAssetService;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/media")
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    public MediaAssetController(
            MediaAssetService mediaAssetService,
            UserService userService,
            AuthorizationService authorizationService) {
        this.mediaAssetService = mediaAssetService;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<java.util.List<MediaAssetDTO.MediaAssetResponse>> list(
            @RequestParam(value = "kind", required = false) MediaKind kind,
            @RequestParam(value = "tag", required = false) String tag,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        return ResponseEntity.ok(mediaAssetService.list(caller, kind, tag));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MediaAssetDTO.MediaAssetResponse> get(
            @PathVariable String id,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        MediaAsset asset = authorizationService.requireMediaAssetVisible(id, caller);
        return ResponseEntity.ok(mediaAssetService.get(asset));
    }

    // @PreAuthorize omitted: Spring Session + multipart compatibility — see
    // AccountController.uploadProfileImage for the full rationale. The inline
    // resolveRegisteredUser call upgrades anyRequest().authenticated() to a
    // registered-user check.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetDTO.MediaAssetResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("kind") MediaKind kind,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "tags", required = false) String tagsCsv,
            @RequestParam(value = "organizationId", required = false) String organizationId,
            @RequestParam(value = "altText", required = false) String altText,
            Authentication authentication) throws IOException {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        MediaAssetDTO.MediaAssetResponse response = switch (kind) {
            case IMAGE -> mediaAssetService.uploadImage(caller, file, name, tagsCsv, organizationId, altText);
            case AUDIO -> mediaAssetService.uploadAudio(caller, file, name, tagsCsv, organizationId);
            case VIDEO_FILE -> mediaAssetService.uploadVideoFile(caller, file, name, tagsCsv, organizationId);
            case VIDEO_EMBED -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use POST /api/media/embed for video embeds");
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/embed", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MediaAssetDTO.MediaAssetResponse> createEmbed(
            @RequestBody MediaAssetDTO.CreateEmbedRequest request,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaAssetService.createEmbed(caller, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MediaAssetDTO.MediaAssetResponse> update(
            @PathVariable String id,
            @RequestBody MediaAssetDTO.UpdateMediaAssetRequest request,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        MediaAsset asset = authorizationService.requireMediaAssetEditable(id, caller);
        return ResponseEntity.ok(mediaAssetService.update(asset, request, caller));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        MediaAsset asset = authorizationService.requireMediaAssetEditable(id, caller);
        mediaAssetService.delete(asset);
        return ResponseEntity.ok().build();
    }
}
