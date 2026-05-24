/**
 * Centralizes per-resource ownership checks.
 *
 * Every mutating endpoint that operates on a stored resource (deck, theme,
 * interactiveSession, organization) must call the matching `require*` method here to
 * verify the caller may act on it. The helper re-fetches the resource by id
 * and compares the stored owner field to {@code caller.id} — no
 * client-supplied ownership claim is trusted.
 *
 * Throws the typed {@link cephadex.brainflex.exception.ApiException} hierarchy:
 *   - {@link NotFoundException} (404) when the resource does not exist — and,
 *     for guessable-key resources (interactive sessions, addressed by room
 *     code), ALSO when the caller is not the host, so existence is masked.
 *   - {@link ForbiddenException} (403) when a high-entropy-id resource (deck,
 *     theme, org, media) exists but the caller is not the owner / editor.
 * Each throw carries a resource-specific {@code code} the frontend branches on.
 * See z-docs/features/exceptions.md (the 404-vs-403 policy).
 *
 * Previously these checks were scattered: {@code DeckService.requireOwned} (a
 * private helper), {@code ThemeController.resolveOwnedTheme} (an inline
 * Optional chain), {@code InteractiveSessionService.cancelInteractiveSession} (an inline
 * comparison). Consolidating them here keeps the rule in one place when
 * org-shared editing or tier-aware co-edit rights are added later.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cephadex.brainflex.exception.ForbiddenException;
import cephadex.brainflex.exception.NotFoundException;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollaborator;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.model.media.GalleryImage;
import cephadex.brainflex.model.media.MediaAsset;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.theme.Theme;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.MediaAssetRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.ThemeRepository;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.user.User;

@Service
public class AuthorizationService {

    private final DeckRepository deckRepository;
    private final ThemeRepository themeRepository;
    private final InteractiveSessionRepository interactiveSessionRepository;
    private final OrganizationRepository organizationRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final DeckCollaboratorRepository deckCollaboratorRepository;

    public AuthorizationService(
            DeckRepository deckRepository,
            ThemeRepository themeRepository,
            InteractiveSessionRepository interactiveSessionRepository,
            OrganizationRepository organizationRepository,
            GalleryImageRepository galleryImageRepository,
            MediaAssetRepository mediaAssetRepository,
            DeckCollaboratorRepository deckCollaboratorRepository) {
        this.deckRepository = deckRepository;
        this.themeRepository = themeRepository;
        this.interactiveSessionRepository = interactiveSessionRepository;
        this.organizationRepository = organizationRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.mediaAssetRepository = mediaAssetRepository;
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
                .orElseThrow(() -> new NotFoundException("DECK_NOT_FOUND", "Deck not found"));
        if (deck.isSystem()) {
            throw new ForbiddenException("DECK_SYSTEM_LOCKED", "System decks are not editable");
        }
        if (canEdit(deck, caller))
            return deck;
        throw new ForbiddenException("DECK_EDIT_FORBIDDEN", "You do not have edit access to this deck");
    }

    /** Caller is OWNER on this deck — used by collaborator management endpoints. */
    public Deck requireDeckOwner(String deckId, User caller) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new NotFoundException("DECK_NOT_FOUND", "Deck not found"));
        if (deck.isSystem()) {
            throw new ForbiddenException("DECK_SYSTEM_LOCKED", "System decks have no owner");
        }
        if (isOwner(deck, caller))
            return deck;
        throw new ForbiddenException("DECK_OWNER_REQUIRED", "Only the deck owner can do that");
    }

    /**
     * True iff the caller can view the deck. PUBLIC/UNLISTED decks are always
     * viewable; ORG decks require shared organization membership; PRIVATE decks
     * require a collaborator row OR the legacy creator pointer.
     */
    public boolean canViewDeck(Deck deck, User caller) {
        if (deck == null)
            return false;
        var visibility = deck.getVisibility();
        if (visibility == cephadex.brainflex.model.enums.DeckVisibility.PUBLIC
                || visibility == cephadex.brainflex.model.enums.DeckVisibility.UNLISTED) {
            return true;
        }
        if (caller == null)
            return false;
        if (visibility == cephadex.brainflex.model.enums.DeckVisibility.ORG) {
            String orgId = deck.getOrganizationId();
            var memberships = caller.getOrganizationIds();
            if (orgId != null && memberships != null && memberships.contains(orgId))
                return true;
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
        if (caller == null || caller.getId() == null)
            return false;
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
        if (caller == null || caller.getId() == null)
            return false;
        Optional<DeckCollaborator> row = deckCollaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), caller.getId());
        if (row.isPresent())
            return row.get().getRole() == CollaboratorRole.OWNER;
        if (deckCollaboratorRepository.findByDeckId(deck.getId()).isEmpty()) {
            return caller.getId().equals(deck.getCreatorUserId());
        }
        return false;
    }

    private boolean hasAnyRole(Deck deck, User caller) {
        if (caller == null || caller.getId() == null)
            return false;
        return deckCollaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), caller.getId())
                .isPresent();
    }

    public Theme requireThemeEditable(String themeId, User caller) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new NotFoundException("THEME_NOT_FOUND", "Theme not found"));
        if (!caller.getId().equals(theme.getOwnerId())) {
            throw new ForbiddenException("THEME_FORBIDDEN", "You do not own this theme");
        }
        return theme;
    }

    /**
     * Room codes are short and human-typed, so existence itself is sensitive
     * (see the 404-masking policy in z-docs/features/exceptions.md). Both "no
     * such room" and "you are not the host" therefore collapse to the same
     * masked 404 {@code SESSION_NOT_FOUND} — a non-host cannot tell a real room
     * from a fake one through this path.
     */
    public InteractiveSession requireInteractiveSessionHost(String roomCode, User caller) {
        InteractiveSession interactiveSession = interactiveSessionRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Interactive session not found"));
        if (!caller.getId().equals(interactiveSession.getHostUserId())) {
            throw new NotFoundException("SESSION_NOT_FOUND", "Interactive session not found");
        }
        return interactiveSession;
    }

    public Organization requireOrgOwner(String orgId, User caller) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("ORG_NOT_FOUND", "Organization not found"));
        if (!caller.getId().equals(org.getOwnerId())) {
            throw new ForbiddenException("ORG_OWNER_REQUIRED", "You do not own this organization");
        }
        return org;
    }

    public GalleryImage requireGalleryImageEditable(String imageId, User caller) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("GALLERY_IMAGE_NOT_FOUND", "Gallery image not found"));
        if (!caller.getId().equals(image.getOwnerId())) {
            throw new ForbiddenException("GALLERY_IMAGE_FORBIDDEN", "You do not own this gallery image");
        }
        return image;
    }

    public GalleryImage requireGalleryImageVisible(String imageId, User caller) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("GALLERY_IMAGE_NOT_FOUND", "Gallery image not found"));
        if (caller.getId().equals(image.getOwnerId()))
            return image;
        String orgId = image.getOrganizationId();
        if (orgId != null && !orgId.isBlank()) {
            List<String> memberships = caller.getOrganizationIds();
            if (memberships != null && memberships.contains(orgId))
                return image;
        }
        throw new ForbiddenException("GALLERY_IMAGE_FORBIDDEN", "You do not have access to this gallery image");
    }

    public MediaAsset requireMediaAssetEditable(String assetId, User caller) {
        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("MEDIA_ASSET_NOT_FOUND", "Media asset not found"));
        if (!caller.getId().equals(asset.getOwnerId())) {
            throw new ForbiddenException("MEDIA_ASSET_FORBIDDEN", "You do not own this media asset");
        }
        return asset;
    }

    public MediaAsset requireMediaAssetVisible(String assetId, User caller) {
        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("MEDIA_ASSET_NOT_FOUND", "Media asset not found"));
        if (caller.getId().equals(asset.getOwnerId()))
            return asset;
        String orgId = asset.getOrganizationId();
        if (orgId != null && !orgId.isBlank()) {
            List<String> memberships = caller.getOrganizationIds();
            if (memberships != null && memberships.contains(orgId))
                return asset;
        }
        throw new ForbiddenException("MEDIA_ASSET_FORBIDDEN", "You do not have access to this media asset");
    }
}
