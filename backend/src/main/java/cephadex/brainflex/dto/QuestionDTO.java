/**
 * Safe representation of a deck element sent to clients during a showcase round.
 * Intentionally omits correctAnswer / correctAnswerText so clients cannot read the
 * answer from the WebSocket payload before submitting their own. SLIDE elements
 * carry only their display fields; QUESTION elements carry their options.
 *
 * For MCQ rounds where the host enabled `shuffleMcqOptions`, the options here
 * are the SHUFFLED order — the same order every client + the server agree on
 * for the lifetime of this question in this showcase.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.QuestionType;

public record QuestionDTO(
        String id,
        ElementKind kind,
        QuestionType type,
        String title,
        String questionText,
        List<String> options,
        int pointValue,
        int timeLimit,
        String imageUrl) {

    public QuestionDTO(Question question) {
        this(question, question.getOptions());
    }

    /** Variant that overrides the option list — used to broadcast a shuffled MCQ. */
    public QuestionDTO(Question question, List<String> options) {
        this(
                question.getId(),
                question.getKind() == null ? ElementKind.QUESTION : question.getKind(),
                question.getType(),
                question.getTitle(),
                question.getQuestionText(),
                options,
                question.getPointValue(),
                question.getTimeLimit(),
                question.getImageUrl());
    }
}
