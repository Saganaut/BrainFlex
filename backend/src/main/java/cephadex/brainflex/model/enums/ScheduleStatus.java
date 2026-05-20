/**
 * Lifecycle of a ScheduledInteractiveSession. The cron sweep transitions
 * SCHEDULED → LIVE when scheduledStartAt passes; LIVE → COMPLETED happens
 * automatically when the booted InteractiveSession reaches FINISHED.
 * CANCELLED is a terminal state set by the host before boot time.
 */
package cephadex.brainflex.model.enums;

public enum ScheduleStatus {
    SCHEDULED,
    LIVE,
    COMPLETED,
    CANCELLED
}
