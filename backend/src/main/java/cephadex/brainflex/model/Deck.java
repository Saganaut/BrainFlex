/**
 * Authored collection of elements (slides + questions) that can be played as a
 * Showcase. Elements are embedded directly in the document for atomic reads /
 * writes; ordering is the natural list order.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import lombok.Data;

@Data
@Document(collection = "decks")
public class Deck {
    @Id
    private String id;

    private String name;
    private String description;

    // Multi-tag categorization (replaces the legacy `category` string).
    private List<String> tags = new ArrayList<>();

    private String creatorUserId; // null for system seeds
    private String organizationId; // optional org scoping
    private boolean system; // seeded by admin, not editable in the UI

    private DeckVisibility visibility = DeckVisibility.PRIVATE;
    private DeckPreset recommendedPreset = DeckPreset.GAME;

    // Presentation chrome
    private String coverImageUrl; // thumbnail tile
    private String backgroundImageUrl; // applied during play (cascades to elements)
    private String themeId; // optional link to a saved Theme

    // Content — order matters; the runtime walks elements in this order.
    private List<DeckElement> elements = new ArrayList<>();

    // Author-suggested showcase defaults — copied into Showcase.settings at create
    // time.
    private ShowcaseSettings defaultSettings = new ShowcaseSettings();

    // Hint for the create-showcase UI; not an enforced limit.
    private Integer estimatedDurationMinutes;

    // Lineage
    private String parentDeckId; // populated when this deck was forked
    private int version = 1; // increment on save

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
