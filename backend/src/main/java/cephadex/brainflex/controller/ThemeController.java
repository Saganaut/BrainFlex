package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import cephadex.brainflex.dto.ThemeDTO;
import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.ThemeRepository;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    private final ThemeRepository themeRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;

    public ThemeController(ThemeRepository themeRepository, UserService userService,
            S3Service s3Service, ImageProcessingService imageProcessingService) {
        this.themeRepository = themeRepository;
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageProcessingService = imageProcessingService;
    }

    /** Returns all themes owned by the caller plus any shared with their org. */
    @GetMapping
    public ResponseEntity<List<ThemeDTO.ThemeResponse>> listThemes(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .map(user -> {
                    List<Theme> themes = new ArrayList<>(themeRepository.findByOwnerId(user.getId()));
                    if (user.getOrganizationId() != null && !user.getOrganizationId().isBlank()) {
                        List<Theme> orgThemes = themeRepository.findByOrganizationId(user.getOrganizationId());
                        orgThemes.stream()
                                .filter(t -> !t.getOwnerId().equals(user.getId()))
                                .forEach(themes::add);
                    }
                    List<ThemeDTO.ThemeResponse> response = themes.stream()
                            .map(ThemeDTO.ThemeResponse::new)
                            .toList();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping
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
                    theme.setOrganizationId(request.organizationId());
                    Theme saved = themeRepository.save(theme);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(new ThemeDTO.ThemeResponse(saved));
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThemeDTO.ThemeResponse> updateTheme(
            @PathVariable String id,
            @RequestBody ThemeDTO.UpdateThemeRequest request,
            Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .flatMap(user -> themeRepository.findById(id)
                        .filter(t -> t.getOwnerId().equals(user.getId())))
                .map(theme -> {
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
                    // Passing empty string clears org sharing; null leaves it unchanged
                    if (request.organizationId() != null) {
                        theme.setOrganizationId(
                                request.organizationId().isBlank() ? null : request.organizationId());
                    }
                    return ResponseEntity.ok(new ThemeDTO.ThemeResponse(themeRepository.save(theme)));
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheme(
            @PathVariable String id,
            Authentication authentication) {
        java.util.Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        java.util.Optional<Theme> themeOpt = themeRepository.findById(id)
                .filter(t -> t.getOwnerId().equals(userOpt.get().getId()));
        if (themeOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        themeRepository.delete(themeOpt.get());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDTO.ThemeResponse> uploadBackground(
            @PathVariable String id,
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {
        return resolveOwnedTheme(id, authentication)
                .map(pair -> {
                    try {
                        byte[] processed = imageProcessingService.validateAndProcessBackground(file);
                        String url = s3Service.uploadThemeBackground(pair.theme().getId(), processed);
                        pair.theme().setBackgroundImageUrl(url);
                        return ResponseEntity.ok(
                                new ThemeDTO.ThemeResponse(themeRepository.save(pair.theme())));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDTO.ThemeResponse> uploadLogo(
            @PathVariable String id,
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {
        return resolveOwnedTheme(id, authentication)
                .map(pair -> {
                    try {
                        byte[] processed = imageProcessingService.validateAndProcessLogo(file);
                        String url = s3Service.uploadThemeLogo(pair.theme().getId(), processed);
                        pair.theme().setLogoImageUrl(url);
                        return ResponseEntity.ok(
                                new ThemeDTO.ThemeResponse(themeRepository.save(pair.theme())));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private record UserThemePair(User user, Theme theme) {
    }

    private java.util.Optional<UserThemePair> resolveOwnedTheme(String themeId, Authentication auth) {
        return userService.resolveRegisteredUser(auth)
                .flatMap(user -> themeRepository.findById(themeId)
                        .filter(t -> t.getOwnerId().equals(user.getId()))
                        .map(t -> new UserThemePair(user, t)));
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
}
