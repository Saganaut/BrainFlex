/**
 * Business logic for organization profile management.
 *
 * Three responsibilities the controller used to inline + one new for chunk 20:
 *   - Owner-only partial update (the {@code PUT /api/organizations/{id}} endpoint)
 *   - Invite-code rotation (the {@code POST /api/organizations/{id}/invite-code/rotate} endpoint)
 *   - Join-by-code lookup ({@code POST /api/organizations/join-by-code})
 *   - {@link #autoJoinByEmailDomain(User)} — idempotent membership add for every
 *     org whose {@link Organization#getEmailDomain()} matches the user's email
 *     domain. Called by the OAuth success handler and by {@code UserService.register}
 *     so a verified user lands in every org that pre-claimed their domain on the
 *     very first login.
 *
 * Membership counts are kept on the user's {@code organizationIds} list and on the
 * denormalised {@code Organization.memberCount}. Both move together here so the
 * controller never has to remember to bump one and forget the other.
 */
package cephadex.brainflex.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.OrganizationDTO;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class OrganizationService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LEN = 10;

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public OrganizationService(OrganizationRepository organizationRepository,
            UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Applies an owner-only partial update to {@code orgId}. Each non-null
     * field on {@code request} maps to a setter call; empty strings clear the
     * stored value so the settings UI can remove a previously-set field.
     * Throws 403 if the caller isn't the owner, 404 if the org doesn't exist.
     */
    public Organization update(String orgId, User caller, OrganizationDTO.UpdateOrganizationRequest request) {
        Organization org = requireOwnedBy(orgId, caller);

        if (request.name() != null) {
            String name = request.name().strip();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name cannot be empty");
            }
            org.setName(name);
        }
        if (request.description() != null) {
            org.setDescription(blankToNull(request.description()));
        }
        if (request.websiteUrl() != null) {
            org.setWebsiteUrl(blankToNull(request.websiteUrl()));
        }
        if (request.location() != null) {
            org.setLocation(blankToNull(request.location()));
        }
        if (request.emailDomain() != null) {
            String domain = request.emailDomain().strip().toLowerCase(Locale.ROOT);
            org.setEmailDomain(domain.isEmpty() ? null : domain);
        }
        if (request.allowPublicJoin() != null) {
            org.setAllowPublicJoin(request.allowPublicJoin());
        }
        if (request.defaultThemeId() != null) {
            org.setDefaultThemeId(blankToNull(request.defaultThemeId()));
        }

        return organizationRepository.save(org);
    }

    /**
     * Generates and persists a fresh invite code on the owner-checked org.
     * Collisions are vanishingly unlikely against a 30^10 alphabet, but we
     * retry up to 5 times before giving up so an unlucky run still succeeds.
     */
    public Organization rotateInviteCode(String orgId, User caller) {
        Organization org = requireOwnedBy(orgId, caller);
        for (int i = 0; i < 5; i++) {
            String candidate = randomInviteCode();
            if (organizationRepository.findByInviteCode(candidate).isEmpty()) {
                org.setInviteCode(candidate);
                return organizationRepository.save(org);
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Could not generate a unique invite code; please try again");
    }

    /**
     * Resolves an invite code to its org, adds the caller as a member, and
     * returns the org. Throws 404 when the code doesn't match any org, 403
     * when the org has {@code allowPublicJoin=false} (the code is treated as
     * invite-only in that case — the explicit-invite flow is reserved for
     * chunk 20 follow-up).
     */
    public Organization joinByCode(String inviteCode, User caller) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite code is required");
        }
        Organization org = organizationRepository.findByInviteCode(inviteCode.strip())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Invite code is invalid"));
        if (!org.isAllowPublicJoin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This organization is not open to public join requests");
        }
        if (addMembership(caller, org)) {
            userRepository.save(caller);
            organizationRepository.save(org);
        }
        return org;
    }

    /**
     * For each org whose {@link Organization#getEmailDomain()} matches the
     * domain on {@code user}'s email, idempotently add the user as a member.
     * Returns the list of orgs newly joined (possibly empty). Safe to call
     * with a user that already belongs to every match — same return path,
     * empty list.
     */
    public List<Organization> autoJoinByEmailDomain(User user) {
        if (user == null || user.getEmail() == null) return Collections.emptyList();
        int at = user.getEmail().indexOf('@');
        if (at < 0 || at == user.getEmail().length() - 1) return Collections.emptyList();
        String domain = user.getEmail().substring(at + 1).toLowerCase(Locale.ROOT);

        List<Organization> matches = organizationRepository.findByEmailDomain(domain);
        if (matches.isEmpty()) return Collections.emptyList();

        List<Organization> joined = new ArrayList<>();
        boolean userChanged = false;
        for (Organization org : matches) {
            if (addMembership(user, org)) {
                organizationRepository.save(org);
                joined.add(org);
                userChanged = true;
            }
        }
        if (userChanged) {
            userRepository.save(user);
        }
        return joined;
    }

    /**
     * Idempotently add {@code orgId} to the user's memberships and bump the
     * org's denorm {@code memberCount}. Caller is responsible for persisting.
     */
    public boolean addMembership(User user, Organization org) {
        List<String> ids = user.getOrganizationIds();
        if (ids == null) {
            ids = new ArrayList<>();
            user.setOrganizationIds(ids);
        }
        if (ids.contains(org.getId())) return false;
        ids.add(org.getId());
        org.setMemberCount(org.getMemberCount() + 1);
        return true;
    }

    private Organization requireOwnedBy(String orgId, User caller) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found"));
        if (caller == null || !caller.getId().equals(org.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the organization owner can perform this action");
        }
        return org;
    }

    private String randomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LEN);
        for (int i = 0; i < INVITE_CODE_LEN; i++) {
            sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    /** Test seam — exposes the canonical org lookup so the controller doesn't have to. */
    public Optional<Organization> findById(String orgId) {
        return organizationRepository.findById(orgId);
    }
}
