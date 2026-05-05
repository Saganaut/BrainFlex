/**
 * Public-facing representation of a ContentPack shown in the game creation UI.
 * Omits internal fields like generatorType and creatorUserId that are not
 * relevant to players browsing available packs.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.ContentPack;

public record ContentPackDTO(
        String id,
        String name,
        String description,
        String category,
        int questionCount,
        boolean isSystem,
        LocalDateTime createdAt) {

    public ContentPackDTO(ContentPack pack) {
        this(
                pack.getId(),
                pack.getName(),
                pack.getDescription(),
                pack.getCategory(),
                pack.getQuestionCount(),
                pack.isSystem(),
                pack.getCreatedAt());
    }
}
