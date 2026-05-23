/**
 * Unit tests for {@link DeckFavoriteService}.
 *
 * The repository and MongoTemplate are mocked so this stays a fast unit test.
 * The key behaviors under test are:
 *   - {@code favorite()} bumps the counter on first call, no-ops on duplicate
 *   - {@code unfavorite()} only decrements when a row actually disappears
 *   - {@code favoritedDeckIds()} batches lookups for list-endpoint hydration
 *   - {@code recountFavorites()} reconciles the denorm from the join rows
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckFavorite;
import cephadex.brainflex.repository.DeckFavoriteRepository;

@ExtendWith(MockitoExtension.class)
class DeckFavoriteServiceTest {

    @Mock private DeckFavoriteRepository favoriteRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private AchievementService achievementService;
    @Mock private org.springframework.context.ApplicationEventPublisher events;

    @InjectMocks private DeckFavoriteService deckFavoriteService;

    @Test
    void favorite_FirstTime_InsertsRowAndIncrementsCounter() {
        Deck after = new Deck();
        after.setId("deck-1");
        after.setFavoriteCount(3);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class)))
                .thenReturn(after);

        long count = deckFavoriteService.favorite("user-1", "deck-1");

        assertEquals(3, count);

        ArgumentCaptor<DeckFavorite> rowCaptor = ArgumentCaptor.forClass(DeckFavorite.class);
        verify(favoriteRepository).insert(rowCaptor.capture());
        assertEquals("user-1", rowCaptor.getValue().getUserId());
        assertEquals("deck-1", rowCaptor.getValue().getDeckId());

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        assertEquals(1, updateCaptor.getValue().getUpdateObject().get("$inc", org.bson.Document.class).getInteger("favoriteCount"));
    }

    @Test
    void favorite_DuplicateKey_IsNoOp_AndReturnsCurrentCount() {
        when(favoriteRepository.insert(any(DeckFavorite.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        Deck existing = new Deck();
        existing.setId("deck-1");
        existing.setFavoriteCount(7);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(existing);

        long count = deckFavoriteService.favorite("user-1", "deck-1");

        assertEquals(7, count);
        // The duplicate path must NOT bump the counter — otherwise repeated
        // taps on the heart would drift it.
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class));
    }

    @Test
    void unfavorite_WhenRowExists_DecrementsCounter() {
        when(favoriteRepository.deleteByUserIdAndDeckId("user-1", "deck-1")).thenReturn(1L);
        Deck after = new Deck();
        after.setId("deck-1");
        after.setFavoriteCount(2);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class)))
                .thenReturn(after);

        long count = deckFavoriteService.unfavorite("user-1", "deck-1");

        assertEquals(2, count);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Deck.class));
        assertEquals(-1, updateCaptor.getValue().getUpdateObject().get("$inc", org.bson.Document.class).getInteger("favoriteCount"));
    }

    @Test
    void unfavorite_WhenRowMissing_IsNoOp() {
        when(favoriteRepository.deleteByUserIdAndDeckId("user-1", "deck-1")).thenReturn(0L);
        Deck existing = new Deck();
        existing.setId("deck-1");
        existing.setFavoriteCount(5);
        when(mongoTemplate.findById("deck-1", Deck.class)).thenReturn(existing);

        long count = deckFavoriteService.unfavorite("user-1", "deck-1");

        assertEquals(5, count);
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Deck.class));
    }

    @Test
    void favoritedDeckIds_BatchesLookup() {
        DeckFavorite a = new DeckFavorite();
        a.setUserId("user-1");
        a.setDeckId("deck-a");
        DeckFavorite b = new DeckFavorite();
        b.setUserId("user-1");
        b.setDeckId("deck-c");
        when(favoriteRepository.findAllByUserIdAndDeckIdIn(eq("user-1"), anyList()))
                .thenReturn(List.of(a, b));

        Set<String> result = deckFavoriteService.favoritedDeckIds(
                "user-1", List.of("deck-a", "deck-b", "deck-c"));

        assertEquals(Set.of("deck-a", "deck-c"), result);
    }

    @Test
    void favoritedDeckIds_EmptyInputs_ShortCircuit() {
        assertTrue(deckFavoriteService.favoritedDeckIds(null, List.of("deck-a")).isEmpty());
        assertTrue(deckFavoriteService.favoritedDeckIds("user-1", List.of()).isEmpty());
    }

    @Test
    void isFavorited_Delegates() {
        when(favoriteRepository.findByUserIdAndDeckId("user-1", "deck-1"))
                .thenReturn(Optional.of(new DeckFavorite()));
        assertTrue(deckFavoriteService.isFavorited("user-1", "deck-1"));

        when(favoriteRepository.findByUserIdAndDeckId("user-1", "deck-2"))
                .thenReturn(Optional.empty());
        assertFalse(deckFavoriteService.isFavorited("user-1", "deck-2"));
    }

    @Test
    void recountFavorites_WritesAuthoritativeValueToDeck() {
        when(favoriteRepository.countByDeckId("deck-1")).thenReturn(4L);

        long result = deckFavoriteService.recountFavorites("deck-1");

        assertEquals(4L, result);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Deck.class));
        assertEquals(4, updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class).getInteger("favoriteCount"));
    }
}
