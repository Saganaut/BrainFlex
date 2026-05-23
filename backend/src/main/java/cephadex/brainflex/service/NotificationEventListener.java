/**
 * Translates {@link NotificationEvents} into {@link NotificationService#send}
 * calls.
 *
 * Listens to the Spring application context, resolves the recipient(s) and
 * display copy (deck name, comment excerpt, achievement title, etc.), then
 * delegates to NotificationService — which already handles self-skip,
 * throttling, and the STOMP push.
 *
 * Listener methods are {@code @Async} so a slow send (e.g. a STOMP push
 * waiting on a broker reconnect) doesn't stretch the originating HTTP call.
 * The async pool is the default Spring {@code SimpleAsyncTaskExecutor};
 * upgrade to a bounded pool here if notification volume ever justifies it.
 *
 * Listener exceptions are swallowed inside NotificationService.send. We do
 * one more defensive try/catch per handler so a failure to look up the
 * deck/achievement document (deleted between event publish and listener
 * execution) doesn't leave a log scar.
 */
package cephadex.brainflex.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckComment;
import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.model.user.Achievement;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.AchievementRepository;
import cephadex.brainflex.repository.DeckCommentRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    /** Truncate denormalised body excerpts to keep dropdown rows compact. */
    private static final int BODY_EXCERPT_CHARS = 140;

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final DeckCommentRepository commentRepository;
    private final AchievementRepository achievementRepository;

    public NotificationEventListener(
            NotificationService notificationService,
            UserRepository userRepository,
            DeckRepository deckRepository,
            DeckCommentRepository commentRepository,
            AchievementRepository achievementRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
        this.commentRepository = commentRepository;
        this.achievementRepository = achievementRepository;
    }

    @Async
    @EventListener
    public void onDeckFavorited(NotificationEvents.DeckFavoritedEvent event) {
        try {
            Deck deck = deckRepository.findById(event.deckId()).orElse(null);
            if (deck == null || deck.getCreatorUserId() == null)
                return;
            User actor = findUser(event.actorUserId());
            Map<String, String> meta = newMeta();
            meta.put("deckId", deck.getId());
            String title = actorName(actor) + " favorited \"" + deck.getContent().getName() + "\"";
            notificationService.send(
                    deck.getCreatorUserId(),
                    NotificationKind.DECK_FAVORITED,
                    title,
                    null,
                    "/decks/" + deck.getId(),
                    meta,
                    actor);
        } catch (Exception e) {
            log.debug("DeckFavoritedEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onDeckCommentCreated(NotificationEvents.DeckCommentCreatedEvent event) {
        try {
            Deck deck = deckRepository.findById(event.deckId()).orElse(null);
            if (deck == null)
                return;
            DeckComment comment = commentRepository.findById(event.commentId()).orElse(null);
            if (comment == null)
                return;
            User actor = findUser(event.actorUserId());

            Map<String, String> meta = newMeta();
            meta.put("deckId", deck.getId());
            meta.put("commentId", comment.getId());

            // Replies notify the parent author; top-level comments notify the
            // deck owner.
            if (event.parentCommentId() != null) {
                DeckComment parent = commentRepository.findById(event.parentCommentId()).orElse(null);
                if (parent == null || parent.getAuthor() == null || parent.getAuthor().userId() == null)
                    return;
                String title = actorName(actor) + " replied to your comment on \"" + deck.getContent().getName() + "\"";
                notificationService.send(
                        parent.getAuthor().userId(),
                        NotificationKind.DECK_COMMENT_REPLY,
                        title,
                        excerpt(comment.getBody()),
                        "/decks/" + deck.getId() + "?comment=" + comment.getId(),
                        meta,
                        actor);
            } else {
                if (deck.getCreatorUserId() == null)
                    return;
                String title = actorName(actor) + " commented on \"" + deck.getContent().getName() + "\"";
                notificationService.send(
                        deck.getCreatorUserId(),
                        NotificationKind.DECK_COMMENT,
                        title,
                        excerpt(comment.getBody()),
                        "/decks/" + deck.getId() + "?comment=" + comment.getId(),
                        meta,
                        actor);
            }
        } catch (Exception e) {
            log.debug("DeckCommentCreatedEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onDeckRatingCreated(NotificationEvents.DeckRatingCreatedEvent event) {
        try {
            Deck deck = deckRepository.findById(event.deckId()).orElse(null);
            if (deck == null || deck.getCreatorUserId() == null)
                return;
            User actor = findUser(event.actorUserId());
            Map<String, String> meta = newMeta();
            meta.put("deckId", deck.getId());
            meta.put("stars", Integer.toString(event.stars()));
            String title = actorName(actor) + " rated \"" + deck.getContent().getName() + "\" "
                    + event.stars() + " star" + (event.stars() == 1 ? "" : "s");
            notificationService.send(
                    deck.getCreatorUserId(),
                    NotificationKind.DECK_RATING,
                    title,
                    null,
                    "/decks/" + deck.getId(),
                    meta,
                    actor);
        } catch (Exception e) {
            log.debug("DeckRatingCreatedEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onDeckCollaboratorInvited(NotificationEvents.DeckCollaboratorInvitedEvent event) {
        try {
            Deck deck = deckRepository.findById(event.deckId()).orElse(null);
            if (deck == null || event.invitedUserId() == null)
                return;
            User actor = findUser(event.actorUserId());
            Map<String, String> meta = newMeta();
            meta.put("deckId", deck.getId());
            String title = actorName(actor) + " invited you to collaborate on \"" + deck.getContent().getName() + "\"";
            notificationService.send(
                    event.invitedUserId(),
                    NotificationKind.COLLAB_INVITE,
                    title,
                    null,
                    "/decks/" + deck.getId() + "/view",
                    meta,
                    actor);
        } catch (Exception e) {
            log.debug("DeckCollaboratorInvitedEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onDeckCollaboratorAccepted(NotificationEvents.DeckCollaboratorAcceptedEvent event) {
        try {
            Deck deck = deckRepository.findById(event.deckId()).orElse(null);
            if (deck == null || event.deckOwnerUserId() == null)
                return;
            User actor = findUser(event.acceptedUserId());
            Map<String, String> meta = newMeta();
            meta.put("deckId", deck.getId());
            String title = actorName(actor) + " accepted your invite to \"" + deck.getContent().getName() + "\"";
            notificationService.send(
                    event.deckOwnerUserId(),
                    NotificationKind.COLLAB_ACCEPTED,
                    title,
                    null,
                    "/decks/" + deck.getId() + "/view",
                    meta,
                    actor);
        } catch (Exception e) {
            log.debug("DeckCollaboratorAcceptedEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onInteractiveSessionInviteSent(NotificationEvents.InteractiveSessionInviteSentEvent event) {
        try {
            if (event.invitedUserId() == null)
                return;
            User actor = findUser(event.actorUserId());
            Map<String, String> meta = newMeta();
            meta.put("scheduledInteractiveSessionId", event.scheduledInteractiveSessionId());
            if (event.inviteId() != null)
                meta.put("inviteId", event.inviteId());
            String title = actorName(actor) + " invited you to a live session";
            notificationService.send(
                    event.invitedUserId(),
                    NotificationKind.INTERACTIVE_SESSION_INVITE,
                    title,
                    null,
                    "/scheduled-sessions/" + event.scheduledInteractiveSessionId(),
                    meta,
                    actor);
        } catch (Exception e) {
            log.debug("InteractiveSessionInviteSentEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onScheduledInteractiveSessionBooting(
            NotificationEvents.ScheduledInteractiveSessionBootingEvent event) {
        try {
            if (event.invitedUserId() == null)
                return;
            Map<String, String> meta = newMeta();
            meta.put("scheduledInteractiveSessionId", event.scheduledInteractiveSessionId());
            notificationService.sendSystem(
                    event.invitedUserId(),
                    NotificationKind.INTERACTIVE_SESSION_STARTING_SOON,
                    "A live session you're invited to starts in 5 minutes",
                    null,
                    "/scheduled-sessions/" + event.scheduledInteractiveSessionId());
        } catch (Exception e) {
            log.debug("ScheduledInteractiveSessionBootingEvent listener failed: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onAchievementEarned(NotificationEvents.AchievementEarnedEvent event) {
        try {
            Achievement achievement = achievementRepository.findById(event.achievementId()).orElse(null);
            if (achievement == null)
                return;
            Map<String, String> meta = newMeta();
            meta.put("achievementId", achievement.getId());
            notificationService.send(
                    event.userId(),
                    NotificationKind.ACHIEVEMENT,
                    "Achievement unlocked: " + achievement.getName(),
                    achievement.getDescription(),
                    "/users/me?tab=achievements",
                    meta,
                    null);
        } catch (Exception e) {
            log.debug("AchievementEarnedEvent listener failed: {}", e.getMessage());
        }
    }

    // ---- Helpers ----

    private User findUser(String userId) {
        if (userId == null)
            return null;
        return userRepository.findById(userId).orElse(null);
    }

    private static String actorName(User actor) {
        if (actor == null)
            return "Someone";
        if (actor.getName() != null && !actor.getName().isBlank())
            return actor.getName();
        if (actor.getUserName() != null && !actor.getUserName().isBlank())
            return actor.getUserName();
        return "Someone";
    }

    private static String excerpt(String body) {
        if (body == null)
            return null;
        String trimmed = body.strip();
        if (trimmed.length() <= BODY_EXCERPT_CHARS)
            return trimmed;
        return trimmed.substring(0, BODY_EXCERPT_CHARS - 1).stripTrailing() + "…";
    }

    private static Map<String, String> newMeta() {
        return new HashMap<>();
    }
}
