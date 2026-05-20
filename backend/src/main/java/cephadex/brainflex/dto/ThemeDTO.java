package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;

public class ThemeDTO {

    /**
     * Wire shape for a Theme. `logo` and `background` carry one URL per
     * {@link cephadex.brainflex.model.element.ImageSize} tier; {@code logoImageUrl} /
     * {@code backgroundImageUrl} are derived back-compat fields (largest
     * variant URL, or null when no image is set) so existing callers that
     * only need a single URL keep working until the frontend migrates.
     */
    public record ThemeResponse(
            String id,
            String name,
            String ownerId,
            String organizationId,
            int huePrimary,
            int hueAccent,
            String mode,
            String backgroundImageUrl,
            String logoImageUrl,
            Image background,
            Image logo,
            LocalDateTime createdAt) {

        public ThemeResponse(Theme theme, Image background, Image logo) {
            this(
                    theme.getId(),
                    theme.getName(),
                    theme.getOwnerId(),
                    theme.getOrganizationId(),
                    theme.getHuePrimary(),
                    theme.getHueAccent(),
                    theme.getMode(),
                    largestUrl(background),
                    largestUrl(logo),
                    background,
                    logo,
                    theme.getCreatedAt());
        }
    }

    public record CreateThemeRequest(
            String name,
            int huePrimary,
            int hueAccent,
            String mode,
            String organizationId) {
    }

    public record UpdateThemeRequest(
            String name,
            Integer huePrimary,
            Integer hueAccent,
            String mode,
            String organizationId) {
    }

    private static String largestUrl(Image image) {
        return image == null ? null : image.largestUrl();
    }

    /** Helper for callers that don't have variants to hydrate (e.g. unit
     *  tests, light-weight list-page responses). */
    public static Map<ImageSize, ImageVariant> noVariants() {
        return Collections.emptyMap();
    }
}
