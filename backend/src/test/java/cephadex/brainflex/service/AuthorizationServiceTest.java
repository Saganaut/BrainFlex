/**
 * Unit tests for AuthorizationService.
 *
 * Each require* method has three test cases: owner happy path, non-owner →
 * FORBIDDEN, missing id → NOT_FOUND. Repositories are mocked so no real
 * MongoDB is required.
 */
package cephadex.brainflex.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollaborator;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.theme.Theme;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.repository.MediaAssetRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.ThemeRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private ThemeRepository themeRepository;
    @Mock private InteractiveSessionRepository interactiveSessionRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private GalleryImageRepository galleryImageRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private DeckCollaboratorRepository deckCollaboratorRepository;

    @InjectMocks private AuthorizationService authorizationService;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("user-owner");
        other = new User();
        other.setId("user-other");
    }

    // ---- requireDeckEditable ----

    @Test
    void requireDeckEditable_AsOwner_ReturnsDeck() {
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        when(deckCollaboratorRepository.findByDeckIdAndUserId("deck-1", owner.getId()))
                .thenReturn(Optional.of(ownerRow("deck-1", owner.getId())));

        Deck result = authorizationService.requireDeckEditable("deck-1", owner);

        assertSame(deck, result);
    }

    @Test
    void requireDeckEditable_AsEditor_ReturnsDeck() {
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        when(deckCollaboratorRepository.findByDeckIdAndUserId("deck-1", other.getId()))
                .thenReturn(Optional.of(editorRow("deck-1", other.getId())));

        Deck result = authorizationService.requireDeckEditable("deck-1", other);

        assertSame(deck, result);
    }

    @Test
    void requireDeckEditable_AsViewer_ThrowsForbidden() {
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        when(deckCollaboratorRepository.findByDeckIdAndUserId("deck-1", other.getId()))
                .thenReturn(Optional.of(viewerRow("deck-1", other.getId())));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireDeckEditable("deck-1", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireDeckEditable_AsNonCollaborator_ThrowsForbidden() {
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        when(deckCollaboratorRepository.findByDeckIdAndUserId("deck-1", other.getId()))
                .thenReturn(Optional.empty());
        when(deckCollaboratorRepository.findByDeckId("deck-1"))
                .thenReturn(java.util.List.of(ownerRow("deck-1", owner.getId())));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireDeckEditable("deck-1", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireDeckEditable_LegacyCreatorWithoutCollaboratorRows_AllowsCreator() {
        Deck deck = new Deck();
        deck.setId("deck-legacy");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-legacy")).thenReturn(Optional.of(deck));
        when(deckCollaboratorRepository.findByDeckIdAndUserId("deck-legacy", owner.getId()))
                .thenReturn(Optional.empty());
        when(deckCollaboratorRepository.findByDeckId("deck-legacy"))
                .thenReturn(java.util.List.of());

        Deck result = authorizationService.requireDeckEditable("deck-legacy", owner);

        assertSame(deck, result);
    }

    @Test
    void requireDeckEditable_OnSystemDeck_ThrowsForbidden() {
        Deck deck = new Deck();
        deck.setId("deck-sys");
        deck.setCreatorUserId(null);
        deck.setSystem(true);
        when(deckRepository.findById("deck-sys")).thenReturn(Optional.of(deck));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireDeckEditable("deck-sys", owner));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireDeckEditable_WhenMissing_ThrowsNotFound() {
        when(deckRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireDeckEditable("missing", owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private DeckCollaborator ownerRow(String deckId, String userId) {
        return makeRow(deckId, userId, CollaboratorRole.OWNER);
    }

    private DeckCollaborator editorRow(String deckId, String userId) {
        return makeRow(deckId, userId, CollaboratorRole.EDITOR);
    }

    private DeckCollaborator viewerRow(String deckId, String userId) {
        return makeRow(deckId, userId, CollaboratorRole.VIEWER);
    }

    private DeckCollaborator makeRow(String deckId, String userId, CollaboratorRole role) {
        DeckCollaborator row = new DeckCollaborator();
        row.setId(deckId + ":" + userId);
        row.setDeckId(deckId);
        row.setUserId(userId);
        row.setRole(role);
        return row;
    }

    // ---- requireThemeEditable ----

    @Test
    void requireThemeEditable_AsOwner_ReturnsTheme() {
        Theme theme = new Theme();
        theme.setId("theme-1");
        theme.setOwnerId(owner.getId());
        when(themeRepository.findById("theme-1")).thenReturn(Optional.of(theme));

        Theme result = authorizationService.requireThemeEditable("theme-1", owner);

        assertSame(theme, result);
    }

    @Test
    void requireThemeEditable_AsNonOwner_ThrowsForbidden() {
        Theme theme = new Theme();
        theme.setId("theme-1");
        theme.setOwnerId(owner.getId());
        when(themeRepository.findById("theme-1")).thenReturn(Optional.of(theme));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireThemeEditable("theme-1", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireThemeEditable_WhenMissing_ThrowsNotFound() {
        when(themeRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireThemeEditable("missing", owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- requireInteractiveSessionHost ----

    @Test
    void requireInteractiveSessionHost_AsHost_ReturnsInteractiveSession() {
        InteractiveSession interactiveSession = new InteractiveSession();
        interactiveSession.setRoomCode("ABCD12");
        interactiveSession.setHostUserId(owner.getId());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(interactiveSession));

        InteractiveSession result = authorizationService.requireInteractiveSessionHost("ABCD12", owner);

        assertSame(interactiveSession, result);
    }

    @Test
    void requireInteractiveSessionHost_LowercasesRoomCode() {
        InteractiveSession interactiveSession = new InteractiveSession();
        interactiveSession.setRoomCode("ABCD12");
        interactiveSession.setHostUserId(owner.getId());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(interactiveSession));

        InteractiveSession result = authorizationService.requireInteractiveSessionHost("abcd12", owner);

        assertSame(interactiveSession, result);
    }

    @Test
    void requireInteractiveSessionHost_AsNonHost_ThrowsForbidden() {
        InteractiveSession interactiveSession = new InteractiveSession();
        interactiveSession.setRoomCode("ABCD12");
        interactiveSession.setHostUserId(owner.getId());
        when(interactiveSessionRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(interactiveSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireInteractiveSessionHost("ABCD12", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireInteractiveSessionHost_WhenMissing_ThrowsNotFound() {
        when(interactiveSessionRepository.findByRoomCode("MISSIN")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireInteractiveSessionHost("MISSIN", owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- requireOrgOwner ----

    @Test
    void requireOrgOwner_AsOwner_ReturnsOrganization() {
        Organization org = new Organization();
        org.setId("org-1");
        org.setOwnerId(owner.getId());
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));

        Organization result = authorizationService.requireOrgOwner("org-1", owner);

        assertSame(org, result);
    }

    @Test
    void requireOrgOwner_AsNonOwner_ThrowsForbidden() {
        Organization org = new Organization();
        org.setId("org-1");
        org.setOwnerId(owner.getId());
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireOrgOwner("org-1", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireOrgOwner_WhenMissing_ThrowsNotFound() {
        when(organizationRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireOrgOwner("missing", owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
