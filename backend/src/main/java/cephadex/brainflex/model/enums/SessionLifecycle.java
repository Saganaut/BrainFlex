/**
 * Lifecycle states for an InteractiveSession.
 * Drives the server-side state machine in InteractiveSessionService and is broadcast
 * to clients so the UI can transition between lobby, play, and results screens.
 */
package cephadex.brainflex.model.enums;

public enum SessionLifecycle {
    LOBBY,
    IN_PROGRESS,
    RESULTS,
    FINISHED,
    CANCELLED
}
