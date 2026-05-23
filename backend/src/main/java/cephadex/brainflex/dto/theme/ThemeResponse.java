// Wire shape for a Theme. `logo` and `background` carry one URL per ImageSize
// tier; `logoImageUrl` / `backgroundImageUrl` are derived back-compat fields
// (largest variant URL, or null when no image is set) so existing callers that
// only need a single URL keep working until the frontend migrates.
package cephadex.brainflex.dto.theme;

import java.time.Instant;

import cephadex.brainflex.model.theme.Theme;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.ThemeMode;

public record ThemeResponse(
        String id,
        String name,
        String ownerId,
        String organizationId,
        int huePrimary,
        int hueAccent,
        ThemeMode mode,
        String backgroundImageUrl,
        String logoImageUrl,
        Image background,
        Image logo,
        Instant createdAt) {

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

    private static String largestUrl(Image image) {
        return image == null ? null : image.largestUrl();
    }
}
