/**
 * Public-facing view of a single {@link cephadex.brainflex.model.deck.DeckRating}.
 * Carries the author's display snapshot so the Reviews tab can render the row
 * without a second user lookup; populated from {@link cephadex.brainflex.model.user.User}
 * at controller-time via {@link cephadex.brainflex.repository.UserRepository}.
 */
package cephadex.brainflex.dto.deck;

import java.time.Instant;

import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.deck.DeckRating;

public record DeckRatingResponse(
        String id,
        String deckId,
        UserSnapshot user,
        int stars,
        String review,
        Instant createdAt,
        Instant updatedAt) {

    public static DeckRatingResponse of(DeckRating rating, String userName, String pictureUrl) {
        return new DeckRatingResponse(
                rating.getId(),
                rating.getDeckId(),
                UserSnapshot.of(rating.getUserId(), userName, pictureUrl),
                rating.getStars(),
                rating.getReview(),
                rating.getCreatedAt(),
                rating.getUpdatedAt());
    }
}
