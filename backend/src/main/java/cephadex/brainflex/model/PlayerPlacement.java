/**
 * Final ranked result for one player at the end of a game.
 * Embedded in ShowcaseResult so the full leaderboard snapshot is stored
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

    // Chunk 12 — team mode. Null on individual-mode showcases. Lets the
    // post-game review page render team standings without re-fetching the
    // Showcase document.
    private String teamId;

    // Chunk 13 — copied off the matching ShowcasePlayer at game end so the
    // placement card has the metadata it needs (streak / accuracy / total
    // reactions sent) without re-loading the Showcase document.
    private int longestStreak;
    private double accuracy;
    private int reactionsSent;
}
