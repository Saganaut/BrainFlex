/**
 * The central document for an active or completed game session.
 * Tracks all runtime state: room code, players, current round, and embedded settings.
 * Unique indexes on roomCode and inviteToken support the two join flows (code + link).
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;
import lombok.Data;

@Data
@Document(collection = "showcases")
public class Showcase {
    @Id
    private String id;

    @Indexed(unique = true)
    private String roomCode; // 6-char uppercase alphanumeric, used for in-person join

    @Indexed(unique = true)
    private String inviteToken; // UUID, used for link-based join

    private GameType type = GameType.TRIVIA;
    private GameStatus status = GameStatus.LOBBY;

    private String hostUserId; // must be a registered user

    private String deckId; // reference to Deck

    // Denormalized snapshot of the deck's presentation assets at the time the
    // showcase was created. Decouples gameplay rendering from a live deck lookup
    // and locks the look-and-feel even if the author edits the deck afterwards.
    private String deckCoverImageUrl;
    private String deckBackgroundImageUrl;

    private ShowcaseSettings settings = new ShowcaseSettings();
    private List<ShowcasePlayer> players = new ArrayList<>();

    private List<String> questionIds = new ArrayList<>(); // ordered draw from the content pack
    private int currentRound = 0; // 0-indexed; increments as rounds complete

    // Per-question option shuffles, populated lazily when an MCQ round is broadcast
    // and reused thereafter so every client + the server agree on the order.
    private Map<String, McqShuffle> mcqShuffles = new HashMap<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime roundStartedAt; // timestamp when the current round began; used for speed bonus
}
