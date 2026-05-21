/**
 * Business logic for the deck-favorites join collection.
 *
 * The two write paths — favorite and unfavorite — are idempotent: repeating
 * either is a no-op. We rely on the {@code (userId, deckId)} unique index to
 * collapse a second favorite attempt into a {@link DuplicateKeyException} that
 * we swallow, and on the delete result to tell us whether anything actually
 * disappeared. Only on a real insert / real delete do we bump the
 * {@code Deck.favoriteCount} denorm — otherwise concurrent or duplicate
 * requests would drift the counter.
 *
 * The counter mutation is an atomic Mongo {@code $inc} so two simultaneous
 * favorites on the same deck always settle on the right number; never use
 * read-modify-write for these counters.
 *
 * Public deck lookups also call into here for {@code isFavorited} resolution;
 * batch reads stay one round-trip via {@link #favoritedDeckIds(String, Collection)}.
 */
package cephadex.brainflex.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckFavorite;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.repository.DeckFavoriteRepository;

@Service
public class DeckFavoriteService {

    private final DeckFavoriteRepository favoriteRepository;
    private final MongoTemplate mongoTemplate;
    private final AchievementService achievementService;
    private final ApplicationEventPublisher events;

    public DeckFavoriteService(
            DeckFavoriteRepository favoriteRepository,
            MongoTemplate mongoTemplate,
            AchievementService achievementService,
            ApplicationEventPublisher events) {
        this.favoriteRepository = favoriteRepository;
        this.mongoTemplate = mongoTemplate;
        this.achievementService = achievementService;
        this.events = events;
    }

    /**
     * Records a favorite row for {@code userId / deckId} and atomically bumps
     * the deck's {@code favoriteCount}. Repeated calls return the current
     * count without writing — the unique index swallows the second insert.
     */
    public long favorite(String userId, String deckId) {
        DeckFavorite row = new DeckFavorite();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setDeckId(deckId);
        try {
            favoriteRepository.insert(row);
            long count = incrementFavoriteCount(deckId, 1);
            // Chunk 17 — only check the deck owner's FAVORITES_RECEIVED on a
            // real new favorite (the dup-key branch below already left the
            // counter alone). Per-deck favoriteCount is the trigger value:
            // "have a deck with N favorites" rather than the harder
            // cross-deck sum, which would need an aggregation on every star.
            Deck deck = mongoTemplate.findById(deckId, Deck.class);
            if (deck != null && deck.getCreatorUserId() != null
                    && !deck.getCreatorUserId().equals(userId)) {
                achievementService.evaluate(deck.getCreatorUserId(),
                        AchievementTrigger.FAVORITES_RECEIVED,
                        (int) Math.min(count, Integer.MAX_VALUE), null, deckId);
                events.publishEvent(new NotificationEvents.DeckFavoritedEvent(deckId, userId));
            }
            return count;
        } catch (DuplicateKeyException ignored) {
            // Already favorited — read back the current count so the caller
            // can echo it without an extra round trip.
            return readFavoriteCount(deckId);
        }
    }

    /**
     * Removes the favorite row if it exists and atomically decrements the
     * deck's {@code favoriteCount}. Repeated calls are a no-op.
     */
    public long unfavorite(String userId, String deckId) {
        long removed = favoriteRepository.deleteByUserIdAndDeckId(userId, deckId);
        if (removed == 0) {
            return readFavoriteCount(deckId);
        }
        return incrementFavoriteCount(deckId, -1);
    }

    /** True if {@code userId} has favorited {@code deckId}. */
    public boolean isFavorited(String userId, String deckId) {
        if (userId == null || deckId == null) return false;
        return favoriteRepository.findByUserIdAndDeckId(userId, deckId).isPresent();
    }

    /**
     * Batch lookup: returns the subset of {@code deckIds} that {@code userId}
     * has favorited. One Mongo round-trip; safe to call from list endpoints
     * for caller-aware {@code isFavorited} hydration.
     */
    public Set<String> favoritedDeckIds(String userId, Collection<String> deckIds) {
        if (userId == null || deckIds == null || deckIds.isEmpty()) return Set.of();
        List<DeckFavorite> hits = favoriteRepository.findAllByUserIdAndDeckIdIn(userId, deckIds);
        Set<String> result = new HashSet<>(hits.size());
        for (DeckFavorite hit : hits) result.add(hit.getDeckId());
        return result;
    }

    /**
     * Paginated favorites for a user, newest-first. Returns the join rows;
     * the caller maps them back to {@link Deck} documents.
     */
    public org.springframework.data.domain.Page<DeckFavorite> listForUser(
            String userId, org.springframework.data.domain.Pageable pageable) {
        return favoriteRepository.findAllByUserId(userId, pageable);
    }

    /**
     * Reconciliation helper for admin use: rewrites {@code Deck.favoriteCount}
     * to the authoritative {@code count(deck_favorites WHERE deckId = ...)}.
     * Returns the new value so the admin can verify drift was the cause.
     */
    public long recountFavorites(String deckId) {
        long authoritative = favoriteRepository.countByDeckId(deckId);
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(deckId)),
                new Update().set("favoriteCount", (int) authoritative),
                Deck.class);
        return authoritative;
    }

    private long incrementFavoriteCount(String deckId, int delta) {
        Deck after = mongoTemplate.findAndModify(
                new Query(Criteria.where("_id").is(deckId)),
                new Update().inc("favoriteCount", delta),
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                Deck.class);
        if (after == null) return 0;
        // Clamp at zero: if a counter ever drifts negative (e.g. a manual
        // delete bypassed the service) we don't want to surface that.
        int favoriteCount = Math.max(0, after.getFavoriteCount());
        if (favoriteCount != after.getFavoriteCount()) {
            mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(deckId)),
                    new Update().set("favoriteCount", 0),
                    Deck.class);
        }
        return favoriteCount;
    }

    private long readFavoriteCount(String deckId) {
        Deck deck = mongoTemplate.findById(deckId, Deck.class);
        return deck == null ? 0 : deck.getFavoriteCount();
    }
}
