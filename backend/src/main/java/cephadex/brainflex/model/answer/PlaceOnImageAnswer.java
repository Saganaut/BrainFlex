/** Normalized 0..1 point coordinates for a PLACE_ON_IMAGE question. */
package cephadex.brainflex.model.answer;

public record PlaceOnImageAnswer(double x, double y) implements AnswerPayload {
}
