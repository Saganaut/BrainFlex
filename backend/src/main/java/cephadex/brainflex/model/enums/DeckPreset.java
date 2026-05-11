/**
 * Advisory hint stored on a Deck for how it's intended to be played. The
 * create-showcase flow uses this to pre-fill defaults (scoringEnabled, etc.)
 * but the host can still override at create time.
 *
 *   GAME         — scored, leaderboard, GameOver screen
 *   PULSE        — unscored audience polling
 *   PRESENTATION — slide-driven, no scoring, host paces manually
 */
package cephadex.brainflex.model.enums;

public enum DeckPreset {
    GAME,
    PULSE,
    PRESENTATION
}
