/**
 * Visual config for a GridQuestion. Use `labels` for a text-only grid
 * (cell N's label is labels[N]) or `backingImage` for an image-grid
 * (the image is sliced into rows × cols cells client-side).
 *
 * One of the two should be set; if both are null the cells render blank.
 */
package cephadex.brainflex.model.element;

import java.util.List;

public record GridCellsConfig(
        List<String> labels,
        Image backingImage
) {

    public GridCellsConfig withBackingImage(Image backingImage) {
        return new GridCellsConfig(labels, backingImage);
    }
}
