/**
 * Per-user denormalized index of finished interactive sessions.
 *
 * Listing "every game I've played" would otherwise need a full
 * {@code interactive_session_results} scan — this collection answers that
 * query with a single indexed seek. One row per (user, session): for any
 * session where the host was also a player, the single row carries
 * {@code wasHost=true}; when the host did not play, a standalone host row
 * is written. The {@code (userId, interactiveSessionId)} unique index makes
 * the writer idempotent and lets the backfill skip already-seeded sessions.
 *
 * Deck-derived fields ({@code deckName}, {@code hostName}, {@code teamName})
 * are denormalized at write time so history rows stay readable after the
 * source deck / team is deleted.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "game_history")
@CompoundIndexes({
        @CompoundIndex(name = "history_user_session_unique_idx", def = "{'userId': 1, 'interactiveSessionId': 1}", unique = true),
        @CompoundIndex(name = "history_user_played_idx", def = "{'userId': 1, 'playedAt': -1}"),
        @CompoundIndex(name = "history_user_deck_played_idx", def = "{'userId': 1, 'deckId': 1, 'playedAt': -1}")
})
public class GameHistoryEntry {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String interactiveSessionId;

    private String deckId;
    private String deckName;

    private String hostUserId;
    private String hostName;

    private int finalScore;
    private int placement;
    private int totalQuestions;
    private int correctAnswers;
    private int longestStreak;
    private int currentStreakAtEnd;
    private double accuracy;
    private int reactionsSent;

    /** {@code session.endedAt - session.startedAt} in ms. Zero when either timestamp is missing. */
    private long durationMs;

    /** Null when the session was not in team mode. */
    private String teamId;
    private String teamName;

    /** True when this row represents the user's role as host of the session. */
    private boolean wasHost;

    /** Snapshot of {@code User.isGuest} at the time the row was written. */
    private boolean wasGuest;

    private LocalDateTime playedAt;
}
