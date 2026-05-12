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

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.ThemeRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private ThemeRepository themeRepository;
    @Mock private ShowcaseRepository showcaseRepository;
    @Mock private OrganizationRepository organizationRepository;

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

        Deck result = authorizationService.requireDeckEditable("deck-1", owner);

        assertSame(deck, result);
    }

    @Test
    void requireDeckEditable_AsNonOwner_ThrowsForbidden() {
        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireDeckEditable("deck-1", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
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

    // ---- requireShowcaseHost ----

    @Test
    void requireShowcaseHost_AsHost_ReturnsShowcase() {
        Showcase showcase = new Showcase();
        showcase.setRoomCode("ABCD12");
        showcase.setHostUserId(owner.getId());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(showcase));

        Showcase result = authorizationService.requireShowcaseHost("ABCD12", owner);

        assertSame(showcase, result);
    }

    @Test
    void requireShowcaseHost_LowercasesRoomCode() {
        Showcase showcase = new Showcase();
        showcase.setRoomCode("ABCD12");
        showcase.setHostUserId(owner.getId());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(showcase));

        Showcase result = authorizationService.requireShowcaseHost("abcd12", owner);

        assertSame(showcase, result);
    }

    @Test
    void requireShowcaseHost_AsNonHost_ThrowsForbidden() {
        Showcase showcase = new Showcase();
        showcase.setRoomCode("ABCD12");
        showcase.setHostUserId(owner.getId());
        when(showcaseRepository.findByRoomCode("ABCD12")).thenReturn(Optional.of(showcase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireShowcaseHost("ABCD12", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireShowcaseHost_WhenMissing_ThrowsNotFound() {
        when(showcaseRepository.findByRoomCode("MISSIN")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorizationService.requireShowcaseHost("MISSIN", owner));

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
