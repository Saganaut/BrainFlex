/**
 * Body of /app/interactive-session/{code}/reveal — chunk 24 host action.
 * The element id pins the action to a specific round so a stale "reveal"
 * (sent after the round already advanced) is a no-op rather than reveals
 * the wrong question's data.
 */
package cephadex.brainflex.dto.session;

public record RevealNowRequest(String elementId) {
}
