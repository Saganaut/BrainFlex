package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.Theme;

public class ThemeDTO {

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
            LocalDateTime createdAt) {

        public ThemeResponse(Theme theme) {
            this(
                    theme.getId(),
                    theme.getName(),
                    theme.getOwnerId(),
                    theme.getOrganizationId(),
                    theme.getHuePrimary(),
                    theme.getHueAccent(),
                    theme.getMode(),
                    theme.getBackgroundImageUrl(),
                    theme.getLogoImageUrl(),
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
}
