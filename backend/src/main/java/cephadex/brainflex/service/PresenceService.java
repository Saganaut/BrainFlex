/**
 * In-memory presence tracker for active WebSocket users.
 *
 * Spring's STOMP session events fire per WebSocket session, so a single user with
 * multiple tabs counts multiple times — we maintain a sessionCount per userId and
 * only broadcast online/offline on 0→1 and 1→0 transitions.
 *
 * Broadcasts are sent on the global /topic/presence so any showcase view can filter
 * by its own player list. Per-showcase scoping can be added later if this becomes
 * noisy at scale; for the current small footprint, a single topic is simpler.
 */
package cephadex.brainflex.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import cephadex.brainflex.dto.PresenceMessage;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final ConcurrentHashMap<String, Integer> sessionCount = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ShowcaseService showcaseService;

    public PresenceService(SimpMessagingTemplate messagingTemplate,
                           @Lazy ShowcaseService showcaseService) {
        this.messagingTemplate = messagingTemplate;
        this.showcaseService = showcaseService;
    }

    /**
     * Register a new WebSocket session for this user; broadcasts on 0→1 transition
     * and tells the showcase layer to flip the player's disconnected flag back
     * to false on reconnect.
     */
    public void onConnect(String userId) {
        if (userId == null)
            return;
        Integer next = sessionCount.merge(userId, 1, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b));
        if (next == 1) {
            log.debug("Presence: {} online", userId);
            broadcast(userId, true);
            // Chunk 13 — keep ShowcasePlayer.disconnected / lastSeenAt in sync.
            showcaseService.markPlayerPresence(userId, true);
        }
    }

    /**
     * Drop a WebSocket session for this user; broadcasts on last-session → 0
     * transition and tells the showcase layer to flip the player's
     * disconnected flag to true.
     */
    public void onDisconnect(String userId) {
        if (userId == null)
            return;
        Integer next = sessionCount.computeIfPresent(userId, (k, v) -> v - 1);
        if (next != null && next <= 0) {
            sessionCount.remove(userId);
            log.debug("Presence: {} offline", userId);
            broadcast(userId, false);
            // Chunk 13 — keep ShowcasePlayer.disconnected / lastSeenAt in sync.
            showcaseService.markPlayerPresence(userId, false);
        }
    }

    /**
     * Read-only snapshot of currently online userIds — useful for REST endpoints
     * later.
     */
    public Set<String> onlineUserIds() {
        return Set.copyOf(sessionCount.keySet());
    }

    private void broadcast(String userId, boolean online) {
        messagingTemplate.convertAndSend("/topic/presence", new PresenceMessage(userId, online));
    }
}
