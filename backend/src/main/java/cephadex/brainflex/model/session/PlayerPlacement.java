/**
 * Final ranked result for one player at the end of a game.
 * Embedded in InteractiveSessionResult so the full leaderboard snapshot is stored
 * alongside the session reference without extra lookups.
 */
package cephadex.brainflex.model.session;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import cephadex.brainflex.model.shared.UserSnapshot;

@Data
public class PlayerPlacement {
    /** Session-scoped public handle copied off the {@link InteractiveSessionPlayer}
     *  at game end. This is what the broadcast InteractiveSessionEndedMessage
     *  carries on the wire. */
    private String playerId;

    /** Denormalized player display snapshot — userId / name / guest. pictureUrl
     *  is copied off the {@link InteractiveSessionPlayer} at game end so the
     *  placement card has its own copy for replays. Server-side only — the
     *  broadcast path projects this through {@link PublicUserSnapshot}. */
    private UserSnapshot user;

    @JsonIgnore public String getUserId()   { return user == null ? null : user.userId(); }
    @JsonIgnore public String getUserName() { return user == null ? null : user.name(); }
    @JsonIgnore public boolean isGuest()    { return user != null && user.guest(); }

    private int finalScore;
    private int placement; // 1 = first place
    private int correctAnswers;
    private int totalQuestions;

    // Chunk 12 — team mode. Null on individual-mode interactiveSessions. Lets the
    // post-game review page render team standings without re-fetching the
    // InteractiveSession document.
    private String teamId;

    // Chunk 13 — copied off the matching InteractiveSessionPlayer at game end so the
    // placement card has the metadata it needs (streak / accuracy / total
    // reactions sent) without re-loading the InteractiveSession document.
    private PlayerEndStats endStats = PlayerEndStats.empty();
    private int speedBonusTotal;
}
