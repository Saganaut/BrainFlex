/**
 * Unit tests for {@link OrganizationService} covering the chunk 20 endpoints:
 * owner-checked update, invite-code rotation, join-by-code, and email-domain
 * auto-join. The repository is mocked so these tests stay fast and don't
 * need MongoDB.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.UpdateOrganizationRequest;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private OrganizationService service;

    @Test
    void update_OwnerOnly_RejectsNonOwner() {
        Organization org = orgWithOwner("org-1", "owner-1");
        User caller = user("rando-1");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update("org-1", caller, new UpdateOrganizationRequest(
                        "New name", null, null, null, null, null, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_AppliesPartialFields_AndLowercasesEmailDomain() {
        Organization org = orgWithOwner("org-1", "owner-1");
        User caller = user("owner-1");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateOrganizationRequest req = new UpdateOrganizationRequest(
                "New name", "New desc", "https://example.com", "Earth",
                "  EXAMPLE.com  ", true, "theme-7");

        Organization saved = service.update("org-1", caller, req);

        assertEquals("New name", saved.getName());
        assertEquals("New desc", saved.getDescription());
        assertEquals("https://example.com", saved.getWebsiteUrl());
        assertEquals("Earth", saved.getLocation());
        // strip + lowercase
        assertEquals("example.com", saved.getEmailDomain());
        assertTrue(saved.isAllowPublicJoin());
        assertEquals("theme-7", saved.getDefaultThemeId());
    }

    @Test
    void update_BlankStringsClearFields() {
        Organization org = orgWithOwner("org-1", "owner-1");
        org.setDescription("old");
        org.setEmailDomain("old.com");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Organization saved = service.update("org-1", user("owner-1"),
                new UpdateOrganizationRequest(
                        null, "", null, null, "  ", null, null));

        assertNull(saved.getDescription());
        assertNull(saved.getEmailDomain());
    }

    @Test
    void update_EmptyNameRejected() {
        Organization org = orgWithOwner("org-1", "owner-1");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update("org-1", user("owner-1"),
                        new UpdateOrganizationRequest(
                                "  ", null, null, null, null, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rotateInviteCode_GeneratesNonEmptyCode() {
        Organization org = orgWithOwner("org-1", "owner-1");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(org));
        when(organizationRepository.findByInviteCode(any())).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Organization rotated = service.rotateInviteCode("org-1", user("owner-1"));

        assertNotNull(rotated.getInviteCode());
        assertEquals(10, rotated.getInviteCode().length());
    }

    @Test
    void joinByCode_RejectsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.joinByCode("  ", user("u-1")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void joinByCode_NotFound() {
        when(organizationRepository.findByInviteCode("BAD")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.joinByCode("BAD", user("u-1")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void joinByCode_PrivateOrg_Returns403() {
        Organization org = orgWithOwner("org-1", "owner-1");
        org.setInviteCode("CODE1");
        org.setAllowPublicJoin(false);
        when(organizationRepository.findByInviteCode("CODE1")).thenReturn(Optional.of(org));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.joinByCode("CODE1", user("u-1")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinByCode_AddsMember_AndBumpsCount() {
        Organization org = orgWithOwner("org-1", "owner-1");
        org.setInviteCode("CODE1");
        org.setAllowPublicJoin(true);
        org.setMemberCount(2);
        User caller = user("u-1");
        when(organizationRepository.findByInviteCode("CODE1")).thenReturn(Optional.of(org));

        Organization joined = service.joinByCode("CODE1", caller);

        assertEquals(3, joined.getMemberCount());
        assertTrue(caller.getOrganizationIds().contains("org-1"));
        verify(userRepository).save(caller);
        verify(organizationRepository).save(org);
    }

    @Test
    void joinByCode_AlreadyMember_Noop() {
        Organization org = orgWithOwner("org-1", "owner-1");
        org.setInviteCode("CODE1");
        org.setAllowPublicJoin(true);
        org.setMemberCount(2);
        User caller = user("u-1");
        caller.setOrganizationIds(new ArrayList<>(List.of("org-1")));
        when(organizationRepository.findByInviteCode("CODE1")).thenReturn(Optional.of(org));

        service.joinByCode("CODE1", caller);

        // No double-add, no save
        assertEquals(1, caller.getOrganizationIds().size());
        assertEquals(2, org.getMemberCount());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void autoJoinByEmailDomain_NoEmail_ReturnsEmpty() {
        User u = user("u-1");
        u.setEmail(null);
        assertTrue(service.autoJoinByEmailDomain(u).isEmpty());
        verify(organizationRepository, never()).findByEmailDomain(any());
    }

    @Test
    void autoJoinByEmailDomain_NoMatches_ReturnsEmpty() {
        User u = user("u-1");
        u.setEmail("alice@unknown.com");
        when(organizationRepository.findByEmailDomain("unknown.com")).thenReturn(List.of());
        assertTrue(service.autoJoinByEmailDomain(u).isEmpty());
    }

    @Test
    void autoJoinByEmailDomain_MatchedOrgs_AddedIdempotently() {
        User u = user("u-1");
        u.setEmail("alice@Stanford.EDU");
        Organization org1 = orgWithOwner("org-1", "owner-1");
        org1.setEmailDomain("stanford.edu");
        Organization org2 = orgWithOwner("org-2", "owner-2");
        org2.setEmailDomain("stanford.edu");
        when(organizationRepository.findByEmailDomain("stanford.edu"))
                .thenReturn(List.of(org1, org2));

        List<Organization> joined = service.autoJoinByEmailDomain(u);

        assertEquals(2, joined.size());
        assertEquals(2, u.getOrganizationIds().size());
        verify(userRepository, times(1)).save(u);
        verify(organizationRepository, times(2)).save(any(Organization.class));
    }

    @Test
    void autoJoinByEmailDomain_PartiallyExisting_OnlyAddsMissing() {
        User u = user("u-1");
        u.setEmail("alice@stanford.edu");
        u.setOrganizationIds(new ArrayList<>(List.of("org-1")));
        Organization org1 = orgWithOwner("org-1", "owner-1");
        org1.setEmailDomain("stanford.edu");
        Organization org2 = orgWithOwner("org-2", "owner-2");
        org2.setEmailDomain("stanford.edu");
        when(organizationRepository.findByEmailDomain("stanford.edu"))
                .thenReturn(List.of(org1, org2));

        List<Organization> joined = service.autoJoinByEmailDomain(u);

        assertEquals(1, joined.size());
        assertEquals("org-2", joined.get(0).getId());
        assertTrue(u.getOrganizationIds().contains("org-2"));
        verify(userRepository, times(1)).save(u);
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void autoJoinByEmailDomain_AlreadyInAll_NoSave() {
        User u = user("u-1");
        u.setEmail("alice@stanford.edu");
        u.setOrganizationIds(new ArrayList<>(List.of("org-1")));
        Organization org1 = orgWithOwner("org-1", "owner-1");
        org1.setEmailDomain("stanford.edu");
        when(organizationRepository.findByEmailDomain("stanford.edu"))
                .thenReturn(List.of(org1));

        List<Organization> joined = service.autoJoinByEmailDomain(u);

        assertTrue(joined.isEmpty());
        verify(userRepository, never()).save(any(User.class));
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void autoJoinByEmailDomain_MalformedEmail_ReturnsEmpty() {
        User u = user("u-1");
        u.setEmail("nodomainhere");
        assertTrue(service.autoJoinByEmailDomain(u).isEmpty());

        u.setEmail("trailing@");
        assertTrue(service.autoJoinByEmailDomain(u).isEmpty());
    }

    @Test
    void addMembership_Idempotent() {
        User u = user("u-1");
        Organization org = orgWithOwner("org-1", "owner-1");
        org.setMemberCount(0);

        assertTrue(service.addMembership(u, org));
        assertFalse(service.addMembership(u, org));
        assertEquals(1, u.getOrganizationIds().size());
        assertEquals(1, org.getMemberCount());
    }

    private static Organization orgWithOwner(String id, String ownerId) {
        Organization org = new Organization();
        org.setId(id);
        org.setOwnerId(ownerId);
        return org;
    }

    private static User user(String id) {
        User u = new User();
        u.setId(id);
        u.setOrganizationIds(new ArrayList<>());
        return u;
    }
}
