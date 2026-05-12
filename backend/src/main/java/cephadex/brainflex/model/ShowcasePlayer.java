/**
 * Represents a player who has joined an active Showcase.
 * Embedded in Showcase.players so player state (score, answers) lives
 * inside the session document and is updated atomically with game state.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ShowcasePlayer {
    private String userId;
    private String userName;
    private String pictureUrl;
    private boolean isGuest;
    private int score = 0;
    private List<PlayerAnswer> answers = new ArrayList<>();

    // Votes cast in VOTE phase of Best Answer rounds (one per best-answer
    // element this player voted on). Mirrors `answers` — both are bounded by
    // deckSnapshot.size() and travel inside the showcase document.
    private List<RoundVote> votes = new ArrayList<>();

    private LocalDateTime joinedAt = LocalDateTime.now();
}
