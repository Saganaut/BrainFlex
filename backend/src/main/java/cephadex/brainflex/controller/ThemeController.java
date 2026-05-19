package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import cephadex.brainflex.dto.ThemeDTO;
import cephadex.brainflex.model.StoredImageVariant;
import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.repository.ThemeRepository;
import cephadex.brainflex.service.AuthorizationService;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.ThemeImageHydrator;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    private final ThemeRepository themeRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;
    private final AuthorizationService authorizationService;
    private final ThemeImageHydrator themeImageHydrator;

    public ThemeController(ThemeRepository themeRepository, UserService userService,
            S3Service s3Service, ImageProcessingService imageProcessingService,
            AuthorizationService authorizationService,
            ThemeImageHydrator themeImageHydrator) {
        this.themeRepository = themeRepository;
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageProcessingService = imageProcessingService;
        this.authorizationService = authorizationService;
        this.themeImageHydrator = themeImageHydrator;
    }

    /** Returns all themes owned by the caller plus any shared with their orgs. */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ThemeDTO.ThemeResponse>> listThemes(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .map(user -> {
                    List<Theme> themes = new ArrayList<>(themeRepository.findByOwnerId(user.getId()));
                    List<String> orgIds = user.getOrganizationIds();
                    if (orgIds != null && !orgIds.isEmpty()) {
                        for (String orgId : orgIds) {
                            if (orgId == null || orgId.isBlank()) continue;
                            themeRepository.findByOrganizationId(orgId).stream()
                                    .filter(t -> !t.getOwnerId().equals(user.getId()))
                                    .forEach(themes::add);
                        }
                    }
                    List<ThemeDTO.ThemeResponse> response = themes.stream()
                            .map(this::buildResponse)
                            .toList();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ThemeDTO.ThemeResponse> createTheme(
            @RequestBody ThemeDTO.CreateThemeRequest request,
            Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .map(user -> {
                    Theme theme = new Theme();
                    theme.setName(request.name());
                    theme.setOwnerId(user.getId());
                    theme.setHuePrimary(clampHue(request.huePrimary()));
                    theme.setHueAccent(clampHue(request.hueAccent()));
                    theme.setMode(validateMode(request.mode()));
                    theme.setOrganizationId(resolveOrgScope(user, request.organizationId()));
                    Theme saved = themeRepository.save(theme);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(buildResponse(saved));
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ThemeDTO.ThemeResponse> updateTheme(
            @PathVariable String id,
            @RequestBody ThemeDTO.UpdateThemeRequest request,
            Authentication authentication) {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        Theme theme = authorizationService.requireThemeEditable(id, user);

        if (request.name() != null && !request.name().isBlank()) {
            theme.setName(request.name());
        }
        if (request.huePrimary() != null) {
            theme.setHuePrimary(clampHue(request.huePrimary()));
        }
        if (request.hueAccent() != null) {
            theme.setHueAccent(clampHue(request.hueAccent()));
        }
        if (request.mode() != null) {
            theme.setMode(validateMode(request.mode()));
        }
        // Passing empty string clears org sharing; null leaves it unchanged.
        // Sharing to an org the caller does not belong to is rejected.
        if (request.organizationId() != null) {
            theme.setOrganizationId(resolveOrgScope(user, request.organizationId()));
        }
        return ResponseEntity.ok(buildResponse(themeRepository.save(theme)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteTheme(
            @PathVariable String id,
            Authentication authentication) {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        Theme theme = authorizationService.requireThemeEditable(id, user);
        if (theme.getLogoVariants() != null && !theme.getLogoVariants().isEmpty()) {
            s3Service.deleteThemeLogo(theme.getId(), theme.getLogoVariants());
        }
        if (theme.getBackgroundVariants() != null && !theme.getBackgroundVariants().isEmpty()) {
            s3Service.deleteThemeBackground(theme.getId(), theme.getBackgroundVariants());
        }
        themeRepository.delete(theme);
        return ResponseEntity.ok().build();
    }

    // @PreAuthorize intentionally omitted on multipart endpoints — see
    // AccountController.uploadProfileImage for the rationale. The inline
    // resolveRegisteredUser + authorizationService.requireThemeEditable pair
    // performs both authn and ownership authorization here.
    @PostMapping(value = "/{id}/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDTO.ThemeResponse> uploadBackground(
            @PathVariable String id,
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        Theme theme = authorizationService.requireThemeEditable(id, user);

        Map<ImageSize, ProcessedVariant> processed = imageProcessingService.processBackground(file);
        List<StoredImageVariant> stored = s3Service.uploadThemeBackground(theme.getId(), processed);
        theme.setBackgroundVariants(stored);
        return ResponseEntity.ok(buildResponse(themeRepository.save(theme)));
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDTO.ThemeResponse> uploadLogo(
            @PathVariable String id,
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {
        User user = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        Theme theme = authorizationService.requireThemeEditable(id, user);

        Map<ImageSize, ProcessedVariant> processed = imageProcessingService.processLogo(file);
        List<StoredImageVariant> stored = s3Service.uploadThemeLogo(theme.getId(), processed);
        theme.setLogoVariants(stored);
        return ResponseEntity.ok(buildResponse(themeRepository.save(theme)));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private ThemeDTO.ThemeResponse buildResponse(Theme theme) {
        return new ThemeDTO.ThemeResponse(
                theme,
                themeImageHydrator.backgroundImageOf(theme),
                themeImageHydrator.logoImageOf(theme));
    }

    private static int clampHue(int hue) {
        return Math.max(0, Math.min(360, hue));
    }

    private static String validateMode(String mode) {
        if (mode == null) return "system";
        return switch (mode) {
            case "light", "dark", "system" -> mode;
            default -> "system";
        };
    }

    /**
     * Normalises a client-supplied org scope: blank/null → personal (null
     * stored); otherwise reject unless the caller is a member of that org.
     */
    private static String resolveOrgScope(User caller, String orgId) {
        if (orgId == null || orgId.isBlank()) return null;
        List<String> memberships = caller.getOrganizationIds();
        if (memberships == null || !memberships.contains(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only share themes with organizations you belong to");
        }
        return orgId;
    }
}
