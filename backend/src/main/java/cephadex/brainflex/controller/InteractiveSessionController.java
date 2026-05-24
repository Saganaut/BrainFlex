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

import cephadex.brainflex.dto.session.ChatSendRequest;
import cephadex.brainflex.dto.session.CreateInteractiveSessionRequest;
import cephadex.brainflex.dto.session.InteractiveSessionChatMessageResponse;
import cephadex.brainflex.dto.session.InteractiveSessionResponse;
import cephadex.brainflex.dto.session.InteractiveSessionResultResponse;
import cephadex.brainflex.dto.session.InteractiveSessionReviewResponse;
import cephadex.brainflex.dto.session.JoinInteractiveSessionRequest;
import cephadex.brainflex.dto.session.ReactionSendRequest;
import cephadex.brainflex.dto.session.TeamCrudRequest;
import cephadex.brainflex.dto.session.TeamMoveRequest;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.Reaction;
import cephadex.brainflex.service.InteractiveSessionService;
import cephadex.brainflex.service.MembershipService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;
import cephadex.brainflex.model.user.User;

@RestController
@RequestMapping("/api/interactive-sessions")
public class InteractiveSessionController {

    private final InteractiveSessionService gameService;
    private final UserService userService;
    private final MembershipService membershipService;

    public InteractiveSessionController(InteractiveSessionService gameService, UserService userService,
            MembershipService membershipService) {
        this.gameService = gameService;
        this.userService = userService;
        this.membershipService = membershipService;
    }

