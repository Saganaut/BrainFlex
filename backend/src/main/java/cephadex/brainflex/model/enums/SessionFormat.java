/**
 * Top-level chrome selector for a live interactive session.
 *
 *   GAME         — persistent leaderboard, score animations, podium at round
 *                  end, GameOver placement screen at session end. The surface
 *                  where game-board sub-variants eventually live.
 *   PRESENTATION — no persistent leaderboard; round-end focuses on aggregated
 *                  data (charts, distributions, word clouds); SessionSummary
 *                  screen at the end aggregates all responses.
 *
 * Lives on the deck as the *default* (Deck.defaultSessionFormat) and on the
 * session as the *actual* (InteractiveSession.format) — the session value is
 * authoritative once the session is created. Every element kind is permitted
 * in both formats; the format only controls which UI shell renders the chrome.
 *
 * {@code scoringEnabled} is intentionally independent of this enum — a
 * PRESENTATION can still award points; a GAME can include unscored breaks.
 *
 * Legacy {@code DeckPreset.PULSE} documents deserialize to PRESENTATION via
 * the {@link com.fasterxml.jackson.annotation.JsonAlias} below; on next save
 * the field is back-filled with the new name.
 */
package cephadex.brainflex.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionFormat {
    GAME,
    PRESENTATION;

    /**
     * Accepts the retired {@code DeckPreset.PULSE} value (a Pulse-shaped
     * deck is just a PRESENTATION with {@code scoringEnabled = false}) so
     * pre-migration Mongo documents and any in-flight clients stay readable.
     */
    @JsonCreator
    public static SessionFormat fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "GAME" -> GAME;
            case "PRESENTATION", "PULSE" -> PRESENTATION;
            default -> throw new IllegalArgumentException("Unknown SessionFormat: " + value);
        };
    }
}
