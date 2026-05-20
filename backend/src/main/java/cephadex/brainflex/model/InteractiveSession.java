/**
 * The central document for an active or completed interactiveSession.
 *
 * Tracks all runtime state: room code, players, current round, and the
 * frozen `deckSnapshot` of elements taken at create time so authoring the
 * source deck mid-interactiveSession can't desync clients. Unique indexes on roomCode
 * and inviteToken support the two join flows (code + link).
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.InteractiveSessionPhase;
import lombok.Data;

@Data
@Document(collection = "interactive_sessions")
public class InteractiveSession {
    @Id
    private String id;

    @Indexed(unique = true)
    private String roomCode;

    @Indexed(unique = true)
    private String inviteToken;

    private GameStatus status = GameStatus.LOBBY;

    // The active phase within the current round. Most question kinds live in
    // SUBMIT only; Best-Answer-mode questions cycle SUBMIT → VOTE → REVEAL.
    private InteractiveSessionPhase phase = InteractiveSessionPhase.SUBMIT;

    private String hostUserId;

    // Source deck reference + frozen snapshot of its elements at create time.
    // All gameplay (broadcasting, scoring, review) reads from deckSnapshot —
    // never re-fetches the live Deck — so mid-interactiveSession deck edits don't desync.
    private String deckId;
    private List<DeckElement> deckSnapshot = new ArrayList<>();

    // Denormalized presentation assets from the source deck for chrome rendering.
    private String deckCoverImageUrl;
    private String deckBackgroundImageUrl;
    private String themeId;

    private InteractiveSessionSettings settings = new InteractiveSessionSettings();
    private List<InteractiveSessionPlayer> players = new ArrayList<>();

    // Chunk 12 — team mode. `teamMode` mirrors InteractiveSessionSettings.teamMode for
    // convenience (so scoring/broadcast code doesn't have to reach through
    // settings on every round) but `settings.teamMode` remains the source of
    // truth at create time. `teams` is empty unless team mode is on.
    private boolean teamMode = false;
    private boolean autoBalanceTeams = true;
    private List<Team> teams = new ArrayList<>();

    private int currentRound = 0;   // index into deckSnapshot

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime roundStartedAt;   // timestamp current round began; speed-bonus reference

    // Chunk 13 — lobby + host display additions. anonymousMode hides real names
    // on the leaderboard and reveal (avatarKey + colorTag take over). The
    // host* fields are denormalised so the lobby header doesn't need a User
    // lookup on every refresh. customRoomCode is the host-typed override —
    // active roomCode falls back to the auto-generated value when blank.
    private boolean anonymousMode = false;
    private String customRoomCode;
    private String hostName;
    private String hostAvatarUrl;
    private boolean allowReJoin = true;
    private int spectatorCount = 0;
    private LocalDateTime lobbyOpenedAt = LocalDateTime.now();

    // Populated by chunk 16 (Deck analytics) when a CSV/PDF report is exported
    // off the finished session; null until then.
    private String exportedReportUrl;
}
