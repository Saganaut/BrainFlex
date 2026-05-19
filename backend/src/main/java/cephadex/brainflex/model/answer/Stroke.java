/**
 * A single brush stroke inside a {@link DrawingAnswer}.
 *
 * Points are stored flat ({@code [x0, y0, x1, y1, ...]}) rather than as an
 * array of (x, y) objects: the flat form halves the JSON byte count and the
 * client decodes it cheaply at render time. Coordinates are in the same
 * logical-unit space declared by the question's
 * {@code canvasWidth}/{@code canvasHeight}.
 *
 * {@code color} is either a hex string or a design-system token name —
 * the renderer accepts both so authors who hand-curate {@code palette} on
 * the question can mix conventions.
 */
package cephadex.brainflex.model.answer;

import java.util.List;

public record Stroke(
        String color,
        double thickness,
        List<Double> points) {
}
