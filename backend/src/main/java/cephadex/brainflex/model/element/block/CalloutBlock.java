/**
 * Tinted callout block — rich-text body inside a styled container. `tone`
 * picks the visual treatment (info / warn / success); each maps to a
 * surface + edge token pair on the frontend.
 */
package cephadex.brainflex.model.element.block;

import cephadex.brainflex.model.enums.CalloutTone;
import cephadex.brainflex.model.enums.SlideBlockKind;

public record CalloutBlock(
        String id,
        String richBody,
        CalloutTone tone
) implements SlideBlock {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.CALLOUT;
    }
}
