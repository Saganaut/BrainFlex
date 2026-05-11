/**
 * Configuration knobs the host picks when starting a Showcase. Embedded inside
 * the Showcase document so settings travel with the session for its lifetime.
 *
 * `timePerQuestion = 0` is the "unlimited" signal — questions wait for all
 * players to answer (SIMULTANEOUS) or for the host to advance (TURN_BASED).
 * Per-element `displaySeconds` overrides this value unconditionally; this is
 * only the fallback when an element left `displaySeconds = 0`.
 */
package cephadex.brainflex.model;

import cephadex.brainflex.model.enums.GameMode;
import lombok.Data;

@Data
public class ShowcaseSettings {
    private int maxPlayers = 8;
    private int totalRounds = 10;
    private int timePerQuestion = 15;   // 0 = unlimited
    private boolean speedBonus = true;
    private boolean allowGuests = true;
    private GameMode gameMode = GameMode.SIMULTANEOUS;
    private boolean allowLateJoin = false;
    private boolean showScoresImmediately = true;
    private boolean scoringEnabled = true;   // false = Pulse preset
}
