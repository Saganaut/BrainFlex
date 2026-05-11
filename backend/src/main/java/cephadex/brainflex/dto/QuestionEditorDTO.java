/**
 * Full deck-element representation returned to the deck owner in the editor.
 * Unlike QuestionDTO (used during gameplay), this includes correctAnswer/correctAnswerText
 * so the creator can view and edit the answer key. SLIDE elements use the same shape
 * with answer fields left null.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.QuestionType;

public record QuestionEditorDTO(
        String id,
        ElementKind kind,
        QuestionType type,
        Double position,
        String title,
        String questionText,
        List<String> options,
        int correctAnswer,
        String correctAnswerText,
        int pointValue,
        int timeLimit,
        Difficulty difficulty,
        String imageUrl) {

    public QuestionEditorDTO(Question q) {
        this(
                q.getId(),
                q.getKind() == null ? ElementKind.QUESTION : q.getKind(),
                q.getType(),
                q.getPosition(),
                q.getTitle(),
                q.getQuestionText(),
                q.getOptions(),
                q.getCorrectAnswer(),
                q.getCorrectAnswerText(),
                q.getPointValue(),
                q.getTimeLimit(),
                q.getDifficulty(),
                q.getImageUrl());
    }
}
