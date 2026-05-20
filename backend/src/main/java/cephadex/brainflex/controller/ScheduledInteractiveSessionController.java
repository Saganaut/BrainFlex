/**
 * REST endpoints for scheduling, listing, editing, and cancelling
 * ScheduledInteractiveSession rows. The invite redeem endpoint lives on
 * {@link InviteController} to keep its URL short (`/api/invites/{token}`).
 */
package cephadex.brainflex.controller;

import java.util.List;

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

import cephadex.brainflex.dto.AddInviteRequest;
import cephadex.brainflex.dto.CreateScheduledInteractiveSessionRequest;
import cephadex.brainflex.dto.InteractiveSessionInviteDTO;
import cephadex.brainflex.dto.ScheduledInteractiveSessionDTO;
import cephadex.brainflex.dto.UpdateScheduledInteractiveSessionRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.ScheduledInteractiveSessionService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/scheduled-interactive-sessions")
public class ScheduledInteractiveSessionController {

    private final ScheduledInteractiveSessionService scheduleService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;

    public ScheduledInteractiveSessionController(ScheduledInteractiveSessionService scheduleService,
                                                 UserService userService,
                                                 UserRepository userRepository,
                                                 DeckRepository deckRepository) {
        this.scheduleService = scheduleService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/mine")
    public List<ScheduledInteractiveSessionDTO> listMyScheduledSessions(Authentication authentication) {
        User host = requireUser(authentication);
        return scheduleService.listMine(host.getId()).stream()
                .map(s -> toDto(s, host))
                .toList();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public ScheduledInteractiveSessionDTO getScheduledSession(@PathVariable String id, Authentication authentication) {
        User caller = requireUser(authentication);
        ScheduledInteractiveSession schedule = scheduleService.getById(id);
        if (!schedule.getHostUserId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the host");
        }
        return toDto(schedule, caller);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}/invites")
    public List<InteractiveSessionInviteDTO> listScheduledInvites(@PathVariable String id, Authentication authentication) {
        User caller = requireUser(authentication);
        ScheduledInteractiveSession schedule = scheduleService.getById(id);
        if (!schedule.getHostUserId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the host");
        }
        return scheduleService.listInvites(id).stream()
                .map(InteractiveSessionInviteDTO::from)
                .toList();
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ScheduledInteractiveSessionDTO> createScheduledSession(
            @Valid @RequestBody CreateScheduledInteractiveSessionRequest request,
            Authentication authentication) {
        User host = requireUser(authentication);
        ScheduledInteractiveSession schedule = scheduleService.schedule(host, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(schedule, host));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public ScheduledInteractiveSessionDTO updateScheduledSession(
            @PathVariable String id,
            @Valid @RequestBody UpdateScheduledInteractiveSessionRequest request,
            Authentication authentication) {
        User host = requireUser(authentication);
        return toDto(scheduleService.update(id, host, request), host);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/cancel")
    public ScheduledInteractiveSessionDTO cancelScheduledSession(@PathVariable String id, Authentication authentication) {
        User host = requireUser(authentication);
        return toDto(scheduleService.cancel(id, host), host);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/invite")
    public ResponseEntity<InteractiveSessionInviteDTO> addScheduledInvite(
            @PathVariable String id,
            @Valid @RequestBody AddInviteRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        InteractiveSessionInvite invite = scheduleService.addInvite(id, host, body.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(InteractiveSessionInviteDTO.from(invite));
    }

    private User requireUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));
    }

    private ScheduledInteractiveSessionDTO toDto(ScheduledInteractiveSession s, User caller) {
        String hostName;
        if (s.getHostUserId().equals(caller.getId())) {
            hostName = caller.getName() != null ? caller.getName() : caller.getUserName();
        } else {
            hostName = userRepository.findById(s.getHostUserId())
                    .map(u -> u.getName() != null ? u.getName() : u.getUserName())
                    .orElse("Host");
        }
        String deckName = deckRepository.findById(s.getDeckId()).map(Deck::getName).orElse("Deck");
        return ScheduledInteractiveSessionDTO.of(s, hostName, deckName);
    }
}
