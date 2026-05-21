/**
 * Writes {@link Notification} rows and pushes them realtime over STOMP.
 *
 * The hot path is {@link #send(String, NotificationKind, String, String, String, Map, User)}:
 * insert a row, denormalise the actor display fields, then fan out to the
 * recipient over {@code /user/{principal}/queue/notifications} so a connected
 * client prepends without polling. Offline clients reconcile via the paginated
 * GET endpoint on next focus.
 *
 * DECK_FAVORITED is throttled via a Redis dedupe key
 * {@code notif:favorited:{deckOwnerId}:{actorId}:{deckId}} with a 24h TTL.
 * Other kinds are not throttled — comment / rating / collab events are
 * sufficiently rare that a per-event row is the right granularity.
 *
 * STOMP user-routing keys on the OAuth provider id, not the User.id, because
 * Spring's {@code convertAndSendToUser} matches against
 * {@code Principal.getName()}. The translation happens via
 * {@link OAuthProviderService#principalNameFor(User)}.
 *
 * The send path is fire-and-forget from the caller's perspective: every
 * exception is logged and swallowed so a downed Redis or a missing User row
 * can never break the underlying user action (a comment, a rating, etc.).
 *
 * Mute preferences (chunk 20 {@code User.notificationPrefs}) are not honored
 * here yet — when that lands, gate the STOMP push + email handoff on
 * {@code prefs.muted(kind) == false} while still writing the row.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.NotificationDTO;
import cephadex.brainflex.model.Notification;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserSnapshot;
import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.repository.NotificationRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Match the per-user destination on the STOMP client. */
    public static final String USER_DESTINATION = "/queue/notifications";

    /** Redis dedupe TTL for DECK_FAVORITED throttling — 24 hours. */
    private static final Duration FAVORITED_DEDUPE_TTL = Duration.ofHours(24);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final OAuthProviderService oAuthProviderService;
    private final UserImageHydrator userImageHydrator;
    private final StringRedisTemplate redis;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate,
            SimpMessagingTemplate messagingTemplate,
            OAuthProviderService oAuthProviderService,
            UserImageHydrator userImageHydrator,
            StringRedisTemplate redis) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.messagingTemplate = messagingTemplate;
        this.oAuthProviderService = oAuthProviderService;
        this.userImageHydrator = userImageHydrator;
        this.redis = redis;
    }

    /**
     * Insert a notification for {@code userId} and push it over STOMP.
     *
     * Returns the inserted {@link Notification} on success, or {@code null}
     * when the call was a no-op — recipient was the actor, the user does not
     * exist, the throttle key was hot, or any of the above. Never throws.
     */
    public Notification send(
            String userId,
            NotificationKind kind,
            String title,
            String body,
            String link,
            Map<String, String> meta,
            User actor) {
        try {
            if (userId == null || userId.isBlank() || kind == null) return null;
            // Never notify the actor about their own action — the action UI
            // already gave them feedback, and a self-notification reads as a
            // bug to the user.
            if (actor != null && userId.equals(actor.getId())) return null;

            // Throttle DECK_FAVORITED: one row per (deckOwner, actor, deck) per
            // 24h. We dedupe before the insert so the row count matches what
            // the badge claims.
            if (kind == NotificationKind.DECK_FAVORITED && actor != null
                    && meta != null && meta.get("deckId") != null) {
                if (!claimFavoritedDedupe(userId, actor.getId(), meta.get("deckId"))) {
                    return null;
                }
            }

            User recipient = userRepository.findById(userId).orElse(null);
            if (recipient == null) return null;
            // Guests have no inbox UI; suppress to avoid orphaned rows on
            // accounts the system never reads back.
            if (Boolean.TRUE.equals(recipient.getIsGuest())) return null;

            Notification row = new Notification();
            row.setId(UUID.randomUUID().toString());
            row.setUserId(userId);
            row.setKind(kind);
            row.setTitle(title);
            row.setBody(body);
            row.setLink(link);
            row.setMeta(meta == null ? new HashMap<>() : new HashMap<>(meta));
            if (actor != null) {
                String actorName = actor.getName() != null ? actor.getName() : actor.getUserName();
                row.setActor(UserSnapshot.of(actor.getId(), actorName, userImageHydrator.pictureUrlOf(actor)));
            }
            row.setRead(false);
            row.setCreatedAt(LocalDateTime.now());
            Notification saved = notificationRepository.insert(row);

            pushOverStomp(recipient, saved);
            return saved;
        } catch (Exception e) {
            log.warn("NotificationService.send failed for user {} kind {}: {}",
                    userId, kind, e.getMessage());
            return null;
        }
    }

    /** Convenience: no provenance map, no actor (system / scheduled). */
    public Notification sendSystem(
            String userId, NotificationKind kind, String title, String body, String link) {
        return send(userId, kind, title, body, link, null, null);
    }

    /** Paginated newest-first for the dropdown / inbox. */
    public Page<Notification> listForUser(String userId, Pageable pageable) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to view notifications");
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    /** Badge count. */
    public long unreadCount(String userId) {
        if (userId == null) return 0;
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Flip {@code read} to true for a single row. Refuses to read another
     * user's row.
     */
    public NotificationDTO markRead(String id, String userId) {
        Notification row = requireOwnedRow(id, userId);
        if (row.isRead()) return NotificationDTO.from(row);
        LocalDateTime now = LocalDateTime.now();
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(id)),
                new Update().set("read", true).set("readAt", now),
                Notification.class);
        row.setRead(true);
        row.setReadAt(now);
        return NotificationDTO.from(row);
    }

    /** Flip every unread row for the user to read. Returns the rewritten count. */
    public long markAllRead(String userId) {
        if (userId == null) return 0;
        var result = mongoTemplate.updateMulti(
                new Query(Criteria.where("userId").is(userId).and("read").is(false)),
                new Update().set("read", true).set("readAt", LocalDateTime.now()),
                Notification.class);
        return result.getModifiedCount();
    }

    /** Delete a single row. Refuses to delete another user's row. */
    public void dismiss(String id, String userId) {
        Notification row = requireOwnedRow(id, userId);
        notificationRepository.delete(row);
    }

    private Notification requireOwnedRow(String id, String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to manage notifications");
        }
        Notification row = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!userId.equals(row.getUserId())) {
            // 404 not 403: the row exists but the caller has no claim to it,
            // and acknowledging existence would leak the recipient.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return row;
    }

    private void pushOverStomp(User recipient, Notification row) {
        String principal = oAuthProviderService.principalNameFor(recipient);
        if (principal == null) return;
        try {
            messagingTemplate.convertAndSendToUser(principal, USER_DESTINATION, NotificationDTO.from(row));
        } catch (Exception e) {
            // The row is already persisted; a STOMP push failure is harmless
            // because the next polling tick / page focus will hydrate it.
            log.debug("STOMP push for notification {} skipped: {}", row.getId(), e.getMessage());
        }
    }

    /**
     * Returns {@code true} if the dedupe slot was claimed and the caller should
     * proceed with the send. Returns {@code false} if a prior send already
     * occupied the slot in the past 24 hours. Falls open on Redis errors so a
     * Redis outage degrades to "more notifications" rather than blocking them.
     */
    private boolean claimFavoritedDedupe(String deckOwnerId, String actorId, String deckId) {
        String key = "notif:favorited:" + deckOwnerId + ":" + actorId + ":" + deckId;
        try {
            Boolean set = redis.opsForValue().setIfAbsent(key, "1", FAVORITED_DEDUPE_TTL);
            return set == null || set;
        } catch (Exception e) {
            log.debug("Favorited dedupe check failed for {}: {}", key, e.getMessage());
            return true;
        }
    }

}
