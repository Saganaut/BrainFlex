/**
 * Configuration knobs the host picks when starting a InteractiveSession. Embedded inside
 * the InteractiveSession document so settings travel with the session for its lifetime.
 *
 * `timePerQuestion = 0` is the "unlimited" signal — questions wait for all
 * players to answer (SIMULTANEOUS) or for the host to advance (TURN_BASED).
 * Per-element `displaySeconds` overrides this value unconditionally; this is
 * only the fallback when an element left `displaySeconds = 0`.
 */
package cephadex.brainflex.model;

import cephadex.brainflex.model.enums.GameMode;
import lombok.Data;

//TODO: We don't have a clear delimitation between Deck and InteractiveSession, a interactiveSession uses a deck but the delimitation is blurry
@Data
public class InteractiveSessionSettings {
    private int maxPlayers = 8;
    private int totalRounds = 10;
    private int timePerQuestion = 15; // 0 = unlimited
    private boolean speedBonus = true;
    private boolean allowGuests = true;
    private GameMode gameMode = GameMode.SIMULTANEOUS;
    private boolean allowLateJoin = false;
    private boolean showScoresImmediately = true;
    private boolean scoringEnabled = true; // false = Pulse preset

    // Audience engagement (chunk 11). Per-slide DeckElement.reactionsEnabled()
    // is layered on top of this flag: reactions only fire when the interactiveSession
    // flag is on AND the current element opts in.
    private boolean reactionsEnabled = true;
    private boolean chatEnabled = true;

    // Team mode (chunk 12). When `teamMode` is true, createInteractiveSession seeds
    // `teamCount` default teams; players are auto-balanced into them on join
    // unless `autoBalanceTeams` is disabled (in which case the join payload
    // must carry a teamId). teamCount is ignored when the host pre-creates
    // teams via the CRUD endpoints.
    private boolean teamMode = false;
    private int teamCount = 2;
    private boolean autoBalanceTeams = true;

    // Chunk 13 — live-show polish knobs.
    //
    // shuffleQuestions: pre-shuffle deckSnapshot once at game start (stable
    // for the rest of the show, deterministic on interactiveSession id so reconnects
    // see the same order).
    // shuffleAnswers: default-on per-player shuffle of MCQ options /
    // Ranking items. Per-element question.shuffleOptions still overrides
    // (an author who set it false on a specific question wins).
    // autoAdvance: when true the round advances on a scheduled timer at
    // roundStartedAt + timePerQuestion + podiumDuration — host doesn't have
    // to click "Next".
    // podiumDuration: seconds the round-result screen stays up before the
    // next round when autoAdvance is on.
    // lobbyCountdownSeconds: "Game starts in N..." overlay before the host
    // transitions the lobby to IN_PROGRESS.
    // lobbyMusicAssetId: MediaAsset reference (chunk 19) for ambient lobby
    // music; null = silence.
    // requireFullName: disallow lobby-only nicknames; players must use their
    // registered name (guests are rejected when also set).
    // spectatorsAllowed: viewers without a player slot.
    private boolean shuffleQuestions = false;
    private boolean shuffleAnswers = true;
    private boolean autoAdvance = false;
    private int podiumDuration = 15;
    private int lobbyCountdownSeconds = 5;
    private String lobbyMusicAssetId;
    private boolean requireFullName = false;
    private boolean spectatorsAllowed = false;
}
