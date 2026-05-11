/**
 * One statement (row) in a SCALES question. Stable id keeps responses
 * referenceable when displayed order changes.
 */
package cephadex.brainflex.model.element;

public record ScaleStatement(
        String id,
        String text
) {
}
