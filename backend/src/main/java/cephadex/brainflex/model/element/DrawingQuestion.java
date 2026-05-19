/**
 * Open-canvas / "scribble" question. Players draw on a blank or image-backed
 * surface; the answer is a list of strokes. Never scored ({@code scored=false},
 * {@code survey=true} always) — use Best Answer mode to put a host-curated
 * vote on top of the submissions.
 *
 * Storage strategy is inline: the full stroke list lives on
 * {@link cephadex.brainflex.model.answer.DrawingAnswer} on
 * {@code PlayerAnswer.payload}. {@code maxStrokesPerPlayer} and
 * {@code maxPointsPerStroke} bound a single submission so the document
 * doesn't blow past Mongo's 16 MB limit, and a per-answer byte cap (see
 * ShowcaseService) rejects oversize submissions outright.
 *
 * {@code canvasWidth} / {@code canvasHeight} are logical (not pixel) units —
 * the client scales strokes to whatever pixel canvas it renders. {@code
 * backingImage} is an optional underlay (paint over a map, diagram, etc.).
 */
package cephadex.brainflex.model.element;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record DrawingQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        Image backingImage,            // optional underlay
        int canvasWidth,               // logical units; default 1920
        int canvasHeight,              // default 1080
        int maxStrokesPerPlayer,       // default 200
        int maxPointsPerStroke,        // default 500
        List<String> palette,          // optional swatch palette (oklch tokens or hex)
        // scoring (forced to non-scored / survey)
        int pointValue,
        Difficulty difficulty,
        boolean scored,                // always false for drawing
        boolean survey,                // always true
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier (drawings are a natural fit for vote-the-best)
        boolean bestAnswerMode,
        String bestAnswerTitle,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.DRAWING;
    }
}
