package cephadex.brainflex.controller;

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

import cephadex.brainflex.dto.CreateGameRequest;
import cephadex.brainflex.dto.GameSessionDTO;
import cephadex.brainflex.model.GameResult;
import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.GameService;
import cephadex.brainflex.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    public GameController(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }

    /** Create a new game session. Registered users only. */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<GameSessionDTO> createGame(
            @Valid @RequestBody CreateGameRequest request,
            Authentication authentication) {

        User host = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be a registered user to create a game"));

        GameSession session = gameService.createSession(host, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GameSessionDTO(session));
    }

    /** Get session info by room code. Public — used to render the lobby. */
    @GetMapping("/{roomCode}")
    public ResponseEntity<GameSessionDTO> getSession(@PathVariable String roomCode) {
        return ResponseEntity.ok(new GameSessionDTO(gameService.getByRoomCode(roomCode)));
    }

    /** Join a session by room code. Requires a registered or guest session. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<GameSessionDTO> joinByRoomCode(
            @PathVariable String roomCode,
            Authentication authentication) {

        User player = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "You must be logged in or playing as a guest to join a game"));

        GameSession session = gameService.joinSession(roomCode, player);
        return ResponseEntity.ok(new GameSessionDTO(session));
    }

    /**
     * Resolve a session from an invite link token.
     * Returns the session info so the frontend can extract the room code
     * and redirect the user to the lobby page.
     */
    @GetMapping("/join/{inviteToken}")
    public ResponseEntity<GameSessionDTO> getByInviteToken(@PathVariable String inviteToken) {
        return ResponseEntity.ok(new GameSessionDTO(gameService.getByInviteToken(inviteToken)));
    }

    /** Cancel a session. Host only. */
    @PreAuthorize("hasAnyRole('GUEST', 'USER')")
    @DeleteMapping("/{roomCode}")
    public ResponseEntity<Void> cancelGame(
            @PathVariable String roomCode,
            Authentication authentication) {

        User host = userService.resolveAnyAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication required"));

        gameService.cancelSession(roomCode, host);
        return ResponseEntity.noContent().build();
    }

    /** Get final results for a completed session. Public. */
    @GetMapping("/{roomCode}/results")
    public ResponseEntity<GameResult> getResults(@PathVariable String roomCode) {
        return gameService.getResults(roomCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
