package cephadex.brainflex.dto.user;

public record UpdateProfileRequest(
        String pictureUrl,
        Boolean newsletter,
        String activeThemeId,
        String timezone) {
}
