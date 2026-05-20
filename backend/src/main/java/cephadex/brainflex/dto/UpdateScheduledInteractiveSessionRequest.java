/**
 * Request body for PUT /api/scheduled-interactive-sessions/{id}. Hosts may edit
 * the start time, end-time estimate, reminder copy, and settings before the
 * row boots. Once status leaves SCHEDULED, updates are rejected.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.InteractiveSessionSettings;
import jakarta.validation.constraints.Future;

public record UpdateScheduledInteractiveSessionRequest(
        @Future LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        InteractiveSessionSettings settings,
        String reminderEmailTemplate) {
}
