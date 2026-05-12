package cephadex.brainflex.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** Returns the caller's current organization, or 404 if they have none. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> getMyOrg(Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        if (user.getOrganizationId() == null) return ResponseEntity.notFound().build();

        return organizationRepository.findById(user.getOrganizationId())
                .map(org -> ResponseEntity.ok(new OrganizationDTO.OrganizationResponse(org)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new organization and sets the caller as its owner and first member.
     * A user who already belongs to an org must leave it first.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> createOrg(
            @RequestBody OrganizationDTO.CreateOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        if (user.getOrganizationId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already a member of an organization. Leave it first.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }

        Organization org = new Organization();
        org.setName(request.name().strip());
        org.setOwnerId(user.getId());
        Organization saved = organizationRepository.save(org);

        user.setOrganizationId(saved.getId());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrganizationDTO.OrganizationResponse(saved));
    }

    /**
     * Joins an existing organization by its id.
     * A user who already belongs to an org must leave it first.
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> joinOrg(
            @RequestBody OrganizationDTO.JoinOrganizationRequest request,
            Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        if (user.getOrganizationId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already a member of an organization. Leave it first.");
        }

        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found"));

        user.setOrganizationId(org.getId());
        userRepository.save(user);

        return ResponseEntity.ok(new OrganizationDTO.OrganizationResponse(org));
    }

    /** Removes the caller from their current organization. */
    @DeleteMapping("/me/leave")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> leaveOrg(Authentication authentication) {
        Optional<User> userOpt = userService.resolveRegisteredUser(authentication);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User user = userOpt.get();
        if (user.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are not a member of any organization");
        }

        user.setOrganizationId(null);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
