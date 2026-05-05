/**
 * Determines how players answer within a game session.
 * SIMULTANEOUS: all players answer at the same time (speed matters).
 * TURN_BASED: players answer in sequence (host controls advancement).
 */
package cephadex.brainflex.model.enums;

public enum GameMode {
    SIMULTANEOUS,
    TURN_BASED
}
