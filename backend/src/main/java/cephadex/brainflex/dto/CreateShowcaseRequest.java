/**
 * Request body for POST /api/showcases. The host picks a deck; settings fall
 * back to that deck's `defaultSettings` (which in turn fall back to platform
 * defaults). `timePerQuestion = 0` disables the round timer for questions
 * (slides always have their own display timer).
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.GameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShowcaseRequest(
        @NotBlank String deckId,
        GameMode gameMode,                              // null → deck default
        @Min(1) @Max(60) Integer totalRounds,           // null → deck default
        @Min(0) @Max(120) Integer timePerQuestion,      // null → deck default; 0 = unlimited
        Boolean speedBonus,
        Boolean allowGuests,
        @Min(2) @Max(20) Integer maxPlayers,
        Boolean allowLateJoin,
        Boolean showScoresImmediately,
        Boolean scoringEnabled,
        Boolean reactionsEnabled,
        Boolean chatEnabled,
        // Chunk 12 — team mode.
        Boolean teamMode,
        @Min(2) @Max(8) Integer teamCount,
        Boolean autoBalanceTeams,
        // Chunk 13 — host-set polish knobs. customRoomCode is 4–8 chars from
        // the same ROOM_CODE alphabet used for auto-generation so we can fall
        // back to the auto-code without revalidating client characters. Blank
        // / null = generate an auto code as before.
        @Pattern(regexp = "^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{4,8}$",
                message = "customRoomCode must be 4–8 chars from A-Z (no I/O) and 2-9")
        @Size(max = 8) String customRoomCode,
        Boolean anonymousMode,
        Boolean shuffleQuestions,
        Boolean shuffleAnswers,
        Boolean autoAdvance,
        @Min(0) @Max(60) Integer podiumDuration,
        @Min(0) @Max(30) Integer lobbyCountdownSeconds,
        Boolean requireFullName,
        Boolean spectatorsAllowed) {
}
