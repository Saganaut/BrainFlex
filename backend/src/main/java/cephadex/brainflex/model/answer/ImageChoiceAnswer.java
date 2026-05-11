/** Answer for IMAGE_CHOICE — same shape as MCQ but distinguishable per kind. */
package cephadex.brainflex.model.answer;

public record ImageChoiceAnswer(String optionId) implements AnswerPayload {
}
