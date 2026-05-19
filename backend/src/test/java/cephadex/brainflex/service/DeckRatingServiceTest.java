/**
 * Unit tests for {@link DeckRatingService}.
 *
 * The repository and MongoTemplate are mocked. The two behaviors most worth
 * pinning down are:
 *   - {@code upsert} routes inserts vs. updates correctly and propagates the
 *     right delta to {@code Deck.averageRating} / {@code ratingCount}
 *   - {@code delete} only acts when a row actually disappears, and reduces
 *     the denorm by the deleted row's stars
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckRating;
import cephadex.brainflex.repository.DeckRatingRepository;

@ExtendWith(MockitoExtension.class)
class DeckRatingServiceTest {

    @Mock private DeckRatingRepository ratingRepository;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks private DeckRatingService deckRatingService;

    @Test
    void upsert_FirstTime_InsertsAndBumpsDenorms() {
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.empty());
        when(ratingRepository.insert(any(DeckRating.class))).thenAnswer(i -> i.getArgument(0));
        Deck before = new Deck();
        before.setId("deck-1");
        before.setAverageRating(4.0);
        before.setRatingCount(2);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(before);

        DeckRating saved = deckRatingService.upsert("deck-1", "user-1", 5, "great");

        assertEquals(5, saved.getStars());
        assertEquals("great", saved.getReview());

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(
                any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        org.bson.Document set = updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        // ratingSum was 4.0 * 2 = 8 → 8 + 5 = 13; new count = 3; avg = 13/3 = 4.3
        assertEquals(4.3, set.getDouble("averageRating"));
        assertEquals(3, set.getInteger("ratingCount"));
    }

    @Test
    void upsert_SecondTime_UpdatesStarsInPlace_AndAppliesDelta() {
        DeckRating existing = new DeckRating();
        existing.setId("rating-1");
        existing.setDeckId("deck-1");
        existing.setUserId("user-1");
        existing.setStars(2);
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(DeckRating.class))).thenAnswer(i -> i.getArgument(0));
        Deck before = new Deck();
        before.setId("deck-1");
        before.setAverageRating(3.0);
        before.setRatingCount(4);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(before);

        DeckRating saved = deckRatingService.upsert("deck-1", "user-1", 5, "still good");

        assertEquals(5, saved.getStars());
        // No insert path on update.
        verify(ratingRepository, never()).insert(any(DeckRating.class));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(
                any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        org.bson.Document set = updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        // oldSum = 12, delta = +3, count unchanged = 4 → 15/4 = 3.8 (rounded to 3.8)
        assertEquals(3.8, set.getDouble("averageRating"));
        assertEquals(4, set.getInteger("ratingCount"));
    }

    @Test
    void upsert_NoStarsChange_SkipsDenormUpdate() {
        DeckRating existing = new DeckRating();
        existing.setId("rating-1");
        existing.setDeckId("deck-1");
        existing.setUserId("user-1");
        existing.setStars(4);
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(DeckRating.class))).thenAnswer(i -> i.getArgument(0));

        deckRatingService.upsert("deck-1", "user-1", 4, "updated review only");

        // Pure review-only edit must NOT touch the deck-level denorm.
        verify(mongoTemplate, never()).findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class));
    }

    @Test
    void upsert_RejectsOutOfRangeStars() {
        assertThrows(ResponseStatusException.class,
                () -> deckRatingService.upsert("deck-1", "user-1", 0, null));
        assertThrows(ResponseStatusException.class,
                () -> deckRatingService.upsert("deck-1", "user-1", 6, null));
    }

    @Test
    void upsert_RejectsOverlongReview() {
        String tooLong = "x".repeat(2001);
        assertThrows(ResponseStatusException.class,
                () -> deckRatingService.upsert("deck-1", "user-1", 3, tooLong));
    }

    @Test
    void delete_WhenRatingExists_DecrementsCountAndSum() {
        DeckRating existing = new DeckRating();
        existing.setStars(4);
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.deleteByDeckIdAndUserId("deck-1", "user-1")).thenReturn(1L);
        Deck before = new Deck();
        before.setId("deck-1");
        before.setAverageRating(4.0);
        before.setRatingCount(5);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(before);

        assertTrue(deckRatingService.delete("deck-1", "user-1"));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(
                any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        org.bson.Document set = updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        // oldSum = 20, after delete = 16, count = 4 → 16/4 = 4.0
        assertEquals(4.0, set.getDouble("averageRating"));
        assertEquals(4, set.getInteger("ratingCount"));
    }

    @Test
    void delete_WhenLastRating_ZeroesAverage() {
        DeckRating existing = new DeckRating();
        existing.setStars(5);
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.deleteByDeckIdAndUserId("deck-1", "user-1")).thenReturn(1L);
        Deck before = new Deck();
        before.setId("deck-1");
        before.setAverageRating(5.0);
        before.setRatingCount(1);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(before);

        assertTrue(deckRatingService.delete("deck-1", "user-1"));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(
                any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        org.bson.Document set = updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        // Last rating gone → average must be 0.0, not divide-by-zero.
        assertEquals(0.0, set.getDouble("averageRating"));
        assertEquals(0, set.getInteger("ratingCount"));
    }

    @Test
    void delete_WhenNoRating_ReturnsFalse_AndDoesNothing() {
        when(ratingRepository.findByDeckIdAndUserId("deck-1", "user-1"))
                .thenReturn(Optional.empty());

        assertFalse(deckRatingService.delete("deck-1", "user-1"));

        verify(ratingRepository, never()).deleteByDeckIdAndUserId(any(), any());
        verify(mongoTemplate, never()).findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class));
    }

    @Test
    void recountFor_WritesAuthoritativeValuesToDeck() {
        DeckRating a = new DeckRating();
        a.setStars(5);
        DeckRating b = new DeckRating();
        b.setStars(3);
        when(ratingRepository.findAllByDeckId(eq("deck-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a, b)));

        DeckRatingService.RatingSnapshot snap = deckRatingService.recountFor("deck-1");

        assertEquals(2, snap.ratingCount());
        assertEquals(4.0, snap.averageRating());
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Deck.class));
        org.bson.Document set = updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertEquals(4.0, set.getDouble("averageRating"));
        assertEquals(2, set.getInteger("ratingCount"));
    }
}
