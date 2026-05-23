/**
 * Unit tests for {@link DeckCommentService}.
 *
 * The interesting behaviors are:
 *   - replies must point at top-level comments (depth=2 cap)
 *   - soft-delete keeps the row + replaces the body with "[removed]"
 *   - upvote toggles in both directions, idempotent under repeats
 *   - only the author can edit / delete
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.model.deck.DeckComment;
import cephadex.brainflex.repository.DeckCommentRepository;

@ExtendWith(MockitoExtension.class)
class DeckCommentServiceTest {

    @Mock
    private DeckCommentRepository commentRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserImageHydrator userImageHydrator;
    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private DeckCommentService deckCommentService;

    private User author(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUserName(name);
        u.setPictureUrl("https://example.com/" + id + ".png");
        return u;
    }

    @Test
    void create_TopLevel_Persists() {
        when(commentRepository.save(any(DeckComment.class))).thenAnswer(i -> i.getArgument(0));

        DeckComment row = deckCommentService.create(
                "deck-1", author("user-1", "kevin"), "Hello", null);

        assertEquals("Hello", row.getBody());
        assertEquals("user-1", row.getAuthorUserId());
        assertEquals("kevin", row.getAuthorName());
        assertFalse(row.isDeleted());
    }

    @Test
    void create_Reply_AttachesToTopLevel() {
        DeckComment parent = new DeckComment();
        parent.setId("comment-parent");
        parent.setDeckId("deck-1");
        parent.setParentCommentId(null);
        when(commentRepository.findById("comment-parent")).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(DeckComment.class))).thenAnswer(i -> i.getArgument(0));

        DeckComment row = deckCommentService.create(
                "deck-1", author("user-2", "alice"), "good point", "comment-parent");

        assertEquals("comment-parent", row.getParentCommentId());
    }

    @Test
    void create_RejectsRepliesOfReplies() {
        DeckComment depth1 = new DeckComment();
        depth1.setId("comment-depth-1");
        depth1.setDeckId("deck-1");
        depth1.setParentCommentId("comment-root");
        when(commentRepository.findById("comment-depth-1")).thenReturn(Optional.of(depth1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> deckCommentService.create(
                "deck-1", author("user-1", "kevin"), "deeper reply", "comment-depth-1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_RejectsParentOnDifferentDeck() {
        DeckComment parent = new DeckComment();
        parent.setId("comment-parent");
        parent.setDeckId("deck-2");
        parent.setParentCommentId(null);
        when(commentRepository.findById("comment-parent")).thenReturn(Optional.of(parent));

        assertThrows(ResponseStatusException.class, () -> deckCommentService.create(
                "deck-1", author("user-1", "kevin"), "wrong deck", "comment-parent"));
    }

    @Test
    void create_RejectsBlankBody() {
        assertThrows(ResponseStatusException.class, () -> deckCommentService.create(
                "deck-1", author("user-1", "kevin"), "   ", null));
        assertThrows(ResponseStatusException.class, () -> deckCommentService.create(
                "deck-1", author("user-1", "kevin"), null, null));
    }

    @Test
    void edit_OwnComment_UpdatesBodyAndMarksEdited() {
        DeckComment row = newCommentBy("user-1");
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));
        when(commentRepository.save(any(DeckComment.class))).thenAnswer(i -> i.getArgument(0));

        DeckComment edited = deckCommentService.edit(
                "deck-1", "comment-1", author("user-1", "kevin-new"), "updated");

        assertEquals("updated", edited.getBody());
        assertTrue(edited.isEdited());
        // Display snapshot refreshed.
        assertEquals("kevin-new", edited.getAuthorName());
    }

    @Test
    void edit_OtherUserComment_IsForbidden() {
        DeckComment row = newCommentBy("user-1");
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deckCommentService.edit("deck-1", "comment-1",
                        author("user-2", "alice"), "hacked"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(commentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void softDelete_OwnComment_PreservesRow_BlanksBody() {
        DeckComment row = newCommentBy("user-1");
        row.setBody("original");
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));
        when(commentRepository.save(any(DeckComment.class))).thenAnswer(i -> i.getArgument(0));

        DeckComment deleted = deckCommentService.softDelete(
                "deck-1", "comment-1", author("user-1", "kevin"));

        assertTrue(deleted.isDeleted());
        // Body replaced server-side so a malicious client can't display the
        // original text by hiding the deleted-flag check.
        assertEquals("[removed]", deleted.getBody());
    }

    @Test
    void softDelete_AlreadyDeleted_IsNoOp() {
        DeckComment row = newCommentBy("user-1");
        row.setDeleted(true);
        row.setBody("[removed]");
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));

        DeckComment result = deckCommentService.softDelete(
                "deck-1", "comment-1", author("user-1", "kevin"));

        assertTrue(result.isDeleted());
        verify(commentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void toggleUpvote_AddsAndRemovesIdempotently() {
        DeckComment row = newCommentBy("user-1");
        row.setUpvoterUserIds(new HashSet<>());
        row.setUpvotes(0);
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));

        // Add
        DeckComment first = deckCommentService.toggleUpvote("deck-1", "comment-1", "user-2");
        assertEquals(1, first.getUpvotes());
        assertTrue(first.getUpvoterUserIds().contains("user-2"));

        // Toggle off
        DeckComment second = deckCommentService.toggleUpvote("deck-1", "comment-1", "user-2");
        assertEquals(0, second.getUpvotes());
        assertFalse(second.getUpvoterUserIds().contains("user-2"));

        verify(mongoTemplate, org.mockito.Mockito.times(2))
                .updateFirst(any(Query.class), any(Update.class), eq(DeckComment.class));
    }

    @Test
    void toggleUpvote_RequiresUser() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deckCommentService.toggleUpvote("deck-1", "comment-1", null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void requireComment_RejectsCrossDeckLookup() {
        DeckComment row = newCommentBy("user-1");
        row.setDeckId("deck-1");
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(row));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deckCommentService.edit("deck-OTHER", "comment-1",
                        author("user-1", "kevin"), "stuff"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private static DeckComment newCommentBy(String userId) {
        DeckComment row = new DeckComment();
        row.setId("comment-1");
        row.setDeckId("deck-1");
        row.setAuthor(UserSnapshot.of(userId, null));
        row.setBody("hello");
        row.setUpvoterUserIds(new HashSet<>());
        return row;
    }
}
