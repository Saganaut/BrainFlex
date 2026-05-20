/**
 * Single source of truth for the GrantedAuthorities a {@link User} carries.
 *
 * Authorities are derived from persisted state (membership tier + status,
 * organizationIds, isGuest), not from anything the client supplies. Every entry
 * point that builds a SecurityContext — the OAuth2 success path and the guest
 * login endpoint — must funnel through this helper so the authority set stays
 * consistent across login mechanisms.
 *
 * Authority layout:
 *   - ROLE_GUEST       — guest user (no other roles granted)
 *   - ROLE_USER        — umbrella registered-user role (existing contract)
 *   - ROLE_USER_FREE   — registered user on the free tier or with a lapsed paid plan
 *   - ROLE_USER_BASIC  — paid individual / org seat
 *   - ROLE_USER_PREMIUM — org team or business plan
 *   - ROLE_ORG_MEMBER  — user belongs to some organization
 *   - ROLE_ORG_OWNER   — user owns the organization they belong to
 *
 * Tier and org roles compose: a paid user who owns an org gets ROLE_USER_BASIC
 * (or _PREMIUM) and ROLE_ORG_OWNER on top of ROLE_USER and ROLE_ORG_MEMBER. A
 * RoleHierarchy bean in SecurityConfig wires the implications so controllers
 * can gate on the *lowest* tier they need (e.g. hasRole("USER_BASIC") accepts
 * USER_PREMIUM too).
 *
 * Phase 2 of the auth hardening plan introduces this service. No endpoint
 * currently enforces tier or org roles — those gates land alongside the
 * Stripe integration. This service just makes the authorities available so
 * future @PreAuthorize checks have something to read.
 */
package cephadex.brainflex.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Membership;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;
import cephadex.brainflex.model.enums.UserRole;
import cephadex.brainflex.repository.OrganizationRepository;

@Service
public class AuthoritiesService {

    public static final String ROLE_GUEST = "ROLE_GUEST";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_USER_FREE = "ROLE_USER_FREE";
    public static final String ROLE_USER_BASIC = "ROLE_USER_BASIC";
    public static final String ROLE_USER_PREMIUM = "ROLE_USER_PREMIUM";
    public static final String ROLE_ORG_MEMBER = "ROLE_ORG_MEMBER";
    public static final String ROLE_ORG_OWNER = "ROLE_ORG_OWNER";
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final OrganizationRepository organizationRepository;

    public AuthoritiesService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Returns the full authority set for {@code user}. Safe to call for guests
     * and for users whose Membership is null (treated as FREE).
     */
    public Set<GrantedAuthority> authoritiesFor(User user) {
        Set<GrantedAuthority> auths = new HashSet<>();

        if (Boolean.TRUE.equals(user.getIsGuest())) {
            auths.add(new SimpleGrantedAuthority(ROLE_GUEST));
            return auths;
        }

        auths.add(new SimpleGrantedAuthority(ROLE_USER));
        auths.add(new SimpleGrantedAuthority(tierRole(user.getMembership())));

        var orgIds = user.getOrganizationIds();
        if (orgIds != null) {
            for (String orgId : orgIds) {
                if (orgId == null || orgId.isBlank()) continue;
                auths.add(new SimpleGrantedAuthority(ROLE_ORG_MEMBER));
                organizationRepository.findById(orgId).ifPresent(org -> {
                    if (user.getId() != null && user.getId().equals(org.getOwnerId())) {
                        auths.add(new SimpleGrantedAuthority(ROLE_ORG_OWNER));
                    }
                });
            }
        }

        // Persisted user grants. The role hierarchy in SecurityConfig makes
        // ADMIN imply MODERATOR, so we emit each present grant independently
        // rather than collapsing them here.
        var userRoles = user.getRoles();
        if (userRoles != null) {
            if (userRoles.contains(UserRole.MODERATOR)) {
                auths.add(new SimpleGrantedAuthority(ROLE_MODERATOR));
            }
            if (userRoles.contains(UserRole.ADMIN)) {
                auths.add(new SimpleGrantedAuthority(ROLE_ADMIN));
            }
        }

        return auths;
    }

    // Lapsed or never-paid memberships collapse to FREE so a delinquent payment
    // can't keep premium features lit. ACTIVE and TRIALING are the only
    // statuses that grant the tier's role.
    private static String tierRole(Membership membership) {
        if (membership == null) return ROLE_USER_FREE;
        MembershipTier tier = membership.getTier() != null ? membership.getTier() : MembershipTier.FREE;
        MembershipStatus status = membership.getStatus() != null ? membership.getStatus() : MembershipStatus.NONE;
        boolean paid = status == MembershipStatus.ACTIVE || status == MembershipStatus.TRIALING;
        if (!paid) return ROLE_USER_FREE;
        return switch (tier) {
            case FREE -> ROLE_USER_FREE;
            case INDIVIDUAL, ORG_SEAT -> ROLE_USER_BASIC;
            case ORG_TEAM, ORG_BUSINESS -> ROLE_USER_PREMIUM;
        };
    }
}
