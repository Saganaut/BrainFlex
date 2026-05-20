/**
 * Builds the wire-ready {@link Image}s for a Theme's logo and background.
 * Mirrors {@link UserImageHydrator} — themes never have an "external" image
 * source, so the result is either an internal Image with refreshed variants
 * or {@link Image#empty()}.
 */
package cephadex.brainflex.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;

@Service
public class ThemeImageHydrator {

    private final S3Service s3Service;

    public ThemeImageHydrator(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public Image logoImageOf(Theme theme) {
        if (theme == null) return Image.empty();
        if (theme.getLogoVariants() == null || theme.getLogoVariants().isEmpty()) return Image.empty();
        Map<ImageSize, ImageVariant> fresh = s3Service.refreshThemeLogo(theme.getId(), theme.getLogoVariants());
        return new Image(false, null, null, fresh);
    }

    public Image backgroundImageOf(Theme theme) {
        if (theme == null) return Image.empty();
        if (theme.getBackgroundVariants() == null || theme.getBackgroundVariants().isEmpty()) return Image.empty();
        Map<ImageSize, ImageVariant> fresh = s3Service.refreshThemeBackground(theme.getId(), theme.getBackgroundVariants());
        return new Image(false, null, null, fresh);
    }
}
