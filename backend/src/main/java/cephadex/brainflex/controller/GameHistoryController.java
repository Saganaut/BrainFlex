/**
 * Read-only access to the per-user game-history index.
 *
 * Mounts three GETs that all return the same {@link GameHistoryPage} shape:
 *   - {@code /api/users/me/history}: paginated history for the caller
 *   - {@code /api/users/{userId}/history}: public profile view (closed and
 *     guest accounts are 404'd to avoid leaking their existence)
 *   - {@code /api/decks/{deckId}/history/mine}: caller's history filtered to
 *     one deck, used by the deck-detail "your best score" widget
 *
 * Writes go through {@link cephadex.brainflex.service.InteractiveSessionService}
 * at game-end; there is no public POST/DELETE here.
 */
package cephadex.brainflex.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.GameHistoryDTO;
import cephadex.brainflex.dto.GameHistoryPage;
import cephadex.brainflex.model.GameHistoryEntry;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.GameHistoryService;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api")
public class GameHistoryController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final GameHistoryService gameHistoryService;
    private final UserService userService;
    private final UserRepository userRepository;

    public GameHistoryController(
            GameHistoryService gameHistoryService,
            UserService userService,
            UserRepository userRepository) {
        this.gameHistoryService = gameHistoryService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/users/me/history")
    public GameHistoryPage listMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Registered account required"));
        return toPage(gameHistoryService.listForUser(caller.getId(), buildPageable(page, size)));
    }

    @GetMapping("/users/{userId}/history")
    public GameHistoryPage listUserHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (Boolean.TRUE.equals(target.getIsGuest()) || Boolean.TRUE.equals(target.getIsClosed())) {
            // Don't leak the existence of ephemeral / closed accounts on the
            // public profile route — return the same 404 a non-existent id
            // would.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return toPage(gameHistoryService.listForUser(target.getId(), buildPageable(page, size)));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/decks/{deckId}/history/mine")
    public GameHistoryPage listMyHistoryForDeck(
            @PathVariable String deckId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User caller = userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Registered account required"));
        return toPage(gameHistoryService.listForUserAndDeck(
                caller.getId(), deckId, buildPageable(page, size)));
    }

    private PageRequest buildPageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size <= 0 ? DEFAULT_PAGE_SIZE : size));
        return PageRequest.of(safePage, safeSize, Sort.by("playedAt").descending());
    }

    private GameHistoryPage toPage(Page<GameHistoryEntry> page) {
        List<GameHistoryDTO> items = page.getContent().stream()
                .map(GameHistoryDTO::from)
                .toList();
        boolean hasMore = (long) (page.getNumber() + 1) * page.getSize() < page.getTotalElements();
        return new GameHistoryPage(items, page.getNumber(), page.getSize(), page.getTotalElements(), hasMore);
    }
}
