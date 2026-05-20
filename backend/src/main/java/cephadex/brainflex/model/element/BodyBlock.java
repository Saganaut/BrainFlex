/**
 * Rich-text paragraph block — TipTap/ProseMirror HTML, same contract as the
 * `RichTextInput` component on the frontend. Migration from the legacy
 * `Slide.body` field wraps the old string into a single BodyBlock.
 */
package cephadex.brainflex.model.element;

import cephadex.brainflex.model.enums.SlideBlockKind;

public record BodyBlock(
        String id,
        String richBody
) implements SlideBlock {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.BODY;
    }
}
