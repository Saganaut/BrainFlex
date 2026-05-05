/**
 * Records a single player's answer to one question during a game session.
 * Embedded inside SessionPlayer so all answers for a player are co-located
 * with their session data rather than in a separate collection.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PlayerAnswer {
    private String questionId;
    private int selectedOption; // index into Question.options; -1 means timed out (no answer)
    private boolean isCorrect;
    private int pointsAwarded;
    private LocalDateTime answeredAt; // used to calculate speed bonus in SIMULTANEOUS mode
}
