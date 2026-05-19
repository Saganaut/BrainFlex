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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.PublishStatus;
import lombok.Data;

@Data
@Document(collection = "decks")
// Explore ordering: live PUBLIC + PUBLISHED decks ranked by rating then plays.
// Combined index supports the `/api/decks/explore` query with `sort=top-rated`
// or `sort=most-played` without a sort stage on top of a filter.
@CompoundIndex(name = "deck_explore_idx", def = "{'publishStatus': 1, 'visibility': 1, 'averageRating': -1, 'playCount': -1}")
public class Deck {
    @Id
    private String id;

    private String name;
    private String description;

    // Tag taxonomy. `tagIds` references documents in the `tags` collection and
    // is the primary tagging field going forward. `tags` is the legacy
    // free-form mirror — kept readable until the frontend migration is done,
    // re-populated from `tagIds` on every save.
    private List<String> tagIds = new ArrayList<>();

    // Primary tag (always a root in the taxonomy). Used by the Explore page
    // to bucket decks under a Subject. Null until the deck is categorized.
    private String subjectTagId;

    private List<String> tags = new ArrayList<>();

    private String creatorUserId; // null for system seeds
    private String organizationId; // optional org scoping
    private boolean system; // seeded by admin, not editable in the UI

    private DeckVisibility visibility = DeckVisibility.PRIVATE;
    // TODO: We will change the name of presets here and default
    private DeckPreset recommendedPreset = DeckPreset.GAME;

    // Presentation chrome
    private Image cover; // thumbnail tile
    private Image background; // applied during play (cascades to elements)
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
    // Original author of the content. Distinct from `parentDeckId`, which is
    // the lineage pointer — this is who first wrote the elements. Copied
    // forward on every fork so credit doesn't get lost down a chain.
    private String originalAuthorUserId;
    private int version = 1; // increment on save

    // Discovery + lifecycle metadata. `publishStatus` decides whether the
    // deck is surfaced in Explore; `visibility` (PUBLIC/UNLISTED/ORG/PRIVATE)
    // still decides who can read it once it is. `publishedAt` is stamped on
    // the DRAFT → PUBLISHED transition.
    private PublishStatus publishStatus = PublishStatus.DRAFT;
    private LocalDateTime publishedAt;

    // BCP-47 language tag (e.g. "en", "en-US", "es"). Default English to
    // match the existing seed content; the editor exposes a picker later.
    private String language = "en";

    private Difficulty difficulty = Difficulty.MEDIUM;

    /** Free-form audience range string, e.g. "6-9", "10-12", "13+", "adult". */
    private String ageRange;

    private License license = License.ALL_RIGHTS_RESERVED;

    // Denormalized counters powering the Explore card without n+1 joins.
    // Updated via Mongo `$inc` / `$set` so they're race-free under concurrent
    // plays/views — never trust a read-modify-write here.
    private int playCount;
    private int viewCount;
    private int favoriteCount;
    private double averageRating;
    private int ratingCount;
    private LocalDateTime lastPlayedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
