/**
 * The central document for an active or completed game session.
 * Tracks all runtime state: room code, players, current round, and embedded settings.
 * Unique indexes on roomCode and inviteToken support the two join flows (code + link).
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;
import lombok.Data;

@Data
@Document(collection = "game_sessions")
public class GameSession {
    @Id
    private String id;

    @Indexed(unique = true)
    private String roomCode; // 6-char uppercase alphanumeric, used for in-person join

    @Indexed(unique = true)
    private String inviteToken; // UUID, used for link-based join

    private GameType type = GameType.TRIVIA;
    private GameStatus status = GameStatus.LOBBY;

    private String hostUserId; // must be a registered user

    private String contentPackId; // reference to ContentPack

    private GameSettings settings = new GameSettings();
    private List<SessionPlayer> players = new ArrayList<>();

    private List<String> questionIds = new ArrayList<>(); // ordered draw from the content pack
    private int currentRound = 0; // 0-indexed; increments as rounds complete

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime roundStartedAt; // timestamp when the current round began; used for speed bonus
}
