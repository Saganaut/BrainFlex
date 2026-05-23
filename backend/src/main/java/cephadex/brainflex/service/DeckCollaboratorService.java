/**
 * Business logic for the deck-collaborators join collection.
 *
 * Every deck has exactly one OWNER row; EDITOR/VIEWER rows attach co-editors and
 * read-only collaborators. The legacy {@code Deck.creatorUserId} field is the
 * first-author pointer and is never rewritten here — ownership transfers move
 * the OWNER row, not the creator pointer.
 *
 * Invites accept a registered userId, a userName, or an email. Unknown emails
 * become pending rows (userId=null, email=set, acceptedAt=null) that the
 * {@code AuthController} login flow promotes on the invitee's first login (see
 * {@link #claimPendingInvitesFor}).
 *
 * All resolution writes go through {@link DeckCollaboratorRepository} which has
 * a {@code (deckId, userId)} unique index; concurrent invites for the same
 * (deck, user) pair are collapsed into a single row via DuplicateKeyException.
 */
package cephadex.brainflex.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.deck.DeckCollaboratorResponse;
import cephadex.brainflex.dto.deck.InviteCollaboratorRequest;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollaborator;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class DeckCollaboratorService {

    private final DeckCollaboratorRepository collaboratorRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final UserImageHydrator userImageHydrator;
    private final ApplicationEventPublisher events;

    public DeckCollaboratorService(
            DeckCollaboratorRepository collaboratorRepository,
            DeckRepository deckRepository,
            UserRepository userRepository,
            UserImageHydrator userImageHydrator,
            ApplicationEventPublisher events) {
        this.collaboratorRepository = collaboratorRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.userImageHydrator = userImageHydrator;
        this.events = events;
    }

    // ---- Read ----

    public List<DeckCollaborator> listForDeck(String deckId) {
        return collaboratorRepository.findByDeckId(deckId);
    }

    public List<DeckCollaboratorResponse> listForDeckHydrated(String deckId) {
        List<DeckCollaborator> rows = collaboratorRepository.findByDeckId(deckId);
        return hydrate(rows);
    }

    public Optional<DeckCollaborator> findOwner(String deckId) {
        return collaboratorRepository.findByDeckIdAndRole(deckId, CollaboratorRole.OWNER);
    }

    public Optional<DeckCollaborator> findRow(String deckId, String userId) {
        if (userId == null)
            return Optional.empty();
        return collaboratorRepository.findByDeckIdAndUserId(deckId, userId);
    }

    /** Decks the user can edit (OWNER or EDITOR). */
    public List<DeckCollaborator> findEditableByUser(String userId) {
        return collaboratorRepository.findByUserIdAndRoleIn(
                userId, List.of(CollaboratorRole.OWNER, CollaboratorRole.EDITOR));
    }

    /** All rows for the user — owned, edited, and viewer-shared. */
    public List<DeckCollaborator> findAllByUser(String userId) {
        return collaboratorRepository.findByUserId(userId);
    }

    /** Just the deck ids the user can edit (OWNER or EDITOR). */
    public List<String> editableDeckIdsFor(String userId) {
        List<DeckCollaborator> rows = findEditableByUser(userId);
        List<String> ids = new ArrayList<>(rows.size());
        for (DeckCollaborator row : rows)
            ids.add(row.getDeckId());
        return ids;
    }

    // ---- Write ----

    /**
     * Insert the initial OWNER row for a freshly-created deck. Called from
     * DeckService.createDeck. Idempotent: if a row already exists (e.g. the
     * migration backfilled it), we leave it alone.
     */
    public DeckCollaborator addInitialOwner(Deck deck, User owner) {
        if (deck == null || owner == null)
            return null;
        Optional<DeckCollaborator> existing = collaboratorRepository
                .findByDeckIdAndUserId(deck.getId(), owner.getId());
        if (existing.isPresent())
            return existing.get();
        DeckCollaborator row = new DeckCollaborator();
        row.setId(UUID.randomUUID().toString());
        row.setDeckId(deck.getId());
        row.setUserId(owner.getId());
        row.setRole(CollaboratorRole.OWNER);
        row.setInvitedByUserId(owner.getId());
        Instant now = deck.getCreatedAt() == null ? Instant.now() : deck.getCreatedAt();
        row.setInvitedAt(now);
        row.setAcceptedAt(now);
        try {
            return collaboratorRepository.insert(row);
        } catch (DuplicateKeyException dup) {
            return collaboratorRepository
                    .findByDeckIdAndUserId(deck.getId(), owner.getId())
                    .orElseThrow();
        }
    }

    /**
     * Invite a user (or email) to collaborate on a deck. EDITOR / VIEWER only —
     * promoting someone to OWNER goes through {@link #transferOwnership}.
     *
     * If the same user already has a row on this deck, their role is updated in
     * place (idempotent). The OWNER role is never overwritten this way.
     */
    public DeckCollaborator invite(String deckId, User inviter, InviteCollaboratorRequest request) {
        if (request.role() == CollaboratorRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Use the transfer endpoint to assign OWNER");
        }
        String raw = request.userIdOrEmail() == null ? "" : request.userIdOrEmail().trim();
        if (raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userIdOrEmail is required");
        }

        Optional<User> target = resolveUser(raw);
        if (target.isPresent()) {
            User user = target.get();
            if (inviter.getId().equals(user.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "You are already the owner");
            }
            Optional<DeckCollaborator> existing = collaboratorRepository
                    .findByDeckIdAndUserId(deckId, user.getId());
            if (existing.isPresent()) {
                DeckCollaborator row = existing.get();
                if (row.getRole() == CollaboratorRole.OWNER) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "User is already the owner");
                }
                row.setRole(request.role());
                return collaboratorRepository.save(row);
            }
            DeckCollaborator inserted = insertRow(deckId, user.getId(), null, request.role(), inviter, true);
            events.publishEvent(new NotificationEvents.DeckCollaboratorInvitedEvent(
                    deckId, user.getId(), inviter.getId()));
            return inserted;
        }

        if (!looksLikeEmail(raw)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No user matches that id or username");
        }
        Optional<DeckCollaborator> existing = collaboratorRepository
                .findByDeckIdAndEmail(deckId, raw.toLowerCase());
        if (existing.isPresent()) {
            DeckCollaborator row = existing.get();
            row.setRole(request.role());
            return collaboratorRepository.save(row);
        }
        return insertRow(deckId, null, raw.toLowerCase(), request.role(), inviter, false);
    }

    /**
     * Change an existing collaborator's role to EDITOR or VIEWER. Promoting to
     * OWNER is rejected — the transfer endpoint handles that with the demote-
     * the-previous-owner step.
     */
    public DeckCollaborator updateRole(String deckId, String userId, CollaboratorRole role) {
        if (role == CollaboratorRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Use the transfer endpoint to assign OWNER");
        }
        DeckCollaborator row = collaboratorRepository
                .findByDeckIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Collaborator not found"));
        if (row.getRole() == CollaboratorRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Transfer ownership before demoting the owner");
        }
        row.setRole(role);
        return collaboratorRepository.save(row);
    }

    /**
     * Remove a collaborator row by (deckId, userId). Refuses to remove the owner.
     */
    public void remove(String deckId, String userId) {
        DeckCollaborator row = collaboratorRepository
                .findByDeckIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Collaborator not found"));
        if (row.getRole() == CollaboratorRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot remove the owner — transfer first");
        }
        collaboratorRepository.deleteByDeckIdAndUserId(deckId, userId);
    }

    /**
     * Move the OWNER role to a new user. The previous owner is demoted to
     * EDITOR (the role they almost certainly want next). The recipient must
     * already exist as a registered user — pending email invites can't receive
     * ownership.
     */
    public void transferOwnership(String deckId, User currentOwner, String newOwnerUserId) {
        if (newOwnerUserId == null || newOwnerUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (currentOwner.getId().equals(newOwnerUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You are already the owner");
        }
        User recipient = userRepository.findById(newOwnerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient user not found"));

        DeckCollaborator ownerRow = collaboratorRepository
                .findByDeckIdAndRole(deckId, CollaboratorRole.OWNER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Deck has no owner row"));
        if (!currentOwner.getId().equals(ownerRow.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the current owner can transfer");
        }
        Optional<DeckCollaborator> recipientRowOpt = collaboratorRepository
                .findByDeckIdAndUserId(deckId, recipient.getId());

        // Demote current owner first so the unique role we're about to write
        // doesn't conflict with the existing OWNER row at any point.
        ownerRow.setRole(CollaboratorRole.EDITOR);
        collaboratorRepository.save(ownerRow);

        if (recipientRowOpt.isPresent()) {
            DeckCollaborator row = recipientRowOpt.get();
            row.setRole(CollaboratorRole.OWNER);
            row.setAcceptedAt(row.getAcceptedAt() == null ? Instant.now() : row.getAcceptedAt());
            collaboratorRepository.save(row);
        } else {
            insertRow(deckId, recipient.getId(), null, CollaboratorRole.OWNER, currentOwner, true);
        }
    }

    /**
     * Clean up the join collection when a deck is deleted. Called from
     * DeckService.deleteDeck.
     */
    public void onDeckDeleted(String deckId) {
        collaboratorRepository.deleteByDeckId(deckId);
    }

    /**
     * Promote any pending email-based invites to a registered userId when a
     * user signs in for the first time. Idempotent — safe to call on every
     * login. Returns the number of rows promoted.
     */
    public int claimPendingInvitesFor(User user) {
        if (user == null || user.getEmail() == null)
            return 0;
        List<DeckCollaborator> pending = collaboratorRepository.findByEmail(user.getEmail().toLowerCase());
        int promoted = 0;
        for (DeckCollaborator row : pending) {
            if (row.getUserId() != null)
                continue;
            // Skip if a row already exists for (deckId, userId) — let the user
            // keep whichever role is higher.
            Optional<DeckCollaborator> dup = collaboratorRepository
                    .findByDeckIdAndUserId(row.getDeckId(), user.getId());
            if (dup.isPresent()) {
                collaboratorRepository.delete(row);
                continue;
            }
            row.setUserId(user.getId());
            row.setEmail(null);
            row.setAcceptedAt(Instant.now());
            try {
                collaboratorRepository.save(row);
                promoted++;
                String deckOwnerUserId = collaboratorRepository
                        .findByDeckIdAndRole(row.getDeckId(), CollaboratorRole.OWNER)
                        .map(DeckCollaborator::getUserId)
                        .orElse(null);
                if (deckOwnerUserId != null) {
                    events.publishEvent(new NotificationEvents.DeckCollaboratorAcceptedEvent(
                            row.getDeckId(), user.getId(), deckOwnerUserId));
                }
            } catch (DuplicateKeyException dup2) {
                collaboratorRepository.delete(row);
            }
        }
        return promoted;
    }

    // ---- Hydration ----

    public List<DeckCollaboratorResponse> hydrate(Collection<DeckCollaborator> rows) {
        if (rows == null || rows.isEmpty())
            return List.of();
        List<String> userIds = new ArrayList<>();
        for (DeckCollaborator row : rows) {
            if (row.getUserId() != null)
                userIds.add(row.getUserId());
        }
        java.util.Map<String, User> users = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userRepository.findAllById(userIds))
                users.put(u.getId(), u);
        }
        List<DeckCollaboratorResponse> out = new ArrayList<>(rows.size());
        for (DeckCollaborator row : rows) {
            User user = row.getUserId() == null ? null : users.get(row.getUserId());
            String picture = user == null ? null : userImageHydrator.pictureUrlOf(user);
            out.add(DeckCollaboratorResponse.of(row, user, picture));
        }
        return out;
    }

    // ---- Helpers ----

    private DeckCollaborator insertRow(
            String deckId, String userId, String email, CollaboratorRole role,
            User inviter, boolean autoAccept) {
        DeckCollaborator row = new DeckCollaborator();
        row.setId(UUID.randomUUID().toString());
        row.setDeckId(deckId);
        row.setUserId(userId);
        row.setEmail(email);
        row.setRole(role);
        row.setInvitedByUserId(inviter == null ? null : inviter.getId());
        row.setInvitedAt(Instant.now());
        if (autoAccept) {
            row.setAcceptedAt(Instant.now());
        }
        try {
            return collaboratorRepository.insert(row);
        } catch (DuplicateKeyException dup) {
            // Lost the race with another invite for the same (deck, user) pair.
            // Read the winner back so the caller sees a consistent state.
            return collaboratorRepository.findByDeckIdAndUserId(deckId, userId)
                    .orElseThrow();
        }
    }

    private Optional<User> resolveUser(String raw) {
        Optional<User> byId = userRepository.findById(raw);
        if (byId.isPresent())
            return byId;
        Optional<User> byUserName = userRepository.findByUserName(raw);
        if (byUserName.isPresent())
            return byUserName;
        if (looksLikeEmail(raw)) {
            return userRepository.findByEmail(raw.toLowerCase());
        }
        return Optional.empty();
    }

    private static boolean looksLikeEmail(String raw) {
        return raw != null && raw.contains("@") && raw.indexOf('@') < raw.length() - 1;
    }

    /**
     * Returns true if the deck has no collaborator rows at all — used by the
     * compat path that falls back to {@code Deck.creatorUserId} when a deck
     * predates the backfill migration.
     */
    public boolean hasCollaboratorRows(String deckId) {
        return !collaboratorRepository.findByDeckId(deckId).isEmpty();
    }
}
