/**
 * A named collection of questions that players can choose when creating a game.
 * System packs are seeded by the admin (isSystem=true); the model is designed
 * to support user-created and AI-generated packs in future phases.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "content_packs")
public class ContentPack {
    @Id
    private String id;

    private String name;
    private String description;
    private String category;

    private boolean isSystem; // true = seeded by admin, not editable
    private String creatorUserId; // null for system packs

    private boolean isPublic = true;
    private int questionCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Placeholder fields for future AI generation support
    private String generatorType; // e.g. "AI", "USER" — null for system packs
    private String generationStatus; // PENDING | GENERATING | READY | FAILED — null for system packs
}
