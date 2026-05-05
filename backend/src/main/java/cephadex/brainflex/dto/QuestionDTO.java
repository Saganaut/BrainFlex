/**
 * Safe representation of a Question sent to clients during a game round.
 * Intentionally omits correctAnswer so clients cannot read it from the
 * WebSocket payload before submitting their answer.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.QuestionType;

public record QuestionDTO(
        String id,
        String questionText,
        List<String> options,
        int pointValue,
        int timeLimit,
        QuestionType type,
        String imageUrl) {

    public QuestionDTO(Question question) {
        this(
                question.getId(),
                question.getQuestionText(),
                question.getOptions(),
                question.getPointValue(),
                question.getTimeLimit(),
                question.getType(),
                question.getImageUrl());
    }
}
