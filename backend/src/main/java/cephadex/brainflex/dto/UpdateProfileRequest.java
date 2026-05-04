package cephadex.brainflex.dto;

public record UpdateProfileRequest(
        String pictureUrl,
        Boolean newsletter) {
}
