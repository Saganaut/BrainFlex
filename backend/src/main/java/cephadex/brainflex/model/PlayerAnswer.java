/**
 * Records a single player's answer to one question during a game session.
 * Embedded inside ShowcasePlayer so all answers for a player are co-located
 * with their session data rather than in a separate collection.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PlayerAnswer {
    private String questionId;
    // MCQ: index into Question.options; -1 means timed out. TEXT_INPUT: ignored (use textAnswer).
    private int selectedOption;
    // TEXT_INPUT: raw text the player submitted; null when timed out or for MCQ rounds.
    private String textAnswer;
    private boolean isCorrect;
    private int pointsAwarded;
    private LocalDateTime answeredAt; // used to calculate speed bonus in SIMULTANEOUS mode
}
