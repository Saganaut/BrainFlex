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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.OrganizationDTO;
import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public OrganizationController(OrganizationRepository organizationRepository,
            UserRepository userRepository, UserService userService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /** Returns every organization the caller belongs to (possibly empty). */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrganizationDTO.OrganizationResponse>> listMyOrgs(Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        List<String> ids = user.getOrganizationIds();
        if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());

        List<OrganizationDTO.OrganizationResponse> response =
                organizationRepository.findAllById(ids).stream()
                        .map(OrganizationDTO.OrganizationResponse::new)
                        .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new organization and adds the caller as its owner. Users may
     * belong to multiple orgs simultaneously, so no "leave first" check.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> createOrg(
            @RequestBody OrganizationDTO.CreateOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }

        User user = userOpt.get();
        Organization org = new Organization();
        org.setName(request.name().strip());
        org.setOwnerId(user.getId());
        Organization saved = organizationRepository.save(org);

        addMembership(user, saved.getId());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrganizationDTO.OrganizationResponse(saved));
    }

    /**
     * Adds the caller to an existing organization by id. Idempotent if the
     * caller is already a member.
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> joinOrg(
            @RequestBody OrganizationDTO.JoinOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (request.organizationId() == null || request.organizationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID is required");
        }

        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found"));

        User user = userOpt.get();
        if (addMembership(user, org.getId())) {
            userRepository.save(user);
        }

        return ResponseEntity.ok(new OrganizationDTO.OrganizationResponse(org));
    }

    /** Removes the caller from a specific organization they belong to. */
    @DeleteMapping("/{id}/leave")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> leaveOrg(@PathVariable String id, Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        List<String> ids = user.getOrganizationIds();
        if (ids == null || !ids.contains(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are not a member of this organization");
        }

        ids.remove(id);
        userRepository.save(user);
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
        if (ids.contains(orgId)) return false;
        ids.add(orgId);
        return true;
    }
}
