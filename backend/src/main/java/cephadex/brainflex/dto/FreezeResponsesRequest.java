/**
 * Body of /app/interactive-session/{code}/freeze — chunk 24 host action.
 * {@code mode} must be ACCEPTING_RESPONSES or NOT_ACCEPTING_RESPONSES; any
 * other value is rejected. {@code elementId} pins the change to a specific
 * round so a late freeze doesn't bleed into the next question.
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.ResponseMode;

public record FreezeResponsesRequest(String elementId, ResponseMode mode) {
}
