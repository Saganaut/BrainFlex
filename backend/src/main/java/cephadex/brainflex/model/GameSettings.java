/**
 * Configuration options chosen by the host when creating a GameSession.
 * Embedded directly in GameSession so all settings travel with the session document.
 */
package cephadex.brainflex.model;

import cephadex.brainflex.model.enums.GameMode;
import lombok.Data;

@Data
public class GameSettings {
    private int maxPlayers = 8;
    private int totalRounds = 10;
    private int timePerQuestion = 15; // seconds
    private boolean speedBonus = true; // faster correct answers score higher in SIMULTANEOUS mode
    private boolean allowGuests = true;
    private GameMode gameMode = GameMode.SIMULTANEOUS;
}
