/**
 * Request body for POST /api/scheduled-interactive-sessions. The host picks a
 * deck, a future start time, the settings the session will boot with, and a
 * list of invitee emails. All fields except `deckId` and `scheduledStartAt`
 * are optional — null `settings` means use the deck's defaults at boot time.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.InteractiveSessionSettings;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScheduledInteractiveSessionRequest(
        @NotBlank String deckId,
        @NotNull @Future LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        InteractiveSessionSettings settings,
        String reminderEmailTemplate,
        List<String> invitedEmails) {
}
