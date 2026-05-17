/**
 * Non-interactive element used for title screens, section dividers, callouts,
 * and the end card. Carries no answer fields; the round just advances when the
 * display timer expires (or the host clicks Next in TURN_BASED).
 *
 * Inherits `bestAnswerMode` / `bestAnswerTitle` / `bestAnswerBonus` defaults
 * from the interface — slides are never eligible for best-answer voting.
 */
package cephadex.brainflex.model.element;

import java.util.Map;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.SlideKind;

public record Slide(
        String id,
        SlideKind slideKind,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String body,                  // markdown-friendly; rendered as plain text for v1
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
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
