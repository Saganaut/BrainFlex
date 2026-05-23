package cephadex.brainflex.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.org.CreateOrganizationRequest;
import cephadex.brainflex.dto.session.JoinByCodeRequest;
import cephadex.brainflex.dto.org.JoinOrganizationRequest;
import cephadex.brainflex.dto.org.OrganizationResponse;
import cephadex.brainflex.dto.org.UpdateOrganizationRequest;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.OrganizationService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationRepository organizationRepository,
            UserRepository userRepository, UserService userService,
            OrganizationService organizationService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    /** Returns every organization the caller belongs to (possibly empty). */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrganizationResponse>> listMyOrgs(Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        List<String> ids = user.getOrganizationIds();
        if (ids == null || ids.isEmpty())
            return ResponseEntity.ok(List.of());

        List<OrganizationResponse> response = organizationRepository.findAllById(ids).stream()
                .map(OrganizationResponse::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new organization and adds the caller as its owner. Users may
     * belong to multiple orgs simultaneously, so no "leave first" check.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> createOrg(
            @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }

        User user = userOpt.get();
        Organization org = new Organization();
        org.setName(request.name().strip());
        org.setOwnerId(user.getId());
        org.setMemberCount(1);
        Organization saved = organizationRepository.save(org);

        addMembership(user, saved.getId());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrganizationResponse(saved));
    }

    /**
     * Owner-only partial update of profile fields. Chunk 20 endpoint that
     * powers the org settings page; the service layer enforces ownership and
     * normalises {@code emailDomain} to lowercase so the OAuth auto-join hook
     * can look it up without normalising at read time.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> updateOrg(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
        Organization updated = organizationService.update(id, caller, request);
        return ResponseEntity.ok(new OrganizationResponse(updated));
    }

    /**
     * Regenerates the {@code inviteCode} for an owner-checked org. The old
     * code stops working immediately — there's no "grace period" pool;
     * rotation is the kill switch for a leaked code.
     */
    @PostMapping("/{id}/invite-code/rotate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> rotateInviteCode(
            @PathVariable String id,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
        Organization rotated = organizationService.rotateInviteCode(id, caller);
        return ResponseEntity.ok(new OrganizationResponse(rotated));
    }

    /**
     * Adds the caller to an existing organization by id. Idempotent if the
     * caller is already a member.
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> joinOrg(
            @RequestBody JoinOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (request.organizationId() == null || request.organizationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID is required");
        }

        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found"));

        User user = userOpt.get();
        if (addMembership(user, org.getId())) {
            org.setMemberCount(org.getMemberCount() + 1);
            userRepository.save(user);
            organizationRepository.save(org);
        }

        return ResponseEntity.ok(new OrganizationResponse(org));
    }

    /**
     * Joins an org by its shareable {@code inviteCode}. The org must have
     * {@code allowPublicJoin=true}; otherwise the code is treated as
     * invite-only and the endpoint returns 403.
     */
    @PostMapping("/join-by-code")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> joinByCode(
            @RequestBody JoinByCodeRequest request,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered account required"));
        Organization joined = organizationService.joinByCode(
                request == null ? null : request.inviteCode(),
                caller);
        return ResponseEntity.ok(new OrganizationResponse(joined));
    }

    /** Removes the caller from a specific organization they belong to. */
    @DeleteMapping("/{id}/leave")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> leaveOrg(@PathVariable String id, Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        List<String> ids = user.getOrganizationIds();
        if (ids == null || !ids.contains(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are not a member of this organization");
        }

        ids.remove(id);
        userRepository.save(user);
        organizationRepository.findById(id).ifPresent(org -> {
            org.setMemberCount(Math.max(0, org.getMemberCount() - 1));
            organizationRepository.save(org);
        });
        return ResponseEntity.ok().build();
    }

    /**
     * Inserts {@code orgId} into the user's memberships if not already present.
     * Mutates the user in place; the caller is responsible for persisting.
     * Returns true when a change was made.
     */
    private static boolean addMembership(User user, String orgId) {
        List<String> ids = user.getOrganizationIds();
        if (ids == null) {
            ids = new ArrayList<>();
            user.setOrganizationIds(ids);
        }
        if (ids.contains(orgId))
            return false;
        ids.add(orgId);
        return true;
    }
}
