/**
 * Lifecycle states for a Showcase.
 * Drives the server-side state machine in ShowcaseService and is broadcast
 * to clients so the UI can transition between lobby, play, and results screens.
 */
package cephadex.brainflex.model.enums;

public enum GameStatus {
    LOBBY,
    IN_PROGRESS,
    RESULTS,
    FINISHED,
    CANCELLED
}
