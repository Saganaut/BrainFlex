/**
 * Discriminator for the {@link cephadex.brainflex.model.element.SlideBlock}
 * sealed family. The same value is serialized as the Jackson `kind` property
 * so REST callers can read/write blocks without inspecting Java types.
 */
package cephadex.brainflex.model.enums;

public enum SlideBlockKind {
    HEADING,
    BODY,
    BULLET_LIST,
    IMAGE,
    CALLOUT
}
