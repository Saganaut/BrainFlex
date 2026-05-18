/**
 * Drop a point on an image. Coordinates are normalized to 0–1 so the question
 * scores correctly regardless of the rendered image size on the client.
 *
 * Scoring:
 *   BINARY — full points iff the submitted point is within `tolerance`
 *            (Euclidean distance in normalized space) of the target
 *   LINEAR — points scaled linearly from full (at zero distance) to zero
 *            (at distance == tolerance); zero outside tolerance
 */
package cephadex.brainflex.model.element;

import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.PlaceScoring;
import cephadex.brainflex.model.enums.ResponseMode;

public record PlaceOnImageQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        Image targetImage,
        double correctX,               // 0..1
        double correctY,               // 0..1
        double tolerance,              // 0..1
        PlaceScoring scoring,
        // scoring
        int pointValue,
        Difficulty difficulty,
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier
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
        return ElementKind.PLACE_ON_IMAGE;
    }
}
