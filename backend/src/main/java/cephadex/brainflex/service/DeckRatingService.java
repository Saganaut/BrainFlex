/**
 * Business logic for the per-(deck, user) star rating + optional review.
 *
 * The hot path is {@link #upsert(String, String, int, String)}: a single call
 * either inserts a new rating or updates the caller's existing one. In both
 * cases the {@code Deck.averageRating} + {@code ratingCount} denorms are
 * maintained incrementally so list views never need to recompute by scanning
 * the {@code deck_ratings} collection. The math:
 *
 *   insert:  ratingCount' = ratingCount + 1
 *            ratingSum'   = ratingSum + stars
 *            avg'         = ratingSum' / ratingCount'
 *
 *   update:  ratingSum'   = ratingSum + (newStars - oldStars)
 *            avg'         = ratingSum' / ratingCount     (count unchanged)
 *
 *   delete:  ratingCount' = ratingCount - 1
 *            ratingSum'   = ratingSum - oldStars
 *            avg'         = ratingCount' > 0 ? ratingSum' / ratingCount' : 0
 *
 * {@code Deck} does not carry an explicit running-sum field, so we reconstruct
 * the previous sum from {@code averageRating * ratingCount}. That's safe as
 * long as the average is always written by this code path (every other caller
 * reads it only). If the denorm ever drifts, {@link #recountFor(String)} can
 * recompute from the join collection.
 *
 * Counter writes are atomic {@code findAndModify} updates so two simultaneous
 * ratings on the same deck always settle on the right number; never use
 * read-modify-write for these.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckRating;
import cephadex.brainflex.repository.DeckRatingRepository;

@Service
public class DeckRatingService {

    static final int MAX_REVIEW_CHARS = 2000;

    private final DeckRatingRepository ratingRepository;
    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher events;

    public DeckRatingService(
            DeckRatingRepository ratingRepository,
            MongoTemplate mongoTemplate,
            ApplicationEventPublisher events) {
        this.ratingRepository = ratingRepository;
        this.mongoTemplate = mongoTemplate;
        this.events = events;
    }

    /**
     * Inserts a new rating or updates the caller's existing one, then
     * propagates the change to {@code Deck.averageRating} / {@code ratingCount}.
     * Returns the persisted rating row.
     */
    public DeckRating upsert(String deckId, String userId, int stars, String review) {
        validateStars(stars);
        String normalizedReview = normalizeReview(review);

        Optional<DeckRating> existing = ratingRepository.findByDeckIdAndUserId(deckId, userId);
        if (existing.isPresent()) {
            DeckRating row = existing.get();
            int oldStars = row.getStars();
            row.setStars(stars);
            row.setReview(normalizedReview);
            DeckRating saved = ratingRepository.save(row);
            if (oldStars != stars) {
                applyDelta(deckId, stars - oldStars, 0);
            }
            return saved;
        }

        DeckRating row = new DeckRating();
        row.setId(UUID.randomUUID().toString());
        row.setDeckId(deckId);
        row.setUserId(userId);
        row.setStars(stars);
        row.setReview(normalizedReview);
        try {
            DeckRating saved = ratingRepository.insert(row);
            applyDelta(deckId, stars, 1);
            events.publishEvent(new NotificationEvents.DeckRatingCreatedEvent(deckId, stars, userId));
            return saved;
        } catch (DuplicateKeyException dup) {
            // Lost the race with another concurrent insert from the same user.
            // Read back the winner and recurse into the update branch so the
            // caller still sees their requested stars/review reflected.
            return upsert(deckId, userId, stars, normalizedReview);
        }
    }

    /**
     * Remove the caller's rating, if any. Returns true iff a row actually
     * disappeared so the controller can echo the right state.
     */
    public boolean delete(String deckId, String userId) {
        Optional<DeckRating> existing = ratingRepository.findByDeckIdAndUserId(deckId, userId);
        if (existing.isEmpty()) return false;
        int oldStars = existing.get().getStars();
        long removed = ratingRepository.deleteByDeckIdAndUserId(deckId, userId);
        if (removed == 0) return false;
        applyDelta(deckId, -oldStars, -1);
        return true;
    }

    /** Caller's own rating, or empty. */
    public Optional<DeckRating> findMine(String deckId, String userId) {
        if (userId == null || deckId == null) return Optional.empty();
        return ratingRepository.findByDeckIdAndUserId(deckId, userId);
    }

    /** Paginated list of ratings (newest first) for a deck. */
    public Page<DeckRating> listForDeck(String deckId, Pageable pageable) {
        return ratingRepository.findAllByDeckId(deckId, pageable);
    }

    /**
     * Reconciliation helper: rewrites {@code averageRating} + {@code ratingCount}
     * to the authoritative values from the join collection. Useful when the
     * denorm drifts after a manual delete or a crash mid-write.
     */
    public RatingSnapshot recountFor(String deckId) {
        List<DeckRating> all = ratingRepository.findAllByDeckId(deckId, Pageable.unpaged()).getContent();
        int count = all.size();
        long sum = 0;
        for (DeckRating r : all) sum += r.getStars();
        double average = count == 0 ? 0.0 : roundOneDecimal(sum / (double) count);
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(deckId)),
                new Update()
                        .set("averageRating", average)
                        .set("ratingCount", count),
                Deck.class);
        return new RatingSnapshot(average, count);
    }

    public record RatingSnapshot(double averageRating, int ratingCount) {}

    private void applyDelta(String deckId, int starsDelta, int countDelta) {
        // We need the *current* averageRating and ratingCount under the
        // mutation to derive the new average without scanning the collection.
        // findAndModify gives us those atomically — first apply ratingCount
        // and ratingSum reconstruction, then derive avg in a second update.
        Deck before = mongoTemplate.findById(deckId, Deck.class);
        if (before == null) return;
        int oldCount = Math.max(0, before.getRatingCount());
        double oldAverage = before.getAverageRating();
        long oldSum = Math.round(oldAverage * oldCount);
        int newCount = Math.max(0, oldCount + countDelta);
        long newSum = Math.max(0, oldSum + starsDelta);
        double newAverage = newCount == 0 ? 0.0 : roundOneDecimal(newSum / (double) newCount);
        mongoTemplate.findAndModify(
                new Query(Criteria.where("_id").is(deckId)),
                new Update()
                        .set("averageRating", newAverage)
                        .set("ratingCount", newCount),
                FindAndModifyOptions.options().returnNew(true),
                Deck.class);
    }

    static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static void validateStars(int stars) {
        if (stars < 1 || stars > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stars must be in 1..5");
        }
    }

    private static String normalizeReview(String review) {
        if (review == null) return null;
        String trimmed = review.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > MAX_REVIEW_CHARS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "review must be at most " + MAX_REVIEW_CHARS + " characters");
        }
        return trimmed;
    }
}
