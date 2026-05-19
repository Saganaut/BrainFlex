/**
 * Unit tests for DeckService's discovery + lifecycle surface (chunk 02).
 *
 * Repository, AuthorizationService, TagService, and MongoTemplate are all
 * mocked so this stays a fast unit test. The publish lifecycle methods only
 * touch the repository; the {@code incrementPlayCount} / {@code
 * incrementViewCount} helpers funnel through MongoTemplate.updateFirst, so
 * we assert the right Update document hits the mock.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.repository.DeckRepository;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private TagService tagService;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private DeckCollaboratorService deckCollaboratorService;

    @InjectMocks private DeckService deckService;

    private User owner;
    private Deck deck;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("owner-1");

        deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        deck.setPublishStatus(PublishStatus.DRAFT);
    }

    // ---- publish lifecycle ----

    @Test
    void publish_FromDraft_StampsPublishedAt() {
        when(authorizationService.requireDeckEditable("deck-1", owner)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck result = deckService.publish("deck-1", owner);

        assertEquals(PublishStatus.PUBLISHED, result.getPublishStatus());
        assertNotNull(result.getPublishedAt());
    }

    @Test
    void publish_WhenAlreadyPublished_IsNoOp() {
        deck.setPublishStatus(PublishStatus.PUBLISHED);
        LocalDateTime original = LocalDateTime.of(2024, 1, 1, 0, 0);
        deck.setPublishedAt(original);
        when(authorizationService.requireDeckEditable("deck-1", owner)).thenReturn(deck);

        Deck result = deckService.publish("deck-1", owner);

        assertSame(deck, result);
        assertEquals(original, result.getPublishedAt());
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    void publish_FromArchived_PreservesOriginalPublishedAt() {
        deck.setPublishStatus(PublishStatus.ARCHIVED);
        LocalDateTime original = LocalDateTime.of(2024, 3, 1, 0, 0);
        deck.setPublishedAt(original);
        when(authorizationService.requireDeckEditable("deck-1", owner)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck result = deckService.publish("deck-1", owner);

        assertEquals(PublishStatus.PUBLISHED, result.getPublishStatus());
        assertEquals(original, result.getPublishedAt());
    }

    @Test
    void unpublish_FromPublished_FlipsToDraft() {
        deck.setPublishStatus(PublishStatus.PUBLISHED);
        deck.setPublishedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(authorizationService.requireDeckEditable("deck-1", owner)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck result = deckService.unpublish("deck-1", owner);

        assertEquals(PublishStatus.DRAFT, result.getPublishStatus());
        // publishedAt history is preserved for chunk 16 analytics.
        assertNotNull(result.getPublishedAt());
    }

    @Test
    void archive_FromPublished_FlipsToArchived() {
        deck.setPublishStatus(PublishStatus.PUBLISHED);
        when(authorizationService.requireDeckEditable("deck-1", owner)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck result = deckService.archive("deck-1", owner);

        assertEquals(PublishStatus.ARCHIVED, result.getPublishStatus());
    }

    // ---- counter writes ----

    @Test
    void incrementPlayCount_IssuesAtomicIncAndSet() {
        deckService.incrementPlayCount("deck-1");

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Deck.class));
        var modifiers = updateCaptor.getValue().getUpdateObject();
        assertEquals(1, modifiers.get("$inc", org.bson.Document.class).getInteger("playCount"));
        assertNotNull(modifiers.get("$set", org.bson.Document.class).get("lastPlayedAt"));
    }

    @Test
    void incrementPlayCount_BlankDeckId_IsIgnored() {
        deckService.incrementPlayCount("");
        deckService.incrementPlayCount(null);

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(Class.class));
    }

    @Test
    void incrementViewCount_IssuesAtomicInc() {
        deckService.incrementViewCount("deck-1");

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Deck.class));
        var modifiers = updateCaptor.getValue().getUpdateObject();
        assertEquals(1, modifiers.get("$inc", org.bson.Document.class).getInteger("viewCount"));
    }

    @Test
    void incrementViewCount_BlankDeckId_IsIgnored() {
        deckService.incrementViewCount(null);
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(Class.class));
    }

    @Test
    void getViewable_AnonymousCallerOnPublicDeck_DoesNotIncrementOrFail() {
        deck.setVisibility(cephadex.brainflex.model.enums.DeckVisibility.PUBLIC);
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));

        Deck result = deckService.getViewable(Optional.empty(), "deck-1");

        assertSame(deck, result);
        // getViewable doesn't touch the view counter — DeckController does
        // that after-the-fact so anonymous reads on a public deck still
        // bypass it via the controller's isOwner check.
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(Class.class));
    }
}
