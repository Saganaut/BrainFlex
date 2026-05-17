/**
 * Centralizes per-resource ownership checks.
 *
 * Every mutating endpoint that operates on a stored resource (deck, theme,
 * showcase, organization) must call the matching `require*` method here to
 * verify the caller may act on it. The helper re-fetches the resource by id
 * and compares the stored owner field to {@code caller.id} — no
 * client-supplied ownership claim is trusted.
 *
 * Throws {@link ResponseStatusException}:
 *   - NOT_FOUND when the resource does not exist
 *   - FORBIDDEN when the caller is not the owner / host
 *
 * Previously these checks were scattered: {@code DeckService.requireOwned} (a
 * private helper), {@code ThemeController.resolveOwnedTheme} (an inline
 * Optional chain), {@code ShowcaseService.cancelShowcase} (an inline
 * comparison). Consolidating them here keeps the rule in one place when
 * org-shared editing or tier-aware co-edit rights are added later.
 */
package cephadex.brainflex.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.ThemeRepository;

@Service
public class AuthorizationService {

    private final DeckRepository deckRepository;
    private final ThemeRepository themeRepository;
    private final ShowcaseRepository showcaseRepository;
    private final OrganizationRepository organizationRepository;
    private final GalleryImageRepository galleryImageRepository;

    public AuthorizationService(
            DeckRepository deckRepository,
            ThemeRepository themeRepository,
            ShowcaseRepository showcaseRepository,
            OrganizationRepository organizationRepository,
            GalleryImageRepository galleryImageRepository) {
        this.deckRepository = deckRepository;
        this.themeRepository = themeRepository;
        this.showcaseRepository = showcaseRepository;
        this.organizationRepository = organizationRepository;
        this.galleryImageRepository = galleryImageRepository;
    }

    public Deck requireDeckEditable(String deckId, User caller) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        if (deck.isSystem() || !caller.getId().equals(deck.getCreatorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this deck");
        }
        return deck;
    }

    public Theme requireThemeEditable(String themeId, User caller) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme not found"));
        if (!caller.getId().equals(theme.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this theme");
        }
        return theme;
    }

    public Showcase requireShowcaseHost(String roomCode, User caller) {
        Showcase showcase = showcaseRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showcase not found"));
        if (!caller.getId().equals(showcase.getHostUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action");
        }
        return showcase;
    }

    public Organization requireOrgOwner(String orgId, User caller) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        if (!caller.getId().equals(org.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this organization");
        }
        return org;
    }

    public GalleryImage requireGalleryImageEditable(String imageId, User caller) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gallery image not found"));
        if (!caller.getId().equals(image.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this gallery image");
        }
        return image;
    }

    public GalleryImage requireGalleryImageVisible(String imageId, User caller) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gallery image not found"));
        if (caller.getId().equals(image.getOwnerId())) return image;
        String orgId = image.getOrganizationId();
        if (orgId != null && !orgId.isBlank()) {
            List<String> memberships = caller.getOrganizationIds();
            if (memberships != null && memberships.contains(orgId)) return image;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this gallery image");
    }
}
