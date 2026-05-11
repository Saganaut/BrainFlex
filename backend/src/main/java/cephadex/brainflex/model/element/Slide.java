/**
 * Non-interactive element used for title screens, section dividers, callouts,
 * and the end card. Carries no answer fields; the round just advances when the
 * display timer expires (or the host clicks Next in TURN_BASED).
 */
package cephadex.brainflex.model.element;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.SlideKind;

public record Slide(
        String id,
        SlideKind slideKind,
        String title,
        String body,                  // markdown-friendly; rendered as plain text for v1
        // shared chrome
        int displaySeconds,
        String hostNotes,
        String backgroundImageUrl,
        String imageUrl,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.SLIDE;
    }
}
