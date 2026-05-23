/**
 * Request body for {@code PUT /api/decks/{id}/rating}. {@code stars} is
 * validated 1..5 at the service layer; {@code review} is optional and capped
 * at 2000 chars.
 */
package cephadex.brainflex.dto.deck;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateDeckRequest(
        @Min(1) @Max(5) int stars,
        String review) {
}
