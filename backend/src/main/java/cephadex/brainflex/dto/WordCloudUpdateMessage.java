/**
 * Broadcast on /topic/showcase/{roomCode}/wordCloud during a Word Cloud round.
 *
 * Sent every time a player submits during SUBMIT (so the cloud animates live)
 * and one final time on REVEAL with the locked-in counts. The payload carries
 * the aggregated word -> count map produced by {@link
 * cephadex.brainflex.service.WordCloudAggregator}; the frontend re-renders
 * the cloud from this map alone, so no per-player word lists leak to clients.
 */
package cephadex.brainflex.dto;

import java.util.Map;

public record WordCloudUpdateMessage(
        int round,
        String elementId,
        Map<String, Integer> counts) {
}
