/**
 * Open-canvas / "scribble" question. Players draw on a blank or image-backed
 * surface; the answer is a list of strokes. Never scored ({@code scored=false},
 * {@code survey=true} always — both on {@link ElementChrome}) — use Best
 * Answer mode to put a host-curated vote on top of the submissions.
 *
 * Storage strategy is inline: the full stroke list lives on
 * {@link cephadex.brainflex.model.answer.DrawingAnswer} on
 * {@code PlayerAnswer.payload}. {@code maxStrokesPerPlayer} and
 * {@code maxPointsPerStroke} bound a single submission so the document
 * doesn't blow past Mongo's 16 MB limit, and a per-answer byte cap (see
 * InteractiveSessionService) rejects oversize submissions outright.
 *
 * {@code canvasWidth} / {@code canvasHeight} are logical (not pixel) units —
 * the client scales strokes to whatever pixel canvas it renders. {@code
 * backingImage} is an optional underlay (paint over a map, diagram, etc.).
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;

public record DrawingQuestion(
        String id,
        String prompt,
        Image backingImage,            // optional underlay
        int canvasWidth,               // logical units; default 1920
        int canvasHeight,              // default 1080
        int maxStrokesPerPlayer,       // default 200
        int maxPointsPerStroke,        // default 500
        List<String> palette,          // optional swatch palette (oklch tokens or hex)
        // scoring (question-only — not chrome; always 0 / unscored)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.DRAWING;
    }
}
