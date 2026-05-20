/**
 * Sealed polymorphic element type embedded in a Deck.
 *
 * Two top-level kinds: SLIDE (non-interactive content) and Question (sub-sealed,
 * one variant per question type). Jackson serializes the discriminator as a
 * "kind" property; Spring Data MongoDB stores its usual `_class` discriminator
 * inside the deck document so reads round-trip without any extra config.
 *
 * Every element carries display-time + media + host-notes fields directly so
 * the runtime can render any kind uniformly without a kind switch for chrome.
 *
 * Shared chrome conventions (declared as record components on every kind):
 *   - publicKey / privateKey  short opaque tokens for player-facing and
 *                             host-facing identity (Mentimeter-style)
 *   - title / styledTitle     plain heading + rich-text TipTap/ProseMirror doc
 *   - scored / survey         whether this element awards points and whether
 *                             it's an opinion-style question (no correct answer)
 *   - multipleSelections      null = single-select; n = allow up to n picks
 *   - responseMode            host-controlled accepting/not-accepting toggle
 *   - bestAnswerMode / bestAnswerTitle / bestAnswerBonus
 *                             two-phase SUBMIT to VOTE round modifier
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Slide.class),
        @JsonSubTypes.Type(value = McqQuestion.class),
        @JsonSubTypes.Type(value = TextQuestion.class),
        @JsonSubTypes.Type(value = NumberQuestion.class),
        @JsonSubTypes.Type(value = RankingQuestion.class),
        @JsonSubTypes.Type(value = ScalesQuestion.class),
        @JsonSubTypes.Type(value = QAndAQuestion.class),
        @JsonSubTypes.Type(value = GridQuestion.class),
        @JsonSubTypes.Type(value = PlaceOnImageQuestion.class),
        @JsonSubTypes.Type(value = WordCloudQuestion.class),
        @JsonSubTypes.Type(value = AllocationQuestion.class),
        @JsonSubTypes.Type(value = MatchingQuestion.class),
        @JsonSubTypes.Type(value = DrawingQuestion.class)
})
public sealed interface DeckElement
        permits Slide, McqQuestion, TextQuestion, NumberQuestion,
                RankingQuestion, ScalesQuestion, QAndAQuestion, GridQuestion,
                PlaceOnImageQuestion, WordCloudQuestion,
                AllocationQuestion, MatchingQuestion, DrawingQuestion {

    String id();

    ElementKind kind();

    /** Player-facing short opaque token (Mentimeter's `slide_public_key`). */
    String publicKey();

    /** Host/admin-facing short opaque token (Mentimeter's `slide_admin_key`). */
    String privateKey();

    /** Plain-text heading shown in lists, thumbnails, exports. */
    String title();

    /** Rich-text version of `title` — TipTap/ProseMirror JSON doc. Nullable. */
    Map<String, Object> styledTitle();

    /** Auto-advance after this many seconds. 0 = host advances manually. */
    int displaySeconds();

    /** Private notes shown only to the host during play. Never broadcast to participants. */
    String speakerNotes();

    /** Element-level background image override; falls back to deck-level. */
    Image background();

    Image image();

    String videoUrl(); // YouTube link (v1)

    String audioUrl();

    MediaPosition mediaPosition();

    /**
     * Whether this element contributes to the leaderboard. Slide and Q&A are
     * inherently unscored; Scales is unscored when used as a pulse poll.
     */
    boolean scored();

    /**
     * Opinion-style question with no correct answer (results = distribution,
     * not points). Independent of `scored` — a survey is unscored by definition
     * but a non-survey can also be unscored (e.g. a recap slide).
     */
    boolean survey();

    /**
     * null = single-select (default); n = player may submit up to n picks.
     * Currently honored by MCQ — other kinds ignore it.
     */
    Integer multipleSelections();

    /** Host-controlled freeze toggle. NOT_ACCEPTING_RESPONSES rejects submissions. */
    ResponseMode responseMode();

    /**
     * Best Answer mode is a two-phase round modifier (SUBMIT to VOTE to REVEAL).
     * Element kinds that declare these as record components automatically
     * override these defaults via their generated accessors; Slide (and any
     * future non-scored kind) inherits the default `false / 0` here.
     */
    default boolean bestAnswerMode() {
        return false;
    }

    /** Prompt shown during the VOTE phase, e.g. "Which answer is the funniest?". */
    default String bestAnswerTitle() {
        return null;
    }

    default int bestAnswerBonus() {
        return 0;
    }

    // ---- Provenance + shared metadata (chunk 10b) ----
    //
    // Declared as default methods returning safe values; every record declares
    // these as record components, so the generated accessor overrides the
    // default. Kept on the interface so future kinds get a working baseline
    // and callers can read them off any DeckElement without a kind switch.

    /** User who first authored this element. Set by the backend on add; null for system seeds. */
    default String createdByUserId() {
        return null;
    }

    /** User who most recently edited this element. Stamped server-side on every update. */
    default String lastEditedByUserId() {
        return null;
    }

    /** Timestamp the element was first added to the deck. Stamped server-side. */
    default LocalDateTime createdAt() {
        return null;
    }

    /** Timestamp of the most recent edit. Stamped server-side on every update. */
    default LocalDateTime updatedAt() {
        return null;
    }

    /** Per-element tag references (independent of the deck's tagIds). Empty by default. */
    default List<String> tagIds() {
        return List.of();
    }

    /** Caption shown beneath the media slot — author-controlled. */
    default String mediaCaption() {
        return null;
    }

    /** Accessibility text for the media slot. */
    default String altText() {
        return null;
    }

    /** When false, audience emoji reactions are suppressed for this element. */
    default boolean reactionsEnabled() {
        return true;
    }

    /** Bumped on every server-side save. Clients use it to detect stale edits. */
    default Integer version() {
        return 1;
    }

    // ── MediaAsset references (chunk 19) ──────────────────────────────────────
    //
    // Optional pointers at a MediaAsset.id. When set, take precedence over the
    // legacy {@code videoUrl} / {@code audioUrl} string fields at render time
    // (renderer should prefer asset-id, fall back to url string). Declared as
    // default methods returning null so existing records don't need to be
    // rewritten; kinds that introduce real value for these fields can override
    // by declaring them as record components.

    /** Optional MediaAsset id (kind=VIDEO_FILE or VIDEO_EMBED). Wins over {@code videoUrl} when set. */
    default String videoAssetId() {
        return null;
    }

    /** Optional MediaAsset id (kind=AUDIO). Wins over {@code audioUrl} when set. */
    default String audioAssetId() {
        return null;
    }
}
