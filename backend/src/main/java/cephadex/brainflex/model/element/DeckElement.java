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
 */
package cephadex.brainflex.model.element;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Slide.class),
        @JsonSubTypes.Type(value = McqQuestion.class),
        @JsonSubTypes.Type(value = TextQuestion.class),
        @JsonSubTypes.Type(value = NumberQuestion.class),
        @JsonSubTypes.Type(value = ImageChoiceQuestion.class),
        @JsonSubTypes.Type(value = RankingQuestion.class),
        @JsonSubTypes.Type(value = ScalesQuestion.class),
        @JsonSubTypes.Type(value = QAndAQuestion.class),
        @JsonSubTypes.Type(value = GridQuestion.class),
        @JsonSubTypes.Type(value = PlaceOnImageQuestion.class)
})
public sealed interface DeckElement
        permits Slide, McqQuestion, TextQuestion, NumberQuestion, ImageChoiceQuestion,
                RankingQuestion, ScalesQuestion, QAndAQuestion, GridQuestion,
                PlaceOnImageQuestion {

    String id();
    ElementKind kind();

    /** Auto-advance after this many seconds. 0 = host advances manually (slides) or no timer (questions). */
    int displaySeconds();

    /** Private notes shown only to the host during play. Never broadcast to participants. */
    String hostNotes();

    /** Element-level background image override; falls back to deck-level. */
    String backgroundImageUrl();

    String imageUrl();
    String videoUrl();        // YouTube link (v1)
    String audioUrl();
    MediaPosition mediaPosition();

    /**
     * Best Answer mode is a two-phase round modifier (SUBMIT → VOTE → REVEAL).
     * Element kinds that declare these as record components automatically
     * override these defaults via their generated accessors; Slide (and any
     * future non-scored kind) inherits the default `false / 0` here.
     */
    default boolean bestAnswerMode() { return false; }
    default int bestAnswerBonus() { return 0; }
}
