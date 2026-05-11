/** Answer for MCQ — the chosen option's id (stable across shuffles). */
package cephadex.brainflex.model.answer;

public record McqAnswer(String optionId) implements AnswerPayload {
}
