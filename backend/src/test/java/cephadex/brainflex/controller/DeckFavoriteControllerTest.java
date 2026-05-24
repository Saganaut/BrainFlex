/**
 * Smoke tests for the favorite/unfavorite endpoints on {@link DeckController}
 * and the favorites list endpoint on {@link UserController}.
 *
 * Services are mocked via @MockitoBean so this stays a fast controller-slice
 * test — the underlying join-row behavior is covered separately in
 * DeckFavoriteServiceTest.
 */
package cephadex.brainflex.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckFavorite;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class DeckFavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckService deckService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private DeckFavoriteService deckFavoriteService;
    @MockitoBean
    private DeckRepository deckRepository;

    private User caller;
    private Deck deck;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));

        deck = new Deck();
        deck.setId("deck-1");
        deck.getContent().setName("My Deck");
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setCreatorUserId("other-user");
    }

    @Test
    void favoriteDeck_ReturnsFavoritedTrue_AndCount() throws Exception {
        when(deckService.getViewable(any(), eq("deck-1"))).thenReturn(deck);
        when(deckFavoriteService.favorite("user-1", "deck-1")).thenReturn(4L);

        mockMvc.perform(post("/api/decks/deck-1/favorite").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value("deck-1"))
                .andExpect(jsonPath("$.isFavorited").value(true))
                .andExpect(jsonPath("$.favoriteCount").value(4));

        verify(deckFavoriteService).favorite("user-1", "deck-1");
    }

    @Test
    void unfavoriteDeck_ReturnsFavoritedFalse_AndCount() throws Exception {
        when(deckFavoriteService.unfavorite("user-1", "deck-1")).thenReturn(2L);

        mockMvc.perform(delete("/api/decks/deck-1/favorite").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value("deck-1"))
                .andExpect(jsonPath("$.isFavorited").value(false))
                .andExpect(jsonPath("$.favoriteCount").value(2));
    }

    @Test
    void recountFavorites_RequiresAdmin() throws Exception {
        // Default @WithMockUser at the class level is ROLE_USER —
        // @PreAuthorize("hasRole('ADMIN')")
        // (chunk 20 migrated this from an in-body adminProperties.isAdmin check)
        // rejects with 403.
        mockMvc.perform(post("/api/decks/deck-1/favorite/recount").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recountFavorites_WhenAdmin_ReturnsAuthoritativeCount() throws Exception {
        when(deckRepository.existsById("deck-1")).thenReturn(true);
        when(deckFavoriteService.recountFavorites("deck-1")).thenReturn(9L);
        when(deckFavoriteService.isFavorited("user-1", "deck-1")).thenReturn(false);

        mockMvc.perform(post("/api/decks/deck-1/favorite/recount").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteCount").value(9));
    }

    @Test
    void getDeck_PopulatesIsFavorited_ForAuthenticatedCaller() throws Exception {
        when(deckService.getViewable(any(), eq("deck-1"))).thenReturn(deck);
        when(deckFavoriteService.isFavorited("user-1", "deck-1")).thenReturn(true);

        mockMvc.perform(get("/api/decks/deck-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("deck-1"))
                .andExpect(jsonPath("$.isFavorited").value(true));
    }

    @Test
    void getDeck_AnonymousCaller_GetsIsFavoritedFalse() throws Exception {
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.empty());
        when(deckService.getViewable(any(), eq("deck-1"))).thenReturn(deck);

        mockMvc.perform(get("/api/decks/deck-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorited").value(false));
    }

    @Test
    void listDecks_BatchesIsFavoritedLookup() throws Exception {
        Deck other = new Deck();
        other.setId("deck-2");
        other.getContent().setName("Other");
        other.setVisibility(DeckVisibility.PUBLIC);
        when(deckService.listPublic()).thenReturn(List.of(deck, other));
        when(deckFavoriteService.favoritedDeckIds(eq("user-1"), anyCollection()))
                .thenReturn(Set.of("deck-2"));

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("deck-1"))
                .andExpect(jsonPath("$[0].isFavorited").value(false))
                .andExpect(jsonPath("$[1].id").value("deck-2"))
                .andExpect(jsonPath("$[1].isFavorited").value(true));
    }

    @Test
    void listMyFavorites_ReturnsHydratedDecks() throws Exception {
        DeckFavorite row = new DeckFavorite();
        row.setId("fav-1");
        row.setUserId("user-1");
        row.setDeckId("deck-1");
        Page<DeckFavorite> page = new PageImpl<>(List.of(row), Pageable.unpaged(), 1);
        when(deckFavoriteService.listForUser(eq("user-1"), any(Pageable.class))).thenReturn(page);
        when(deckRepository.findAllById(List.of("deck-1"))).thenReturn(List.of(deck));

        mockMvc.perform(get("/api/users/me/favorites").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("deck-1"))
                .andExpect(jsonPath("$.items[0].isFavorited").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.CsrfRequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
    }
}
