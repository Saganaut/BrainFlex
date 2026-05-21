/**
 * Spring {@link org.springframework.context.ApplicationEvent} record types
 * that {@link NotificationEventListener} translates into per-recipient
 * {@link cephadex.brainflex.model.Notification} rows.
 *
 * The events are deliberately thin — just the ids needed to look up display
 * fields on the listener side — so a future replacement (e.g. an outbox table)
 * can take them without reshuffling every call site. Publishing is
 * fire-and-forget: emitter services don't care whether listeners run
 * synchronously or async, and a thrown listener cannot break the primary
 * write because it runs after the transaction.
 *
 * Each record sits next to {@link InteractiveSessionEndedEvent} in the
 * service package; consolidating them in one file keeps the catalog of
 * cross-service triggers easy to skim.
 */
package cephadex.brainflex.service;

public final class NotificationEvents {

    private NotificationEvents() {}

    /** Fired by {@link DeckFavoriteService} on a real new favorite row. */
    public record DeckFavoritedEvent(String deckId, String actorUserId) {}

    /**
     * Fired by {@link DeckCommentService} when a new comment is created. The
     * listener decides whether to notify the deck owner (top-level) or the
     * parent comment's author (reply) based on {@code parentCommentId}.
     */
    public record DeckCommentCreatedEvent(
            String deckId, String commentId, String parentCommentId, String actorUserId) {}

    /** Fired by {@link DeckRatingService} on a real new rating insert. */
    public record DeckRatingCreatedEvent(String deckId, int stars, String actorUserId) {}

    /**
     * Fired by {@link DeckCollaboratorService} when an invite row is created
     * for a known userId. Pending email-only invites do not fire — the user
     * doesn't exist yet to receive a notification.
     */
    public record DeckCollaboratorInvitedEvent(
            String deckId, String invitedUserId, String actorUserId) {}

    /**
     * Fired by {@link DeckCollaboratorService} when a pending invite is
     * promoted to a real userId on first login.
     */
    public record DeckCollaboratorAcceptedEvent(
            String deckId, String acceptedUserId, String deckOwnerUserId) {}

    /**
     * Fired by {@link ScheduledInteractiveSessionService} on initial invite
     * creation when the email matches a registered user. Complements the
     * outbound email with an in-app row.
     */
    public record InteractiveSessionInviteSentEvent(
            String scheduledInteractiveSessionId,
            String inviteId,
            String invitedUserId,
            String actorUserId) {}

    /**
     * Fired ~5 minutes before a scheduled session boots so the cron sweep can
     * surface a "starting soon" row to every invitee. {@code invitedUserId} is
     * the resolved user id (skip pending email-only invites).
     */
    public record ScheduledInteractiveSessionBootingEvent(
            String scheduledInteractiveSessionId, String invitedUserId) {}

    /**
     * Fired by {@link AchievementService} when a UserAchievement row is
     * inserted. Listener writes one Notification per earned achievement.
     */
    public record AchievementEarnedEvent(
            String userId, String achievementId) {}
}
