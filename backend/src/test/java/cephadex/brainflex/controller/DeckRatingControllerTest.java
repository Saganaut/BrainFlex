/**
 * Smoke tests for the deck rating + comment endpoints on {@link DeckController}.
 *
 * Services are mocked via {@code @MockitoBean} so this stays a fast controller
 * slice — the underlying logic is covered separately in
 * {@link cephadex.brainflex.service.DeckRatingServiceTest} and
 * {@link cephadex.brainflex.service.DeckCommentServiceTest}.
 */
package cephadex.brainflex.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cephadex.brainflex.config.AdminProperties;
import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckComment;
import cephadex.brainflex.model.deck.DeckRating;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.DeckCommentService;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckRatingService;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class DeckRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckService deckService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private DeckImageHydrationService deckImageHydrationService;
    @MockitoBean
    private DeckTagHydrationService deckTagHydrationService;
    @MockitoBean
    private DeckFavoriteService deckFavoriteService;
    @MockitoBean
    private DeckRatingService deckRatingService;
    @MockitoBean
    private DeckCommentService deckCommentService;
    @MockitoBean
    private DeckRepository deckRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private AdminProperties adminProperties;

    private User caller;
    private Deck deck;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        caller.setPictureUrl("https://example.com/kevin.png");
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));

        deck = new Deck();
        deck.setId("deck-1");
        deck.getContent().setName("My Deck");
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setCreatorUserId("other-user");
        deck.setAverageRating(4.2);
        deck.setRatingCount(5);
        when(deckService.getViewable(any(), eq("deck-1"))).thenReturn(deck);
    }

    @Test
    void rateDeck_UpsertsAndReturnsRow() throws Exception {
        DeckRating row = new DeckRating();
        row.setId("rating-1");
        row.setDeckId("deck-1");
        row.setUserId("user-1");
        row.setStars(5);
        row.setReview("great");
        when(deckRatingService.upsert("deck-1", "user-1", 5, "great")).thenReturn(row);

        mockMvc.perform(put("/api/decks/deck-1/rating")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\":5,\"review\":\"great\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.review").value("great"))
                .andExpect(jsonPath("$.user.name").value("kevin"));

        verify(deckRatingService).upsert("deck-1", "user-1", 5, "great");
    }

    @Test
    void rateDeck_ValidatesStars() throws Exception {
        mockMvc.perform(put("/api/decks/deck-1/rating")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMyRating_ReturnsNoContent() throws Exception {
        when(deckRatingService.delete("deck-1", "user-1")).thenReturn(true);

        mockMvc.perform(delete("/api/decks/deck-1/rating").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void listRatings_EchoesAggregatesAndDistribution() throws Exception {
        DeckRating a = new DeckRating();
        a.setId("r-a");
        a.setDeckId("deck-1");
        a.setUserId("user-2");
        a.setStars(5);
        a.setReview("loved it");
        Page<DeckRating> page = new PageImpl<>(List.of(a), Pageable.unpaged(), 1);
        when(deckRatingService.listForDeck(eq("deck-1"), any(Pageable.class))).thenReturn(page);
        User author = new User();
        author.setId("user-2");
        author.setUserName("alice");
        when(userRepository.findAllById(org.mockito.ArgumentMatchers.<Iterable<String>>any()))
                .thenReturn(List.of(author));

        mockMvc.perform(get("/api/decks/deck-1/ratings").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stars").value(5))
                .andExpect(jsonPath("$.items[0].user.name").value("alice"))
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.ratingCount").value(5))
                .andExpect(jsonPath("$.starDistribution[4]").value(1));
    }

    @Test
    void getDeck_AuthenticatedRater_PopulatesMyRating() throws Exception {
        DeckRating mine = new DeckRating();
        mine.setStars(4);
        when(deckRatingService.findMine("deck-1", "user-1")).thenReturn(Optional.of(mine));
        when(deckFavoriteService.isFavorited("user-1", "deck-1")).thenReturn(false);

        mockMvc.perform(get("/api/decks/deck-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRating").value(4))
                .andExpect(jsonPath("$.isRatedByMe").value(true));
    }

    @Test
    void getDeck_NoRating_LeavesMyRatingNull() throws Exception {
        when(deckRatingService.findMine("deck-1", "user-1")).thenReturn(Optional.empty());
        when(deckFavoriteService.isFavorited("user-1", "deck-1")).thenReturn(false);

        mockMvc.perform(get("/api/decks/deck-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRatedByMe").value(false))
                .andExpect(jsonPath("$.myRating").doesNotExist());
    }

    @Test
    void postComment_CreatesAndReturnsDTO() throws Exception {
        DeckComment row = new DeckComment();
        row.setId("comment-1");
        row.setDeckId("deck-1");
        row.setAuthor(UserSnapshot.of("user-1", "kevin"));
        row.setBody("first");
        row.setUpvoterUserIds(new HashSet<>());
        when(deckCommentService.create(eq("deck-1"), eq(caller), eq("first"), eq(null)))
                .thenReturn(row);

        mockMvc.perform(post("/api/decks/deck-1/comments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"first\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("first"))
                .andExpect(jsonPath("$.upvotes").value(0));
    }

    @Test
    void listComments_AttachesReplyCounts() throws Exception {
        DeckComment row = new DeckComment();
        row.setId("comment-1");
        row.setDeckId("deck-1");
        row.setAuthor(UserSnapshot.of("user-2", "alice"));
        row.setBody("nice deck");
        row.setUpvoterUserIds(Set.of("user-1"));
        row.setUpvotes(1);
        Page<DeckComment> page = new PageImpl<>(List.of(row), Pageable.unpaged(), 1);
        when(deckCommentService.listTopLevel(eq("deck-1"), any(Pageable.class))).thenReturn(page);
        when(deckCommentService.countReplies("deck-1", "comment-1")).thenReturn(3L);

        mockMvc.perform(get("/api/decks/deck-1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("comment-1"))
                .andExpect(jsonPath("$.items[0].replyCount").value(3))
                // caller "user-1" is in the upvoter set, so upvotedByMe is true
                .andExpect(jsonPath("$.items[0].upvotedByMe").value(true));
    }

    @Test
    void toggleCommentUpvote_DelegatesToService() throws Exception {
        DeckComment row = new DeckComment();
        row.setId("comment-1");
        row.setDeckId("deck-1");
        row.setAuthor(UserSnapshot.of("user-2", null));
        row.setBody("nice");
        row.setUpvoterUserIds(Set.of("user-1"));
        row.setUpvotes(1);
        when(deckCommentService.toggleUpvote("deck-1", "comment-1", "user-1")).thenReturn(row);
        when(deckCommentService.countReplies("deck-1", "comment-1")).thenReturn(0L);

        mockMvc.perform(post("/api/decks/deck-1/comments/comment-1/upvote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1))
                .andExpect(jsonPath("$.upvotedByMe").value(true));
    }
}
