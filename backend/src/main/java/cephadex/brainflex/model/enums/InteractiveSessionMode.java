/**
 * Determines how players answer within an interactive session.
 * SIMULTANEOUS: all players answer at the same time (speed matters).
 * TURN_BASED: players answer in sequence (host controls advancement).
 */
package cephadex.brainflex.model.enums;

public enum InteractiveSessionMode {
    SIMULTANEOUS,
    TURN_BASED
}
