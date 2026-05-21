/**
 * Unit tests for {@link DeckCollaboratorService}.
 *
 * Repositories are mocked so this stays a fast unit test. Behaviors covered:
 *   - addInitialOwner inserts the OWNER row once, idempotent on repeat
 *   - invite by userId / userName / email resolves to the right user, or
 *     creates a pending email row when the address is unknown
 *   - invite rejects role=OWNER and self-invite
 *   - updateRole rejects promoting to OWNER and demoting the existing OWNER
 *   - remove refuses to delete the OWNER row
 *   - transferOwnership demotes the previous owner to EDITOR and promotes the
 *     recipient (creating a row when missing)
 *   - claimPendingInvitesFor resolves email-only rows to userId on first login
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.InviteCollaboratorRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.DeckCollaborator;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DeckCollaboratorServiceTest {

    @Mock private DeckCollaboratorRepository collaboratorRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserImageHydrator userImageHydrator;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private DeckCollaboratorService service;

    private User owner;
    private User invitee;
    private Deck deck;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("owner-1");
        owner.setUserName("alice");
        owner.setEmail("alice@example.com");

        invitee = new User();
        invitee.setId("user-2");
        invitee.setUserName("bob");
        invitee.setEmail("bob@example.com");

        deck = new Deck();
        deck.setId("deck-1");
        deck.setCreatorUserId(owner.getId());
    }

    // ---- addInitialOwner ----

    @Test
    void addInitialOwner_InsertsOwnerRow() {
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "owner-1"))
                .thenReturn(Optional.empty());
        when(collaboratorRepository.insert(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator row = service.addInitialOwner(deck, owner);

        assertEquals(CollaboratorRole.OWNER, row.getRole());
        assertEquals("owner-1", row.getUserId());
        assertEquals("deck-1", row.getDeckId());
    }

    @Test
    void addInitialOwner_WhenRowExists_IsIdempotent() {
        DeckCollaborator existing = row("owner-1", CollaboratorRole.OWNER);
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "owner-1"))
                .thenReturn(Optional.of(existing));

        DeckCollaborator row = service.addInitialOwner(deck, owner);

        assertEquals(existing, row);
        verify(collaboratorRepository, never()).insert(any(DeckCollaborator.class));
    }

    // ---- invite ----

    @Test
    void invite_RejectsOwnerRole() {
        InviteCollaboratorRequest request = new InviteCollaboratorRequest("bob", CollaboratorRole.OWNER);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.invite("deck-1", owner, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void invite_ByUserName_InsertsEditorRow() {
        when(userRepository.findById("bob")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.empty());
        when(collaboratorRepository.insert(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator row = service.invite("deck-1", owner,
                new InviteCollaboratorRequest("bob", CollaboratorRole.EDITOR));

        assertEquals(CollaboratorRole.EDITOR, row.getRole());
        assertEquals("user-2", row.getUserId());
        assertNull(row.getEmail());
    }

    @Test
    void invite_ByUnknownEmail_CreatesPendingRow() {
        when(userRepository.findById("ghost@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("ghost@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(collaboratorRepository.findByDeckIdAndEmail("deck-1", "ghost@example.com"))
                .thenReturn(Optional.empty());
        when(collaboratorRepository.insert(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator row = service.invite("deck-1", owner,
                new InviteCollaboratorRequest("ghost@example.com", CollaboratorRole.VIEWER));

        assertEquals(CollaboratorRole.VIEWER, row.getRole());
        assertNull(row.getUserId());
        assertEquals("ghost@example.com", row.getEmail());
        assertNull(row.getAcceptedAt());
    }

    @Test
    void invite_SelfInvite_Throws() {
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.invite("deck-1", owner,
                        new InviteCollaboratorRequest("owner-1", CollaboratorRole.EDITOR)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void invite_ExistingNonOwnerRow_UpdatesRole() {
        DeckCollaborator existing = row("user-2", CollaboratorRole.VIEWER);
        when(userRepository.findById("bob")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator row = service.invite("deck-1", owner,
                new InviteCollaboratorRequest("bob", CollaboratorRole.EDITOR));

        assertEquals(CollaboratorRole.EDITOR, row.getRole());
    }

    @Test
    void invite_ExistingOwnerRow_Throws() {
        DeckCollaborator existing = row("user-2", CollaboratorRole.OWNER);
        when(userRepository.findById("bob")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.invite("deck-1", owner,
                        new InviteCollaboratorRequest("bob", CollaboratorRole.EDITOR)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- updateRole ----

    @Test
    void updateRole_PromotionToOwner_Throws() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateRole("deck-1", "user-2", CollaboratorRole.OWNER));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateRole_DemoteExistingOwner_Throws() {
        DeckCollaborator existing = row("user-2", CollaboratorRole.OWNER);
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateRole("deck-1", "user-2", CollaboratorRole.EDITOR));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateRole_HappyPath() {
        DeckCollaborator existing = row("user-2", CollaboratorRole.VIEWER);
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator row = service.updateRole("deck-1", "user-2", CollaboratorRole.EDITOR);

        assertEquals(CollaboratorRole.EDITOR, row.getRole());
    }

    // ---- remove ----

    @Test
    void remove_OwnerRow_Throws() {
        DeckCollaborator existing = row("owner-1", CollaboratorRole.OWNER);
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "owner-1"))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.remove("deck-1", "owner-1"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void remove_NonOwnerRow_DeletesIt() {
        DeckCollaborator existing = row("user-2", CollaboratorRole.EDITOR);
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));

        service.remove("deck-1", "user-2");

        verify(collaboratorRepository).deleteByDeckIdAndUserId("deck-1", "user-2");
    }

    // ---- transferOwnership ----

    @Test
    void transferOwnership_DemotesOldOwnerAndPromotesRecipient() {
        DeckCollaborator ownerRow = row("owner-1", CollaboratorRole.OWNER);
        DeckCollaborator recipientRow = row("user-2", CollaboratorRole.EDITOR);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndRole("deck-1", CollaboratorRole.OWNER))
                .thenReturn(Optional.of(ownerRow));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(recipientRow));
        when(collaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.transferOwnership("deck-1", owner, "user-2");

        ArgumentCaptor<DeckCollaborator> saved = ArgumentCaptor.forClass(DeckCollaborator.class);
        verify(collaboratorRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        List<DeckCollaborator> all = saved.getAllValues();
        // First save: previous owner demoted to EDITOR.
        assertEquals(CollaboratorRole.EDITOR, all.get(0).getRole());
        assertEquals("owner-1", all.get(0).getUserId());
        // Second save: recipient promoted.
        assertEquals(CollaboratorRole.OWNER, all.get(1).getRole());
        assertEquals("user-2", all.get(1).getUserId());
    }

    @Test
    void transferOwnership_RecipientWithoutRow_CreatesOwnerRow() {
        DeckCollaborator ownerRow = row("owner-1", CollaboratorRole.OWNER);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndRole("deck-1", CollaboratorRole.OWNER))
                .thenReturn(Optional.of(ownerRow));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.empty());
        when(collaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(collaboratorRepository.insert(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.transferOwnership("deck-1", owner, "user-2");

        verify(collaboratorRepository).insert(any(DeckCollaborator.class));
    }

    @Test
    void transferOwnership_NonOwnerCaller_Throws() {
        DeckCollaborator ownerRow = row("owner-1", CollaboratorRole.OWNER);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByDeckIdAndRole("deck-1", CollaboratorRole.OWNER))
                .thenReturn(Optional.of(ownerRow));

        User stranger = new User();
        stranger.setId("user-99");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.transferOwnership("deck-1", stranger, "user-2"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---- claimPendingInvitesFor ----

    @Test
    void claimPendingInvitesFor_PromotesEmailOnlyRowsToUserId() {
        DeckCollaborator pending = new DeckCollaborator();
        pending.setId("row-1");
        pending.setDeckId("deck-1");
        pending.setEmail("bob@example.com");
        pending.setRole(CollaboratorRole.EDITOR);
        when(collaboratorRepository.findByEmail("bob@example.com"))
                .thenReturn(List.of(pending));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.empty());

        int promoted = service.claimPendingInvitesFor(invitee);

        assertEquals(1, promoted);
        assertEquals("user-2", pending.getUserId());
        assertNull(pending.getEmail());
        verify(collaboratorRepository).save(pending);
    }

    @Test
    void claimPendingInvitesFor_WithExistingRowForUser_DropsPending() {
        DeckCollaborator pending = new DeckCollaborator();
        pending.setId("row-1");
        pending.setDeckId("deck-1");
        pending.setEmail("bob@example.com");
        pending.setRole(CollaboratorRole.VIEWER);
        DeckCollaborator existing = row("user-2", CollaboratorRole.EDITOR);
        when(collaboratorRepository.findByEmail("bob@example.com"))
                .thenReturn(List.of(pending));
        when(collaboratorRepository.findByDeckIdAndUserId("deck-1", "user-2"))
                .thenReturn(Optional.of(existing));

        int promoted = service.claimPendingInvitesFor(invitee);

        assertEquals(0, promoted);
        verify(collaboratorRepository).delete(pending);
    }

    private DeckCollaborator row(String userId, CollaboratorRole role) {
        DeckCollaborator r = new DeckCollaborator();
        r.setId("row-" + userId);
        r.setDeckId("deck-1");
        r.setUserId(userId);
        r.setRole(role);
        return r;
    }
}
