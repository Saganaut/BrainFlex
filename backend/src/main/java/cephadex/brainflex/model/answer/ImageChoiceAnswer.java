/** Answer for IMAGE_CHOICE — same shape as MCQ but distinguishable per kind. */
package cephadex.brainflex.model.answer;

import java.util.List;

public record ImageChoiceAnswer(List<String> optionIds) implements AnswerPayload {
}
