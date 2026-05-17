// REST surface for per-user / per-org image galleries. Mirrors ThemeController
// for ownership rules (owner-only edit/delete, org id widens visibility only)
// and for the multipart auth dance: @PreAuthorize is intentionally omitted on
// the upload endpoint because Spring Session + multipart drops the security
// context — the inline resolveRegisteredUser + AuthorizationService pair
// performs both authn and authz on those paths.
package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import cephadex.brainflex.dto.GalleryImageDTO;
import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.service.AuthorizationService;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_TAGS = 20;
    private static final int MAX_TAG_LENGTH = 40;

    private final GalleryImageRepository galleryImageRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;
    private final AuthorizationService authorizationService;

    public GalleryController(
            GalleryImageRepository galleryImageRepository,
            UserService userService,
            S3Service s3Service,
            ImageProcessingService imageProcessingService,
            AuthorizationService authorizationService) {
        this.galleryImageRepository = galleryImageRepository;
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageProcessingService = imageProcessingService;
        this.authorizationService = authorizationService;
    }

    /** Returns the caller's own gallery images plus every image shared with
     *  any of their organizations. Presigned URLs are refreshed on each read
     *  because the persisted URL has typically expired since upload. */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<GalleryImageDTO.GalleryImageResponse>> listImages(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .map(user -> {
                    List<GalleryImage> images = new ArrayList<>(galleryImageRepository.findByOwnerId(user.getId()));
                    List<String> orgIds = user.getOrganizationIds();
                    if (orgIds != null && !orgIds.isEmpty()) {
                        for (String orgId : orgIds) {
                            if (orgId == null || orgId.isBlank()) continue;
                            galleryImageRepository.findByOrganizationId(orgId).stream()
                                    .filter(i -> !i.getOwnerId().equals(user.getId()))
                                    .forEach(images::add);
                        }
                    }
                    List<GalleryImageDTO.GalleryImageResponse> response = images.stream()
                            .map(image -> {
                                if (image.getS3Key() != null) {
                                    image.setImageUrl(s3Service.refreshPresignedUrl(image.getS3Key()));
                                }
                                return new GalleryImageDTO.GalleryImageResponse(image);
                            })
                            .toList();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // @PreAuthorize omitted: Spring Session + multipart compatibility — see
    // AccountController.uploadProfileImage for the full rationale. The inline
    // resolveRegisteredUser call upgrades anyRequest().authenticated() to a
    // registered-user check.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryImageDTO.GalleryImageResponse> uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "tags", required = false) String tagsCsv,
            @RequestParam(value = "organizationId", required = false) String organizationId,
            Authentication authentication) throws IOException {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        byte[] processed = imageProcessingService.validateAndProcessGalleryImage(file);

        GalleryImage image = new GalleryImage();
        image.setId(UUID.randomUUID().toString());
        image.setOwnerId(user.getId());
        image.setOrganizationId(resolveOrgScope(user, organizationId));
        image.setName(sanitizeName(name, file.getOriginalFilename()));
        image.setTags(sanitizeTags(parseTags(tagsCsv)));

        String key = s3Service.galleryImageKey(image.getId());
        String url = s3Service.uploadGalleryImage(image.getId(), processed);
        image.setS3Key(key);
        image.setImageUrl(url);

        GalleryImage saved = galleryImageRepository.save(image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GalleryImageDTO.GalleryImageResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GalleryImageDTO.GalleryImageResponse> updateImage(
            @PathVariable String id,
            @RequestBody GalleryImageDTO.UpdateGalleryImageRequest request,
            Authentication authentication) {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        GalleryImage image = authorizationService.requireGalleryImageEditable(id, user);

        if (request.name() != null && !request.name().isBlank()) {
            image.setName(sanitizeName(request.name(), image.getName()));
        }
        if (request.tags() != null) {
            image.setTags(sanitizeTags(request.tags()));
        }
        // Empty string clears org sharing; null leaves it unchanged.
        if (request.organizationId() != null) {
            image.setOrganizationId(resolveOrgScope(user, request.organizationId()));
        }
        if (image.getS3Key() != null) {
            image.setImageUrl(s3Service.refreshPresignedUrl(image.getS3Key()));
        }
        return ResponseEntity.ok(new GalleryImageDTO.GalleryImageResponse(galleryImageRepository.save(image)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String id,
            Authentication authentication) {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        GalleryImage image = authorizationService.requireGalleryImageEditable(id, user);
        if (image.getS3Key() != null) {
            s3Service.deleteObject(image.getS3Key());
        }
        galleryImageRepository.delete(image);
        return ResponseEntity.ok().build();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static String sanitizeName(String supplied, String fallback) {
        String candidate = supplied == null || supplied.isBlank() ? fallback : supplied;
        if (candidate == null || candidate.isBlank()) candidate = "Untitled";
        if (candidate.length() > MAX_NAME_LENGTH) {
            candidate = candidate.substring(0, MAX_NAME_LENGTH);
        }
        return candidate.trim();
    }

    private static List<String> parseTags(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static List<String> sanitizeTags(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        return raw.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .map(t -> t.length() > MAX_TAG_LENGTH ? t.substring(0, MAX_TAG_LENGTH) : t)
                .distinct()
                .limit(MAX_TAGS)
                .toList();
    }

    /** Same normalisation rule as ThemeController.resolveOrgScope. */
    private static String resolveOrgScope(User caller, String orgId) {
        if (orgId == null || orgId.isBlank()) return null;
        List<String> memberships = caller.getOrganizationIds();
        if (memberships == null || !memberships.contains(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only share images with organizations you belong to");
        }
        return orgId;
    }
}
