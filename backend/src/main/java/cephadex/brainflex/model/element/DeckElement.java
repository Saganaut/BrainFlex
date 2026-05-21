/**
 * Sealed polymorphic element type embedded in a Deck.
 *
 * Two top-level kinds: SLIDE (non-interactive content) and Question (sub-sealed,
 * one variant per question type). Jackson serializes the discriminator as a
 * "kind" property; Spring Data MongoDB stores its usual `_class` discriminator
 * inside the deck document so reads round-trip without any extra config.
 *
 * Every element carries an {@link ElementChrome chrome} component holding the
 * universally-shared display / response / media / best-answer / audit fields.
 * The accessors on this interface delegate to that chrome record so callers
 * can keep reading {@code element.title()}, {@code element.bestAnswerPoints()},
 * etc. without knowing the composition.
 *
 * Kind-specific fields stay as record components on each concrete record:
 *   - {@code prompt}, {@code pointValue}, {@code difficulty}, {@code explanation}
 *     on questions
 *   - {@code slideKind}, {@code body}, {@code blocks}, presentation toggles
 *     on Slide
 */
package cephadex.brainflex.model.element;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cephadex.brainflex.model.enums.BestAnswerScoring;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.ShowResponsesMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /** The shared chrome (display / response / media / audit fields). Never null in practice. */
    ElementChrome chrome();

    // ── Chrome delegate accessors ────────────────────────────────────────────
    //
    // Each accessor pulls from {@link #chrome()} so all callers can keep their
    // existing flat reads (e.g. {@code element.title()}) — the composition is
    // invisible to consumers. {@code chrome} is expected to be non-null; if a
    // record ever needs an empty default it should pass a fully-populated
    // ElementChrome rather than null.

    /** Player-facing short opaque token (Mentimeter's `slide_public_key`). */
    default String publicKey() { return chrome().publicKey(); }

    /** Host/admin-facing short opaque token (Mentimeter's `slide_admin_key`). */
    default String privateKey() { return chrome().privateKey(); }

    /** Plain-text heading shown in lists, thumbnails, exports. */
    default String title() { return chrome().title(); }

    /** Rich-text version of `title` — TipTap/ProseMirror JSON doc. Nullable. */
    default Map<String, Object> styledTitle() { return chrome().styledTitle(); }

    /** Auto-advance after this many seconds. 0 = host advances manually. */
    default int displaySeconds() { return chrome().displaySeconds(); }

    /** Private notes shown only to the host during play. Never broadcast to participants. */
    default String speakerNotes() { return chrome().speakerNotes(); }

    /** Element-level background image override; falls back to deck-level. */
    default Image background() { return chrome().background(); }

    default Image image() { return chrome().image(); }

    /** YouTube link (v1). Superseded by {@link #videoAssetId()} when set. */
    default String videoUrl() { return chrome().videoUrl(); }

    default String audioUrl() { return chrome().audioUrl(); }

    /** Optional MediaAsset id (kind=VIDEO_FILE or VIDEO_EMBED). Wins over {@link #videoUrl()} when set. */
    default String videoAssetId() { return chrome().videoAssetId(); }

    /** Optional MediaAsset id (kind=AUDIO). Wins over {@link #audioUrl()} when set. */
    default String audioAssetId() { return chrome().audioAssetId(); }

    default MediaPosition mediaPosition() { return chrome().mediaPosition(); }

    /**
     * Whether this element contributes to the leaderboard. Slide and Q&A are
     * inherently unscored; Scales is unscored when used as a pulse poll.
     */
    default boolean scored() { return chrome().scored(); }

    /**
     * Opinion-style question with no correct answer (results = distribution,
     * not points). Independent of `scored` — a survey is unscored by definition
     * but a non-survey can also be unscored (e.g. a recap slide).
     */
    default boolean survey() { return chrome().survey(); }

    /**
     * null = single-select (default); n = player may submit up to n picks.
     * Currently honored by MCQ — other kinds ignore it.
     */
    default Integer multipleSelections() { return chrome().multipleSelections(); }

    /** Host-controlled freeze toggle. NOT_ACCEPTING_RESPONSES rejects submissions. */
    default ResponseMode responseMode() { return chrome().responseMode(); }

    /**
     * Bottom of the runtime show-responses cascade (element → deck → session
     * → format default). Default is {@link ShowResponsesMode#INHERIT}, which
     * defers to the next level up. Element kinds that need a per-question
     * override (currently {@link Slide}) declare it as a record component so
     * the generated accessor wins over this default.
     */
    default ShowResponsesMode showResponses() {
        return ShowResponsesMode.INHERIT;
    }

    /** Two-phase SUBMIT to VOTE round modifier. */
    default boolean bestAnswerMode() { return chrome().bestAnswerMode(); }

    /** Prompt shown during the VOTE phase, e.g. "Which answer is the funniest?". */
    default String bestAnswerTitle() { return chrome().bestAnswerTitle(); }

    /**
     * Tunable the resolved {@link cephadex.brainflex.service.bestanswer.BestAnswerScoringStrategy}
     * consumes — per-vote multiplier under POINTS_PER_VOTE, flat amount under
     * FLAT_WINNER. Renamed from {@code bestAnswerBonus} in chunk 24.
     */
    default int bestAnswerPoints() { return chrome().bestAnswerPoints(); }

    /** Strategy used to award points during the VOTE phase. */
    default BestAnswerScoring bestAnswerScoring() { return chrome().bestAnswerScoring(); }

    /** User who first authored this element. Set by the backend on add; null for system seeds. */
    default String createdByUserId() { return chrome().createdByUserId(); }

    /** User who most recently edited this element. Stamped server-side on every update. */
    default String lastEditedByUserId() { return chrome().lastEditedByUserId(); }

    /** Timestamp the element was first added to the deck. Stamped server-side. */
    default LocalDateTime createdAt() { return chrome().createdAt(); }

    /** Timestamp of the most recent edit. Stamped server-side on every update. */
    default LocalDateTime updatedAt() { return chrome().updatedAt(); }

    /** Per-element tag references (independent of the deck's tagIds). Empty by default. */
    default List<String> tagIds() { return chrome().tagIds(); }

    /** Caption shown beneath the media slot — author-controlled. */
    default String mediaCaption() { return chrome().mediaCaption(); }

    /** Accessibility text for the media slot. */
    default String altText() { return chrome().altText(); }

    /** When false, audience emoji reactions are suppressed for this element. */
    default boolean reactionsEnabled() { return chrome().reactionsEnabled(); }

    /** Bumped on every server-side save. Clients use it to detect stale edits. */
    default Integer version() { return chrome().version(); }
}
