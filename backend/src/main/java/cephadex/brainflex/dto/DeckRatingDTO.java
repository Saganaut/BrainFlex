/**
 * Public-facing view of a single {@link cephadex.brainflex.model.DeckRating}.
 * Carries the author's display snapshot so the Reviews tab can render the row
 * without a second user lookup; populated from {@link cephadex.brainflex.model.User}
 * at controller-time via {@link cephadex.brainflex.repository.UserRepository}.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.DeckRating;

public record DeckRatingDTO(
        String id,
        String deckId,
        String userId,
        String userName,
        String userPictureUrl,
        int stars,
        String review,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DeckRatingDTO of(DeckRating rating, String userName, String pictureUrl) {
        return new DeckRatingDTO(
                rating.getId(),
                rating.getDeckId(),
                rating.getUserId(),
                userName,
                pictureUrl,
                rating.getStars(),
                rating.getReview(),
                rating.getCreatedAt(),
                rating.getUpdatedAt());
    }
}
