// Canonical image-size tiers. Every uploaded image is resized once per tier
// (preserving aspect ratio, capped at the original dimensions) so renderers
// can pick the smallest URL that still satisfies the slot they need to fill.
// Target widths are inclusive upper bounds; smaller sources stay at their
// native size at every tier above their own width.
package cephadex.brainflex.model.element;

public enum ImageSize {
    XS(64),
    SM(200),
    MD(600),
    LG(1200),
    XL(2000);

    private final int targetWidth;

    ImageSize(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public int targetWidth() {
        return targetWidth;
    }
}
