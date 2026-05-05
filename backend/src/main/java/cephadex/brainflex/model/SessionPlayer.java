/**
 * Represents a player who has joined an active GameSession.
 * Embedded in GameSession.players so player state (score, answers) lives
 * inside the session document and is updated atomically with game state.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SessionPlayer {
    private String userId;
    private String userName;
    private String pictureUrl;
    private boolean isGuest;
    private int score = 0;
    private List<PlayerAnswer> answers = new ArrayList<>();
    private LocalDateTime joinedAt = LocalDateTime.now();
}
