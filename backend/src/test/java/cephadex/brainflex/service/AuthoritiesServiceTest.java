/**
 * Unit tests for AuthoritiesService.
 *
 * Verifies that the derived authority set covers every (tier × status) and
 * (organizationId × ownership) combination. The OrganizationRepository is
 * mocked so no real MongoDB is required.
 */
package cephadex.brainflex.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import cephadex.brainflex.model.Membership;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;
import cephadex.brainflex.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class AuthoritiesServiceTest {

    @Mock private OrganizationRepository organizationRepository;

    @InjectMocks private AuthoritiesService authoritiesService;

    private static User registered(MembershipTier tier, MembershipStatus status) {
        User user = new User();
        user.setId("user-1");
        user.setIsGuest(false);
        Membership membership = new Membership();
        membership.setTier(tier);
        membership.setStatus(status);
        user.setMembership(membership);
        return user;
    }

    private static Set<String> roles(Set<GrantedAuthority> auths) {
        return auths.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Test
    void guestUser_GetsOnlyGuestRole() {
        User guest = new User();
        guest.setId("guest-1");
        guest.setIsGuest(true);

        Set<String> result = roles(authoritiesService.authoritiesFor(guest));

        assertEquals(Set.of(AuthoritiesService.ROLE_GUEST), result);
    }

    @Test
    void freeUser_GetsUserAndFreeRoles() {
        User user = registered(MembershipTier.FREE, MembershipStatus.NONE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertEquals(Set.of(AuthoritiesService.ROLE_USER, AuthoritiesService.ROLE_USER_FREE), result);
    }

    @Test
    void nullMembership_DefaultsToFree() {
        User user = new User();
        user.setId("user-1");
        user.setIsGuest(false);
        user.setMembership(null);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertEquals(Set.of(AuthoritiesService.ROLE_USER, AuthoritiesService.ROLE_USER_FREE), result);
    }

    @Test
    void activeIndividual_GetsBasicRole() {
        User user = registered(MembershipTier.INDIVIDUAL, MembershipStatus.ACTIVE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_BASIC));
        assertFalse(result.contains(AuthoritiesService.ROLE_USER_FREE));
    }

    @Test
    void trialingIndividual_GetsBasicRole() {
        User user = registered(MembershipTier.INDIVIDUAL, MembershipStatus.TRIALING);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_BASIC));
    }

    @Test
    void activeOrgSeat_GetsBasicRole() {
        User user = registered(MembershipTier.ORG_SEAT, MembershipStatus.ACTIVE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_BASIC));
    }

    @Test
    void activeOrgTeam_GetsPremiumRole() {
        User user = registered(MembershipTier.ORG_TEAM, MembershipStatus.ACTIVE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_PREMIUM));
        assertFalse(result.contains(AuthoritiesService.ROLE_USER_BASIC));
    }

    @Test
    void activeOrgBusiness_GetsPremiumRole() {
        User user = registered(MembershipTier.ORG_BUSINESS, MembershipStatus.ACTIVE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_PREMIUM));
    }

    @Test
    void pastDuePaidUser_FallsBackToFree() {
        User user = registered(MembershipTier.INDIVIDUAL, MembershipStatus.PAST_DUE);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_FREE));
        assertFalse(result.contains(AuthoritiesService.ROLE_USER_BASIC));
    }

    @Test
    void canceledPaidUser_FallsBackToFree() {
        User user = registered(MembershipTier.INDIVIDUAL, MembershipStatus.CANCELED);

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_USER_FREE));
        assertFalse(result.contains(AuthoritiesService.ROLE_USER_BASIC));
    }

    @Test
    void orgMemberNonOwner_GetsMemberRoleOnly() {
        User user = registered(MembershipTier.FREE, MembershipStatus.NONE);
        user.setOrganizationId("org-99");

        Organization org = new Organization();
        org.setId("org-99");
        org.setOwnerId("someone-else");
        lenient().when(organizationRepository.findById("org-99")).thenReturn(Optional.of(org));

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_ORG_MEMBER));
        assertFalse(result.contains(AuthoritiesService.ROLE_ORG_OWNER));
    }

    @Test
    void orgOwner_GetsBothMemberAndOwnerRoles() {
        User user = registered(MembershipTier.FREE, MembershipStatus.NONE);
        user.setOrganizationId("org-99");

        Organization org = new Organization();
        org.setId("org-99");
        org.setOwnerId(user.getId());
        lenient().when(organizationRepository.findById("org-99")).thenReturn(Optional.of(org));

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_ORG_MEMBER));
        assertTrue(result.contains(AuthoritiesService.ROLE_ORG_OWNER));
    }

    @Test
    void orgIdSetButOrgMissing_StillGrantsMemberRole() {
        User user = registered(MembershipTier.FREE, MembershipStatus.NONE);
        user.setOrganizationId("org-ghost");
        lenient().when(organizationRepository.findById("org-ghost")).thenReturn(Optional.empty());

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertTrue(result.contains(AuthoritiesService.ROLE_ORG_MEMBER));
        assertFalse(result.contains(AuthoritiesService.ROLE_ORG_OWNER));
    }

    @Test
    void blankOrgId_DoesNotAddOrgRoles() {
        User user = registered(MembershipTier.FREE, MembershipStatus.NONE);
        user.setOrganizationId("   ");

        Set<String> result = roles(authoritiesService.authoritiesFor(user));

        assertFalse(result.contains(AuthoritiesService.ROLE_ORG_MEMBER));
        assertFalse(result.contains(AuthoritiesService.ROLE_ORG_OWNER));
    }
}
