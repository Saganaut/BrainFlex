/**
 * Heading inside a {@link cephadex.brainflex.model.element.Slide#blocks()}.
 * `level` is 1..3 (h1/h2/h3); the renderer maps each level to a typography
 * scale token.
 */
package cephadex.brainflex.model.element.block;

import cephadex.brainflex.model.enums.SlideBlockKind;

public record HeadingBlock(
        String id,
        String text,
        Integer level
) implements SlideBlock {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.HEADING;
    }
}
