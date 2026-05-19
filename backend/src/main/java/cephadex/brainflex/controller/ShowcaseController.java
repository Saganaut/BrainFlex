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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.ChatSendRequest;
import cephadex.brainflex.dto.CreateShowcaseRequest;
import cephadex.brainflex.dto.JoinShowcaseRequest;
import cephadex.brainflex.dto.ReactionSendRequest;
import cephadex.brainflex.dto.ShowcaseChatMessageDTO;
import cephadex.brainflex.dto.ShowcaseDTO;
import cephadex.brainflex.dto.ShowcaseReviewDTO;
import cephadex.brainflex.dto.TeamCrudRequest;
import cephadex.brainflex.dto.TeamMoveRequest;
import cephadex.brainflex.model.Reaction;
import cephadex.brainflex.model.ShowcaseResult;
import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.ShowcaseService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/showcases")
public class ShowcaseController {

    private final ShowcaseService gameService;
    private final UserService userService;

    public ShowcaseController(ShowcaseService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }

    /** Create a new game session. Registered users only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ShowcaseDTO> createShowcase(
            @Valid @RequestBody CreateShowcaseRequest request,
            Authentication authentication) {

        User host = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be a registered user to create a game"));

        Showcase session = gameService.createShowcase(host, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShowcaseDTO(session));
    }

    /** Get session info by room code. Public — used to render the lobby. */
    @GetMapping("/{roomCode}")
    public ResponseEntity<ShowcaseDTO> getShowcase(@PathVariable String roomCode) {
        return ResponseEntity.ok(new ShowcaseDTO(gameService.getByRoomCode(roomCode)));
    }

    /**
     * Join a session by room code. Requires a registered or guest session.
     * The body is optional in individual mode; in manual team mode it must
     * carry a teamId.
     */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<ShowcaseDTO> joinByRoomCode(
            @PathVariable String roomCode,
            @RequestBody(required = false) JoinShowcaseRequest body,
            Authentication authentication) {

        User player = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "You must be logged in or playing as a guest to join a game"));

        String teamId = body == null ? null : body.teamId();
        String avatarKey = body == null ? null : body.avatarKey();
        String colorTag = body == null ? null : body.colorTag();
        Showcase session = gameService.joinShowcase(roomCode, player, teamId, avatarKey, colorTag);
        return ResponseEntity.ok(new ShowcaseDTO(session));
    }

    /**
     * Resolve a session from an invite link token.
     * Returns the session info so the frontend can extract the room code
     * and redirect the user to the lobby page.
     */
    @GetMapping("/join/{inviteToken}")
    public ResponseEntity<ShowcaseDTO> getByInviteToken(@PathVariable String inviteToken) {
        return ResponseEntity.ok(new ShowcaseDTO(gameService.getByInviteToken(inviteToken)));
    }

    /** Cancel a session. Host only. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @DeleteMapping("/{roomCode}")
    public ResponseEntity<Void> cancelShowcase(
            @PathVariable String roomCode,
            Authentication authentication) {

        User host = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));

        gameService.cancelShowcase(roomCode, host);
        return ResponseEntity.noContent().build();
    }

    /** Get final results for a completed session. Public. */
    @GetMapping("/{roomCode}/results")
    public ResponseEntity<ShowcaseResult> getResults(@PathVariable String roomCode) {
        return gameService.getResults(roomCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Full post-showcase review with per-round distributions. Public; only valid once FINISHED. */
    @GetMapping("/{roomCode}/review")
    public ShowcaseReviewDTO getReview(@PathVariable String roomCode) {
        return gameService.buildReview(roomCode);
    }

    // ---- Audience engagement (chunk 11) ----
    // STOMP is the primary path; these endpoints exist as fallbacks for
    // clients that haven't opened a WebSocket yet (e.g. lobby chat from a
    // mobile browser) and for replay queries that don't belong on a topic.

    /** REST fallback for sending a reaction. STOMP is the preferred path. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @PostMapping("/{roomCode}/reactions")
    public ResponseEntity<Reaction> sendReaction(
            @PathVariable String roomCode,
            @Valid @RequestBody ReactionSendRequest request,
            Authentication authentication) {
        String principalName = requirePrincipalName(authentication);
        Reaction reaction = gameService.acceptReaction(roomCode, request, principalName);
        return ResponseEntity.status(HttpStatus.CREATED).body(reaction);
    }

    /** REST fallback for sending a chat message. STOMP is the preferred path. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @PostMapping("/{roomCode}/chat")
    public ResponseEntity<ShowcaseChatMessageDTO> sendChat(
            @PathVariable String roomCode,
            @Valid @RequestBody ChatSendRequest request,
            Authentication authentication) {
        String principalName = requirePrincipalName(authentication);
        ShowcaseChatMessageDTO message = gameService.acceptChat(roomCode, request, principalName);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /** Paginated chat history. Late joiners load this on mount; the host sees moderated bodies. */
    @GetMapping("/{roomCode}/chat")
    public ResponseEntity<List<ShowcaseChatMessageDTO>> listChat(
            @PathVariable String roomCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        String principalName = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(gameService.listChatHistory(roomCode, page, size, principalName));
    }

    /** Host hides a chat message. Idempotent. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{roomCode}/chat/{messageId}/moderate")
    public ResponseEntity<ShowcaseChatMessageDTO> moderateChat(
            @PathVariable String roomCode,
            @PathVariable String messageId,
            Authentication authentication) {
        String principalName = requirePrincipalName(authentication);
        return ResponseEntity.ok(gameService.moderateChatMessage(roomCode, messageId, principalName));
    }

    // ---- Teams (chunk 12) ----
    // Host-only CRUD plus a "move player" endpoint. Live updates are pushed
    // on /topic/showcase/{roomCode}/teams; these endpoints exist so the host
    // editor can drive them directly without a STOMP send.

    /** Host creates a custom team. Only valid in LOBBY. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{roomCode}/teams")
    public ResponseEntity<ShowcaseDTO> createTeam(
            @PathVariable String roomCode,
            @Valid @RequestBody TeamCrudRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        Showcase session = gameService.createTeam(roomCode, body.name(), body.color(), host);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShowcaseDTO(session));
    }

    /** Host renames or recolors a team. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{roomCode}/teams/{teamId}")
    public ResponseEntity<ShowcaseDTO> updateTeam(
            @PathVariable String roomCode,
            @PathVariable String teamId,
            @Valid @RequestBody TeamCrudRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        Showcase session = gameService.updateTeam(roomCode, teamId, body.name(), body.color(), host);
        return ResponseEntity.ok(new ShowcaseDTO(session));
    }

    /**
     * Host deletes a team. Stranded players are reassigned round-robin to
     * the remaining teams.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{roomCode}/teams/{teamId}")
    public ResponseEntity<ShowcaseDTO> deleteTeam(
            @PathVariable String roomCode,
            @PathVariable String teamId,
            Authentication authentication) {
        User host = requireUser(authentication);
        Showcase session = gameService.deleteTeam(roomCode, teamId, host);
        return ResponseEntity.ok(new ShowcaseDTO(session));
    }

    /** Host moves a player into a specific team. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{roomCode}/players/{userId}/team")
    public ResponseEntity<ShowcaseDTO> movePlayerToTeam(
            @PathVariable String roomCode,
            @PathVariable String userId,
            @Valid @RequestBody TeamMoveRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        Showcase session = gameService.movePlayerToTeam(roomCode, userId, body.teamId(), host);
        return ResponseEntity.ok(new ShowcaseDTO(session));
    }

    private User requireUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));
    }

    private static String requirePrincipalName(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }
}
