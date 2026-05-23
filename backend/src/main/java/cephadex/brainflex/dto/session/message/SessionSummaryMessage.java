/**
 * Broadcast to /topic/interactive-session/{roomCode}/summary when a
 * PRESENTATION-format session ends. The PRESENTATION shell has no
 * leaderboard or placements — instead the host shows aggregated responses
 * across every question (charts, distributions, word clouds, …) and the
 * client renders this message with the same {@code ResultsDisplayType}
 * renderers powering the post-game review screen.
 *
 * GAME sessions emit {@link InteractiveSessionEndedMessage} on /ended
 * instead. The two messages are mutually exclusive per session — clients
 * subscribe to both topics and let the session's frozen
 * {@code format} decide which one they react to.
 */
package cephadex.brainflex.dto.session.message;

import java.util.List;

import cephadex.brainflex.model.element.DeckElement;

public record SessionSummaryMessage(
        // Number of rounds actually played (may be less than totalRounds if
        // the host ended the session early).
        int roundsPlayed,
        // Whether scoring was enabled for at least one element — drives the
        // client-side "Export results" affordance.
        boolean anyScoringEnabled,
        List<RoundSummary> rounds) {

    /**
     * Per-round payload for PRESENTATION end-of-session aggregation. The
     * full element snapshot is included so the renderer has the prompt /
     * options / theme. {@code aggregatedPayloads} is the raw list of player
     * submissions for the round — the front-end aggregator + the existing
     * {@code ResultsDisplayType} component decide how to draw it (pie,
     * bar, word cloud, scatter, …). Empty for slide rounds.
     */
    public record RoundSummary(
            int roundIndex,
            DeckElement element,
            List<Object> aggregatedPayloads) {
    }
}
