/**
 * Lifecycle states for a GameSession.
 * Drives the server-side state machine in GameService and is broadcast
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
