/**
 * Request body for creating or updating an element within a deck.
 * Used by both POST /api/decks/{id}/questions and PUT …/questions/{qId}.
 *
 * The element kind (QUESTION vs SLIDE) plus the question type (MCQ vs TEXT_INPUT)
 * determine which fields are required — validation lives in the @AssertTrue methods
 * below so a single request shape can carry every element kind without separate
 * endpoints.
 */
package cephadex.brainflex.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.QuestionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpsertQuestionRequest(
        ElementKind kind,
        QuestionType type,
        Double position,
        @Size(max = 200) String title,
        @Size(max = 1000) String questionText,
        List<@Size(max = 200) String> options,
        Integer correctAnswer,
        @Size(max = 200) String correctAnswerText,
        @Min(0) @Max(1000) int pointValue,
        @Min(3) @Max(120) int timeLimit,
        Difficulty difficulty) {

    /** Defaults to QUESTION so existing clients that omit `kind` continue to work. */
    public ElementKind resolvedKind() {
        return kind == null ? ElementKind.QUESTION : kind;
    }

    /** Defaults to MULTIPLE_CHOICE so existing clients that omit `type` continue to work. */
    public QuestionType resolvedType() {
        return type == null ? QuestionType.MULTIPLE_CHOICE : type;
    }

    @JsonIgnore
    @AssertTrue(message = "Element body (questionText) is required")
    public boolean isBodyPresent() {
        return questionText != null && !questionText.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "Multiple choice questions require 2–4 options and a valid correctAnswer index")
    public boolean isMultipleChoiceValid() {
        if (resolvedKind() != ElementKind.QUESTION) return true;
        if (resolvedType() != QuestionType.MULTIPLE_CHOICE) return true;
        if (options == null || options.size() < 2 || options.size() > 4) return false;
        if (correctAnswer == null || correctAnswer < 0 || correctAnswer >= options.size()) return false;
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Type-in questions require a non-blank correctAnswerText")
    public boolean isTextInputValid() {
        if (resolvedKind() != ElementKind.QUESTION) return true;
        if (resolvedType() != QuestionType.TEXT_INPUT) return true;
        return correctAnswerText != null && !correctAnswerText.isBlank();
    }
}
