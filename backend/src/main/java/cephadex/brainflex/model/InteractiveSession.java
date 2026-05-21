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
import cephadex.brainflex.model.enums.InteractiveSessionPhase;
import cephadex.brainflex.model.enums.InteractiveSessionStatus;
import cephadex.brainflex.model.enums.SessionFormat;
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

    private InteractiveSessionStatus status = InteractiveSessionStatus.LOBBY;

    // Top-level chrome for this run. Frozen at create time from
    // CreateInteractiveSessionRequest.format → deck.defaultSessionFormat → GAME.
    // After creation this value is authoritative; the deck is never re-read for
    // this field. Same "freeze at start" guarantee as deckSnapshot.
    private SessionFormat format = SessionFormat.GAME;

    // The active phase within the current round. Most question kinds live in
    // SUBMIT only; Best-Answer-mode questions cycle SUBMIT → VOTE → REVEAL.
    private InteractiveSessionPhase phase = InteractiveSessionPhase.SUBMIT;

    private String hostUserId;

    // Source deck reference + frozen snapshot of its elements at create time.
    // All gameplay (broadcasting, scoring, review) reads from deckSnapshot —
    // never re-fetches the live Deck — so mid-interactiveSession deck edits don't
    // desync.
    private String deckId;
    private List<DeckElement> deckSnapshot = new ArrayList<>();

    private InteractiveSessionSettings settings = new InteractiveSessionSettings();
    private List<InteractiveSessionPlayer> players = new ArrayList<>();

    // Chunk 12 — team mode. The teamMode / autoBalanceTeams toggles live on
    // settings; teams is the live roster, empty unless team mode is on.
    private List<Team> teams = new ArrayList<>();

    private int currentRound = 0; // index into deckSnapshot

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime roundStartedAt; // timestamp current round began; speed-bonus reference

    // Chunk 13 — lobby + host display additions. host* fields are denormalised
    // so the lobby header doesn't need a User lookup on every refresh.
    // customRoomCode is the host-typed override — active roomCode falls back to
    // the auto-generated value when blank.
    private String customRoomCode;
    private String hostName;
    private String hostAvatarUrl;
    private int spectatorCount = 0;
    private LocalDateTime lobbyOpenedAt = LocalDateTime.now();

    // Populated by chunk 16 (Deck analytics) when a CSV/PDF report is exported
    // off the finished session; null until then.
    private String exportedReportUrl;

    // Chunk 24 — host-runtime overrides that live for the lifetime of this
    // session only. Never mutates the source deck or the frozen element
    // snapshot. Stored on the session document so the override survives
    // host reconnects and the eventual replay/review screen.
    //
    // revealedElementIds: elementIds for which the host has manually clicked
    // "Reveal results" during an ON_CLICK round. Subsequent reveal calls
    // are no-ops (idempotent). Survives advanceRound so the review can
    // tell which rounds were instant vs. manual.
    // elementResponseModeOverrides: elementId → ResponseMode, applied by
    // submitAnswer before the element's authored mode. Cleared on round
    // advance only via natural lifecycle (no explicit clear step).
    // TODO: THink abotu if these fields are necessary
    private java.util.Set<String> revealedElementIds = new java.util.HashSet<>();
    private java.util.Map<String, cephadex.brainflex.model.enums.ResponseMode> elementResponseModeOverrides = new java.util.HashMap<>();
}
