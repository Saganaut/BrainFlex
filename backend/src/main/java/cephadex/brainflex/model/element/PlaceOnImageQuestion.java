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

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.PlaceScoring;
import cephadex.brainflex.model.image.Image;

public record PlaceOnImageQuestion(
        String id,
        String prompt,
        Image targetImage,
        double correctX,               // 0..1
        double correctY,               // 0..1
        double tolerance,              // 0..1
        PlaceScoring scoring,
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.PLACE_ON_IMAGE;
    }
}