    /** Create a new game session. Registered users only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<InteractiveSessionResponse> createInteractiveSession(
            @Valid @RequestBody CreateInteractiveSessionRequest request,
            Authentication authentication) {

        User host = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be a registered user to create a game"));

        // Chunk 20 — soft monthly quota. 0 limit means unlimited (paid tiers);
        // the free tier sets a positive cap and the counter is bumped on
        // game-end by GameHistoryService.recordFinish.
        if (!membershipService.canStartInteractiveSession(host)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Monthly interactive session limit reached — upgrade for unlimited.");
        }

        InteractiveSession session = gameService.createInteractiveSession(host, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InteractiveSessionResponse.forViewer(session, host.getId()));
    }

    /** Get session info by room code. Public — used to render the lobby. */
    @GetMapping("/{roomCode}")
    public ResponseEntity<InteractiveSessionResponse> getInteractiveSession(@PathVariable String roomCode,
            Authentication authentication) {
        InteractiveSession session = gameService.getByRoomCode(roomCode);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, resolveViewerUserId(authentication)));
    }

    /**
     * Join a session by room code. Requires a registered or guest session.
     * The body is optional in individual mode; in manual team mode it must
     * carry a teamId.
     */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<InteractiveSessionResponse> joinByRoomCode(
            @PathVariable String roomCode,
            @RequestBody(required = false) JoinInteractiveSessionRequest body,
            Authentication authentication) {

        User player = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "You must be logged in or playing as a guest to join a game"));

        String teamId = body == null ? null : body.teamId();
        InteractiveSession session = gameService.joinInteractiveSession(roomCode, player, teamId);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, player.getId()));
    }

    /**
     * Resolve a session from an invite link token.
     * Returns the session info so the frontend can extract the room code
     * and redirect the user to the lobby page.
     */
    @GetMapping("/join/{inviteToken}")
    public ResponseEntity<InteractiveSessionResponse> getByInviteToken(@PathVariable String inviteToken,
            Authentication authentication) {
        InteractiveSession session = gameService.getByInviteToken(inviteToken);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, resolveViewerUserId(authentication)));
    }

    /** Cancel a session. Host only. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @DeleteMapping("/{roomCode}")
    public ResponseEntity<Void> cancelInteractiveSession(
            @PathVariable String roomCode,
            Authentication authentication) {

        User host = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));

        gameService.cancelInteractiveSession(roomCode, host);
        return ResponseEntity.noContent().build();
    }

    /** Get final results for a completed session. Public. */
    @GetMapping("/{roomCode}/results")
    public ResponseEntity<InteractiveSessionResultResponse> getResults(@PathVariable String roomCode) {
        return gameService.getResults(roomCode)
                .map(InteractiveSessionResultResponse::of)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Full post-interactiveSession review with per-round distributions. Public;
     * only valid once FINISHED.
     */
    @GetMapping("/{roomCode}/review")
    public InteractiveSessionReviewResponse getReview(@PathVariable String roomCode) {
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
    public ResponseEntity<InteractiveSessionChatMessageResponse> sendChat(
            @PathVariable String roomCode,
            @Valid @RequestBody ChatSendRequest request,
            Authentication authentication) {
        String principalName = requirePrincipalName(authentication);
        InteractiveSessionChatMessageResponse message = gameService.acceptChat(roomCode, request, principalName);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Paginated chat history. Late joiners load this on mount; the host sees
     * moderated bodies.
     */
    @GetMapping("/{roomCode}/chat")
    public ResponseEntity<List<InteractiveSessionChatMessageResponse>> listChat(
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
    public ResponseEntity<InteractiveSessionChatMessageResponse> moderateChat(
            @PathVariable String roomCode,
            @PathVariable String messageId,
            Authentication authentication) {
        String principalName = requirePrincipalName(authentication);
        return ResponseEntity.ok(gameService.moderateChatMessage(roomCode, messageId, principalName));
    }

    // ---- Teams (chunk 12) ----
    // Host-only CRUD plus a "move player" endpoint. Live updates are pushed
    // on /topic/interactive-session/{roomCode}/teams; these endpoints exist so the
    // host
    // editor can drive them directly without a STOMP send.

    /** Host creates a custom team. Only valid in LOBBY. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{roomCode}/teams")
    public ResponseEntity<InteractiveSessionResponse> createTeam(
            @PathVariable String roomCode,
            @Valid @RequestBody TeamCrudRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        InteractiveSession session = gameService.createTeam(roomCode, body.name(), body.color(), host);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InteractiveSessionResponse.forViewer(session, host.getId()));
    }

    /** Host renames or recolors a team. */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{roomCode}/teams/{teamId}")
    public ResponseEntity<InteractiveSessionResponse> updateTeam(
            @PathVariable String roomCode,
            @PathVariable String teamId,
            @Valid @RequestBody TeamCrudRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        InteractiveSession session = gameService.updateTeam(roomCode, teamId, body.name(), body.color(), host);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, host.getId()));
    }

    /**
     * Host deletes a team. Stranded players are reassigned round-robin to
     * the remaining teams.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{roomCode}/teams/{teamId}")
    public ResponseEntity<InteractiveSessionResponse> deleteTeam(
            @PathVariable String roomCode,
            @PathVariable String teamId,
            Authentication authentication) {
        User host = requireUser(authentication);
        InteractiveSession session = gameService.deleteTeam(roomCode, teamId, host);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, host.getId()));
    }

    /**
     * Host moves a player into a specific team. The path variable carries the
     * session-scoped {@code playerId} of the target — the host learned it from
     * the public InteractiveSessionResponse, which never exposes raw userIds.
     */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{roomCode}/players/{playerId}/team")
    public ResponseEntity<InteractiveSessionResponse> movePlayerToTeam(
            @PathVariable String roomCode,
            @PathVariable String playerId,
            @Valid @RequestBody TeamMoveRequest body,
            Authentication authentication) {
        User host = requireUser(authentication);
        InteractiveSession session = gameService.movePlayerToTeam(roomCode, playerId, body.teamId(), host);
        return ResponseEntity.ok(InteractiveSessionResponse.forViewer(session, host.getId()));
    }

    private User requireUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));
    }

    /**
     * Resolve the caller's userId for {@link InteractiveSessionResponse#forViewer}.
     * Returns {@code null} for anonymous requests (public lobby view) so
     * {@code viewerPlayerId} comes back as {@code null} rather than leaking
     * spurious identity.
     */
    private String resolveViewerUserId(Authentication authentication) {
        return userService.resolveAnyAuthenticatedUser(authentication)
                .map(User::getId)
                .orElse(null);
    }

    private static String requirePrincipalName(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }
}
