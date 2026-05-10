/**
 * Request body for creating or updating a question within a content pack.
 * Used by both POST /api/content-packs/{id}/questions and PUT …/questions/{qId}.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertQuestionRequest(
        @NotBlank @Size(max = 500) String questionText,
        @NotNull @Size(min = 2, max = 4) List<@NotBlank String> options,
        @Min(0) @Max(3) int correctAnswer,
        @Min(10) @Max(1000) int pointValue,
        @Min(5) @Max(120) int timeLimit,
        Difficulty difficulty) {
}
