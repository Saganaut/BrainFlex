/**
 * Sealed polymorphic content fragment inside a {@link Slide#blocks()}. Replaces
 * the legacy single `body: String` field so a Content slide can be a stack of
 * headings + rich-text + bullet lists + images + callouts (Mentimeter Content
 * slides build this way).
 *
 * Each permits record carries its own fields; the shared {@link #id()} is a
 * stable per-block UUID so the editor can reorder freely. Jackson routes via
 * the `kind` discriminator — same pattern as {@link DeckElement} and the
 * AnswerPayload family.
 */
package cephadex.brainflex.model.element;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cephadex.brainflex.model.enums.SlideBlockKind;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HeadingBlock.class),
        @JsonSubTypes.Type(value = BodyBlock.class),
        @JsonSubTypes.Type(value = BulletListBlock.class),
        @JsonSubTypes.Type(value = ImageBlock.class),
        @JsonSubTypes.Type(value = CalloutBlock.class)
})
public sealed interface SlideBlock
        permits HeadingBlock, BodyBlock, BulletListBlock, ImageBlock, CalloutBlock {

    /** Stable per-block UUID; survives reorder so renderer / editor refs stay valid. */
    String id();

    SlideBlockKind kind();
}
