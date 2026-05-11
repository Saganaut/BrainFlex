/**
 * The central document for an active or completed showcase.
 *
 * Tracks all runtime state: room code, players, current round, and the
 * frozen `deckSnapshot` of elements taken at create time so authoring the
 * source deck mid-showcase can't desync clients. Unique indexes on roomCode
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
import cephadex.brainflex.model.enums.ShowcasePhase;
import lombok.Data;

@Data
@Document(collection = "showcases")
public class Showcase {
    @Id
    private String id;

    @Indexed(unique = true)
    private String roomCode;

    @Indexed(unique = true)
    private String inviteToken;

    private GameStatus status = GameStatus.LOBBY;

    // The active phase within the current round. Most question kinds live in
    // SUBMIT only; Best-Answer-mode questions cycle SUBMIT → VOTE → REVEAL.
    private ShowcasePhase phase = ShowcasePhase.SUBMIT;

    private String hostUserId;

    // Source deck reference + frozen snapshot of its elements at create time.
    // All gameplay (broadcasting, scoring, review) reads from deckSnapshot —
    // never re-fetches the live Deck — so mid-showcase deck edits don't desync.
    private String deckId;
    private List<DeckElement> deckSnapshot = new ArrayList<>();

    // Denormalized presentation assets from the source deck for chrome rendering.
    private String deckCoverImageUrl;
    private String deckBackgroundImageUrl;
    private String themeId;

    private ShowcaseSettings settings = new ShowcaseSettings();
    private List<ShowcasePlayer> players = new ArrayList<>();

    private int currentRound = 0;   // index into deckSnapshot

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime roundStartedAt;   // timestamp current round began; speed-bonus reference
}
