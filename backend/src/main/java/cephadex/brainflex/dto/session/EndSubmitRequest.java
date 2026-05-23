/**
 * Body of /app/interactive-session/{code}/endSubmit — chunk 25 host action.
 * The element id pins the action to a specific round so a stale "end submit"
 * (sent after the round already advanced) is a no-op rather than closing the
 * wrong question. Mirrors {@link RevealNowRequest}.
 */
package cephadex.brainflex.dto.session;

public record EndSubmitRequest(String elementId) {
}
