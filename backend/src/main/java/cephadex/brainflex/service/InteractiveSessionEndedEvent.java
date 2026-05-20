/**
 * Fired by InteractiveSessionService when a live session transitions to
 * FINISHED. Used by ScheduledInteractiveSessionService to mark the parent
 * row COMPLETED without a direct service-to-service dependency.
 */
package cephadex.brainflex.service;

public record InteractiveSessionEndedEvent(String interactiveSessionId) {
}
