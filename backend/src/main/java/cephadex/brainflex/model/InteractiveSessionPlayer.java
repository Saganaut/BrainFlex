/**
 * Represents a player who has joined an active InteractiveSession.
 * Embedded in InteractiveSession.players so player state (score, answers) lives
 * inside the session document and is updated atomically with game state.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class InteractiveSessionPlayer {
    /** Denormalized player display snapshot — userId / name / pictureUrl /
     *  guest. Frozen at join time so the lobby and leaderboard render
     *  consistently even if the player's profile changes mid-game. */
    private UserSnapshot user;

    /** Convenience read access through {@link #user}. Kept Lombok-style so internal call
     *  sites that index by user id read naturally; {@code @JsonIgnore} prevents the
     *  delegating accessors from leaking back into the wire shape (only the nested
     *  {@code user} snapshot is serialized). */
    @JsonIgnore public String getUserId()      { return user == null ? null : user.userId(); }
    @JsonIgnore public String getUserName()    { return user == null ? null : user.name(); }
    @JsonIgnore public String getPictureUrl()  { return user == null ? null : user.pictureUrl(); }
    @JsonIgnore public boolean isGuest()       { return user != null && user.guest(); }

    private int score = 0;

    // Spring Security principal name captured at join time so the interactiveSession
    // service can route per-player STOMP messages (e.g. the shuffled round
    // payload from InteractiveSessionService.broadcastRoundStart) without a User lookup
    // on every broadcast. Format: googleId for OAuth users, "guest:<userId>"
    // for guests — same convention resolveUserId() reverses in the service.
    private String principalName;

    // Chunk 12 — populated only when the interactiveSession runs in team mode.
    private String teamId;
    private List<PlayerAnswer> answers = new ArrayList<>();

    // Votes cast in VOTE phase of Best Answer rounds (one per best-answer
    // element this player voted on). Mirrors `answers` — both are bounded by
    // deckSnapshot.size() and travel inside the interactiveSession document.
    private List<RoundVote> votes = new ArrayList<>();

    private LocalDateTime joinedAt = LocalDateTime.now();

    // Chunk 11 — engagement counter; populated by ReactionsEnabled flows.
    private int reactionsSent = 0;

    // Chunk 13 — Kahoot-style preset avatars chosen in the lobby. avatarKey
    // references one of AvatarService's preset keys (e.g. "fox-orange") and
    // is distinct from pictureUrl (which is the user's real avatar — used by
    // the host view and post-game review). colorTag is the design-token name
    // that drives the player's accent color in lobby + leaderboard tiles.
    private String avatarKey;
    private String colorTag;

    // Chunk 13 — streak + accuracy. currentStreak resets to 0 on the first
    // wrong answer; longestStreak monotonically tracks the high-water mark
    // across the whole game. accuracy = correctAnswers / answeredQuestions
    // (0.0 when no questions answered yet). speedBonusTotal aggregates the
    // per-answer speedBonusAwarded so the placement card can break out base
    // points vs speed bonus at game end.
    private int currentStreak = 0;
    private int longestStreak = 0;
    private double accuracy = 0.0;
    private int speedBonusTotal = 0;

    // Chunk 13 — lateJoin is true when the player joined after the game
    // transitioned LOBBY → IN_PROGRESS (only possible with
    // settings.allowLateJoin). disconnected mirrors PresenceService's WS
    // tracking; lastSeenAt is the most recent heartbeat or message tick.
    private boolean lateJoin = false;
    private boolean disconnected = false;
    private LocalDateTime lastSeenAt;
}
