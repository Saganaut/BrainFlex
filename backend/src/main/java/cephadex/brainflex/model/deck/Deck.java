/**
 * Authored collection of elements (slides + questions) that can be played as
 * an InteractiveSession. The "playable bits" — name, elements, theme, cover,
 * default format/showResponses/settings — live on the embedded
 * {@link PlayableContent} value object, which {@link InteractiveSession}
 * takes a deep copy of at session-create time so mid-session deck edits can't
 * desync clients.
 *
 * <p>Template metadata that has no meaning at play time (visibility,
 * publishStatus, tagIds, ratings, lineage, ownership) stays at the top
 * level on this document.
 *
 * <p>{@code version} is incremented on every save in {@code DeckService}
 * and is the value sessions pin via {@code InteractiveSession.deckVersion}.
 */
package cephadex.brainflex.model.deck;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.PublishStatus;
import lombok.Data;
import cephadex.brainflex.model.shared.Auditable;
import cephadex.brainflex.model.shared.PlayableContent;

@Data
@Document(collection = "decks")
// Explore ordering: live PUBLIC + PUBLISHED decks ranked by rating then plays.
// Combined index supports the `/api/decks/explore` query with `sort=top-rated`
// or `sort=most-played` without a sort stage on top of a filter.
@CompoundIndex(name = "deck_explore_idx", def = "{'publishStatus': 1, 'visibility': 1, 'averageRating': -1, 'playCount': -1}")
public class Deck extends Auditable {
    @Id
    private String id;

    // The playable content — name, elements, theme, cover/background, and
    // the author-suggested defaults (format / showResponses / settings)
    // that a session may override after copying.
    private PlayableContent content = new PlayableContent();

    // Tag taxonomy. `tagIds` references documents in the `tags` collection and
    // is the canonical tagging field. `tags` (below) is a transient,
    // read-only wire carrier rehydrated from `tagIds` by DeckTagHydrationService
    // — never persisted. Set semantics: uniqueness matters for tag membership;
    // ordering does not (LinkedHashSet just preserves insertion order to keep
    // wire output stable across reads).
    private Set<String> tagIds = new LinkedHashSet<>();

    // Primary tag (always a root in the taxonomy). Used by the Explore page
    // to bucket decks under a Subject. Null until the deck is categorized.
    private String subjectTagId;

    // Display-name mirror of `tagIds`. @Transient — never persisted to Mongo.
    // Populated by DeckTagHydrationService on read so DeckResponse can ship the
    // resolved names without a second round-trip on the client.
    @Transient
    private Set<String> tags = new LinkedHashSet<>();

    private String creatorUserId; // null for system seeds
    private String organizationId; // optional org scoping
    private boolean system; // seeded by admin, not editable in the UI

    private DeckVisibility visibility = DeckVisibility.PRIVATE;

    // Lineage
    private String parentDeckId; // populated when this deck was forked
    // Original author of the content. Distinct from `parentDeckId`, which is
    // the lineage pointer — this is who first wrote the elements. Copied
    // forward on every fork so credit doesn't get lost down a chain.
    private String originalAuthorUserId;
    // Bumped on every save by DeckService.updateDeck. Sessions store this
    // value as `deckVersion` to pin which authored revision was played.
    private int version = 1;

    // Hint for the create-interactiveSession UI; not an enforced limit.
    private Integer estimatedDurationMinutes;

    // Discovery + lifecycle metadata. `publishStatus` decides whether the
    // deck is surfaced in Explore; `visibility` (PUBLIC/UNLISTED/ORG/PRIVATE)
    // still decides who can read it once it is. `publishedAt` is stamped on
    // the DRAFT → PUBLISHED transition.
    private PublishStatus publishStatus = PublishStatus.DRAFT;
    private Instant publishedAt;

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
    private Instant lastPlayedAt;
}
