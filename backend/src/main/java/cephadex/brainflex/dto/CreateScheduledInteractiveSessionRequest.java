/**
 * Request body for POST /api/scheduled-interactive-sessions. The host picks a
 * deck, a future start time, the settings the session will boot with, and a
 * list of invitee emails. All fields except `deckId` and `scheduledStartAt`
 * are optional — null `settings` means use the deck's defaults at boot time.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.session.InteractiveSessionSettings;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScheduledInteractiveSessionRequest(
        @NotBlank String deckId,
        @NotNull @Future Instant scheduledStartAt,
        Instant scheduledEndAt,
        InteractiveSessionSettings settings,
        String reminderEmailTemplate,
        List<String> invitedEmails) {
}
