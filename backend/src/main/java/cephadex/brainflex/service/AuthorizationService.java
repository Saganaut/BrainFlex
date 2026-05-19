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
import java.util.Optional;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckCollaborator;
import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.Theme;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
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
    private final DeckCollaboratorRepository deckCollaboratorRepository;

    public AuthorizationService(
            DeckRepository deckRepository,
            ThemeRepository themeRepository,
            ShowcaseRepository showcaseRepository,
            OrganizationRepository organizationRepository,
            GalleryImageRepository galleryImageRepository,
            DeckCollaboratorRepository deckCollaboratorRepository) {
        this.deckRepository = deckRepository;
        this.themeRepository = themeRepository;
        this.showcaseRepository = showcaseRepository;
        this.organizationRepository = organizationRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.deckCollaboratorRepository = deckCollaboratorRepository;
    }

    /**
     * Editable = caller is OWNER or EDITOR on this deck. System decks are still
     * locked even for their original creator. Falls back to {@code creatorUserId}
     * for decks that predate the collaborator backfill (no rows yet) so the
     * migration can run idempotently without breaking edits in the meantime.
     */
    public Deck requireDeckEditable(String deckId, User caller) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        if (deck.isSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System decks are not editable");
        }
        if (canEdit(deck, caller)) return deck;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have edit access to this deck");
    }

    /** Caller is OWNER on this deck — used by collaborator management endpoints. */
    public Deck requireDeckOwner(String deckId, User caller) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        if (deck.isSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System decks have no owner");
        }
        if (isOwner(deck, caller)) return deck;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the deck owner can do that");
    }

    /**
     * True iff the caller can view the deck. PUBLIC/UNLISTED decks are always
     * viewable; ORG decks require shared organization membership; PRIVATE decks
     * require a collaborator row OR the legacy creator pointer.
     */
    public boolean canViewDeck(Deck deck, User caller) {
        if (deck == null) return false;
        var visibility = deck.getVisibility();
        if (visibility == cephadex.brainflex.model.enums.DeckVisibility.PUBLIC
                || visibility == cephadex.brainflex.model.enums.DeckVisibility.UNLISTED) {
            return true;
        }
        if (caller == null) return false;
        if (visibility == cephadex.brainflex.model.enums.DeckVisibility.ORG) {
            String orgId = deck.getOrganizationId();
            var memberships = caller.getOrganizationIds();
            if (orgId != null && memberships != null && memberships.contains(orgId)) return true;
        }
        return hasAnyRole(deck, caller)
                || (caller.getId() != null && caller.getId().equals(deck.getCreatorUserId()));
    }

    /** True iff the caller can edit the deck (OWNER or EDITOR; non-system). */
    public boolean canEditDeck(Deck deck, User caller) {
        return deck != null && !deck.isSystem() && canEdit(deck, caller);
    }

    public boolean canEditDeck(String deckId, User caller) {
        Optional<Deck> deck = deckRepository.findById(deckId);
        return deck.isPresent() && canEditDeck(deck.get(), caller);
    }

    private boolean canEdit(Deck deck, User caller) {
        if (caller == null || caller.getId() == null) return false;
        Optional<DeckCollaborator> row = deckCollaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), caller.getId());
        if (row.isPresent()) {
            CollaboratorRole role = row.get().getRole();
            return role == CollaboratorRole.OWNER || role == CollaboratorRole.EDITOR;
        }
        // Fallback for decks that predate the backfill: no rows yet, so allow
        // the legacy creator. Once the migration runs every deck will have an
        // OWNER row and this branch is a no-op.
        if (deckCollaboratorRepository.findByDeckId(deck.getId()).isEmpty()) {
            return caller.getId().equals(deck.getCreatorUserId());
        }
        return false;
    }

    private boolean isOwner(Deck deck, User caller) {
        if (caller == null || caller.getId() == null) return false;
        Optional<DeckCollaborator> row = deckCollaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), caller.getId());
        if (row.isPresent()) return row.get().getRole() == CollaboratorRole.OWNER;
        if (deckCollaboratorRepository.findByDeckId(deck.getId()).isEmpty()) {
            return caller.getId().equals(deck.getCreatorUserId());
        }
        return false;
    }

    private boolean hasAnyRole(Deck deck, User caller) {
        if (caller == null || caller.getId() == null) return false;
        return deckCollaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), caller.getId())
                .isPresent();
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
