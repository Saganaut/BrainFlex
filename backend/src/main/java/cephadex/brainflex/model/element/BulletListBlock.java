/**
 * Bulleted list block — flat list of strings. Kept as plain strings (rather
 * than nested rich-text items) so the editor stays a single text input per
 * row; promote to rich-text later if formatting per bullet is requested.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.SlideBlockKind;

public record BulletListBlock(
        String id,
        List<String> items
) implements SlideBlock {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.BULLET_LIST;
    }
}
