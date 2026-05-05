/**
 * Final ranked result for one player at the end of a game.
 * Embedded in GameResult so the full leaderboard snapshot is stored
 * alongside the session reference without extra lookups.
 */
package cephadex.brainflex.model;

import lombok.Data;

@Data
public class PlayerPlacement {
    private String userId;
    private String userName;
    private boolean isGuest;
    private int finalScore;
    private int placement; // 1 = first place
    private int correctAnswers;
    private int totalQuestions;
}
