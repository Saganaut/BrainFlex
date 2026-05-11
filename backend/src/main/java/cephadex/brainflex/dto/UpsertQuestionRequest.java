/**
 * Request body for creating or updating a question within a content pack.
 * Used by both POST /api/decks/{id}/questions and PUT …/questions/{qId}.
 *
 * Type-aware validation lives in the @AssertTrue methods below so a single request shape
 * can carry both MCQ and TEXT_INPUT questions without two separate endpoints.
 */
package cephadex.brainflex.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.QuestionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertQuestionRequest(
        QuestionType type,
        @NotBlank @Size(max = 500) String questionText,
        List<@NotBlank String> options,
        Integer correctAnswer,
        @Size(max = 200) String correctAnswerText,
        @Min(10) @Max(1000) int pointValue,
        @Min(5) @Max(120) int timeLimit,
        Difficulty difficulty) {

    /** Defaults to MULTIPLE_CHOICE so existing clients that omit `type` continue to work. */
    public QuestionType resolvedType() {
        return type == null ? QuestionType.MULTIPLE_CHOICE : type;
    }

    @JsonIgnore
    @AssertTrue(message = "Multiple choice questions require 2–4 options and a valid correctAnswer index")
    public boolean isMultipleChoiceValid() {
        if (resolvedType() != QuestionType.MULTIPLE_CHOICE) return true;
        if (options == null || options.size() < 2 || options.size() > 4) return false;
        if (correctAnswer == null || correctAnswer < 0 || correctAnswer >= options.size()) return false;
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Type-in questions require a non-blank correctAnswerText")
    public boolean isTextInputValid() {
        if (resolvedType() != QuestionType.TEXT_INPUT) return true;
        return correctAnswerText != null && !correctAnswerText.isBlank();
    }
}
