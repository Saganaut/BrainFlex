/**
 * Request body for PUT /api/scheduled-interactive-sessions/{id}. Hosts may edit
 * the start time, end-time estimate, reminder copy, and settings before the
 * row boots. Once status leaves SCHEDULED, updates are rejected.
 */
package cephadex.brainflex.dto.session;

import java.time.Instant;

import cephadex.brainflex.model.session.InteractiveSessionSettings;
import jakarta.validation.constraints.Future;

public record UpdateScheduledInteractiveSessionRequest(
                @Future Instant scheduledStartAt,
                Instant scheduledEndAt,
                InteractiveSessionSettings settings,
                String reminderEmailTemplate) {
}
