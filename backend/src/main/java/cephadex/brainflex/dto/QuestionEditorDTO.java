/**
 * Full question representation returned to the pack owner in the editor.
 * Unlike QuestionDTO (which is used during gameplay), this includes correctAnswer
 * so the creator can view and edit the answer key.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.QuestionType;

public record QuestionEditorDTO(
        String id,
        String questionText,
        List<String> options,
        int correctAnswer,
        int pointValue,
        int timeLimit,
        QuestionType type,
        Difficulty difficulty,
        String imageUrl) {

    public QuestionEditorDTO(Question q) {
        this(
                q.getId(),
                q.getQuestionText(),
                q.getOptions(),
                q.getCorrectAnswer(),
                q.getPointValue(),
                q.getTimeLimit(),
                q.getType(),
                q.getDifficulty(),
                q.getImageUrl());
    }
}
