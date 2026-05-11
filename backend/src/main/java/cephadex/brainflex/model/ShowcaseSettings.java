/**
 * Configuration options chosen by the host when creating a Showcase.
 * Embedded directly in Showcase so all settings travel with the session document.
 */
package cephadex.brainflex.model;

import cephadex.brainflex.model.enums.GameMode;
import lombok.Data;

@Data
public class ShowcaseSettings {
    private int maxPlayers = 8;
    private int totalRounds = 10;
    private int timePerQuestion = 15; // seconds
    private boolean speedBonus = true; // faster correct answers score higher in SIMULTANEOUS mode
    private boolean allowGuests = true;
    private GameMode gameMode = GameMode.SIMULTANEOUS;

    // If true, rounds never time out — they only end when every player has answered
    // (SIMULTANEOUS) or the host advances (TURN_BASED). UI displays "Unlimited".
    private boolean noTimer = false;

    // If true, players who arrive after the game has started can still join — they
    // wait in the session and pick up at the next round; their score starts at 0.
    private boolean allowLateJoin = false;

    // If false, the live scoreboard hides point values during play; the final
    // standings are only revealed on the game-over screen.
    private boolean showScoresImmediately = true;

    // Preset that distinguishes a game-style Showcase (scoring on, leaderboards, GameOver
    // screen) from a Pulse-style audience poll (scoring off, no leaderboard). Defaults to
    // true; the Pulse creation flow will set it to false once that surface is built.
    private boolean scoringEnabled = true;
}
