/**
 * Smoke tests for {@link DeckCollectionController}: create / read / list /
 * delete / add-deck / remove-deck / reorder.
 *
 * The service layer is mocked so this test exercises wiring (JSON shape,
 * status codes, auth-required endpoints) rather than the underlying
 * collection logic — that lives in DeckCollectionServiceTest once integration
 * tests are split out. Pattern mirrors DeckFavoriteControllerTest.
 */
package cephadex.brainflex.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollection;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.service.DeckCollectionService;
import cephadex.brainflex.service.DeckFavoriteService;
import cephadex.brainflex.service.DeckImageHydrationService;
import cephadex.brainflex.service.DeckTagHydrationService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class DeckCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckCollectionService collectionService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private DeckImageHydrationService deckImageHydrationService;
    @MockitoBean
    private DeckTagHydrationService deckTagHydrationService;
    @MockitoBean
    private DeckFavoriteService deckFavoriteService;

    private User caller;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));
    }

    @Test
    void listMine_ReturnsPaginatedCollections() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        Page<DeckCollection> page = new PageImpl<>(List.of(col), Pageable.unpaged(), 1);
        when(collectionService.listForOwner(eq("user-1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/collections/mine").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("col-1"))
                .andExpect(jsonPath("$.items[0].name").value("Favorites"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCollection_ReturnsDetailWithDecks() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        col.setOwnerUserId("other-user");
        col.setDeckIds(new java.util.ArrayList<>(List.of("deck-1")));
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.getContent().setName("Deck One");
        deck.setVisibility(DeckVisibility.PUBLIC);
        when(collectionService.getViewable(any(), eq("col-1"))).thenReturn(col);
        when(collectionService.resolveDecks(eq(col), any())).thenReturn(List.of(deck));
        when(deckFavoriteService.favoritedDeckIds(eq("user-1"), anyCollection())).thenReturn(Set.of("deck-1"));

        mockMvc.perform(get("/api/collections/col-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("col-1"))
                .andExpect(jsonPath("$.deckCount").value(1))
                .andExpect(jsonPath("$.decks[0].id").value("deck-1"))
                .andExpect(jsonPath("$.decks[0].isFavorited").value(true));

        verify(collectionService).incrementViewCount("col-1");
    }

    @Test
    void getCollection_OwnerSkipsViewCountBump() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        col.setOwnerUserId("user-1");
        when(collectionService.getViewable(any(), eq("col-1"))).thenReturn(col);
        when(collectionService.resolveDecks(eq(col), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/collections/col-1"))
                .andExpect(status().isOk());

        verify(collectionService, org.mockito.Mockito.never()).incrementViewCount(any());
    }

    @Test
    void createCollection_Returns201() throws Exception {
        DeckCollection col = collection("col-2", "New Folder");
        when(collectionService.create(eq(caller), any())).thenReturn(col);

        mockMvc.perform(post("/api/collections")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Folder\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("col-2"))
                .andExpect(jsonPath("$.name").value("New Folder"));
    }

    @Test
    void updateCollection_ReturnsSummary() throws Exception {
        DeckCollection col = collection("col-1", "Renamed");
        when(collectionService.update(eq("col-1"), eq(caller), any())).thenReturn(col);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/collections/col-1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deleteCollection_Returns204() throws Exception {
        mockMvc.perform(delete("/api/collections/col-1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(collectionService).delete("col-1", caller);
    }

    @Test
    void addDeck_AppendsAndReturnsSummary() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        col.setDeckIds(new java.util.ArrayList<>(List.of("deck-1")));
        when(collectionService.addDeck(eq("col-1"), eq(caller), eq("deck-1"), eq(null))).thenReturn(col);

        mockMvc.perform(post("/api/collections/col-1/decks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckId\":\"deck-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckIds[0]").value("deck-1"));
    }

    @Test
    void removeDeck_ReturnsSummary() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        when(collectionService.removeDeck("col-1", caller, "deck-1")).thenReturn(col);

        mockMvc.perform(delete("/api/collections/col-1/decks/deck-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("col-1"));
    }

    @Test
    void reorderDecks_PassesIdsThrough() throws Exception {
        DeckCollection col = collection("col-1", "Favorites");
        col.setDeckIds(new java.util.ArrayList<>(List.of("deck-2", "deck-1")));
        when(collectionService.reorderDecks(eq("col-1"), eq(caller), eq(List.of("deck-2", "deck-1"))))
                .thenReturn(col);

        mockMvc.perform(patch("/api/collections/col-1/decks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckIds\":[\"deck-2\",\"deck-1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckIds[0]").value("deck-2"))
                .andExpect(jsonPath("$.deckIds[1]").value("deck-1"));
    }

    private static DeckCollection collection(String id, String name) {
        DeckCollection col = new DeckCollection();
        col.setId(id);
        col.setOwnerUserId("user-1");
        col.setName(name);
        col.setVisibility(DeckVisibility.PRIVATE);
        return col;
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.CsrfRequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
    }
}
