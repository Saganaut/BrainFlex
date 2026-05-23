/**
 * Visual config for a GridQuestion. Use `labels` for a text-only grid
 * (cell N's label is labels[N]) or `backingImage` for an image-grid
 * (the image is sliced into rows × cols cells client-side).
 *
 * One of the two should be set; if both are null the cells render blank.
 */
package cephadex.brainflex.model.element.parts;

import java.util.List;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageContainer;

public record GridCellsConfig(
        List<String> labels,
        Image backingImage
) implements ImageContainer {

    @Override
    public GridCellsConfig withImage(String fieldName, Image value) {
        return switch (fieldName) {
            case "backingImage" -> new GridCellsConfig(labels, value);
            default -> throw new IllegalArgumentException(
                    "GridCellsConfig has no image field named '" + fieldName + "'");
        };
    }
}
