/**
 * A single player's submission for one element. The polymorphic `payload`
 * carries the type-specific response (option id, text, ranking, coordinates,
 * etc.) — the scorer branches on the payload variant to compute correctness
 * and point value.
 *
 * Embedded inside ShowcasePlayer so all answers for a player live with their
 * session record.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import cephadex.brainflex.model.answer.AnswerPayload;
import lombok.Data;

@Data
public class PlayerAnswer {
    private String elementId;
    private AnswerPayload payload;       // see model.answer.* for variants
    private boolean correct;
    private int pointsAwarded;
    private LocalDateTime answeredAt;
}
