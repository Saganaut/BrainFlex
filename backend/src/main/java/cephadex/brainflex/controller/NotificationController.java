/**
 * REST endpoints for the in-app notification stream.
 *
 * The dropdown reads {@code GET /api/notifications} for the paginated list and
 * {@code GET /api/notifications/unread-count} for the badge — the latter is
 * intentionally tiny so the 60-second polling fallback (active when the STOMP
 * connection is closed) is cheap. State changes — mark one read, mark all
 * read, dismiss — go through {@code PUT}/{@code DELETE} mutations that the
 * frontend cache enhancements optimistically update.
 *
 * Every route requires an authenticated registered user; guests get a 403.
 * Reading another user's row is masked as 404 inside the service so existence
 * of unrelated notifications cannot be probed.
 */
package cephadex.brainflex.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.NotificationResponse;
import cephadex.brainflex.dto.Page;
import cephadex.brainflex.dto.UnreadNotificationCountResponse;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.service.NotificationService;
import cephadex.brainflex.service.UserService;
import cephadex.brainflex.model.user.Notification;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public Page<NotificationResponse> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User caller = requireUser(authentication);
        org.springframework.data.domain.Page<Notification> rows = notificationService.listForUser(caller.getId(),
                buildPageable(page, size));
        List<NotificationResponse> items = rows.getContent().stream().map(NotificationResponse::from).toList();
        boolean hasMore = (long) (rows.getNumber() + 1) * rows.getSize() < rows.getTotalElements();
        return new Page<>(items, rows.getNumber(), rows.getSize(), rows.getTotalElements(), hasMore);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadNotificationCount(Authentication authentication) {
        User caller = requireUser(authentication);
        return new UnreadNotificationCountResponse(notificationService.unreadCount(caller.getId()));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/read")
    public NotificationResponse markNotificationRead(@PathVariable String id, Authentication authentication) {
        User caller = requireUser(authentication);
        return notificationService.markRead(id, caller.getId());
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/read-all")
    public UnreadNotificationCountResponse markAllNotificationsRead(Authentication authentication) {
        User caller = requireUser(authentication);
        notificationService.markAllRead(caller.getId());
        // Echo the current unread count (always 0 after read-all unless a new
        // notification raced the request) so the badge can update from the
        // mutation response without a second fetch.
        return new UnreadNotificationCountResponse(notificationService.unreadCount(caller.getId()));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public void dismissNotification(@PathVariable String id, Authentication authentication) {
        User caller = requireUser(authentication);
        notificationService.dismiss(id, caller.getId());
    }

    private User requireUser(Authentication authentication) {
        return userService.resolveRegisteredUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Registered account required"));
    }

    private PageRequest buildPageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size <= 0 ? DEFAULT_PAGE_SIZE : size));
        return PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
    }
}
