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
        @JsonSubTypes.Type(value = PlaceOnImageQuestion.class)
})
public sealed interface DeckElement
        permits Slide, McqQuestion, TextQuestion, NumberQuestion,
                RankingQuestion, ScalesQuestion, QAndAQuestion, GridQuestion,
                PlaceOnImageQuestion {

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
    String backgroundImageUrl();

    String imageUrl();

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
}
