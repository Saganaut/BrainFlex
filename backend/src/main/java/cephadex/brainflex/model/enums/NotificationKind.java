/**
 * Discriminator for {@link cephadex.brainflex.model.user.Notification}.
 *
 * The kind tells the renderer which icon, copy template, and click-target shape
 * to use, and lets the user mute notifications selectively when the
 * {@code notificationPrefs} object lands on User (chunk 20). New kinds must be
 * appended to keep the persisted enum ordinals stable (Spring Mongo stores the
 * name, but downstream consumers — like the email digest — switch on it).
 */
package cephadex.brainflex.model.enums;

public enum NotificationKind {
    /** A scheduled-session invite was sent to this user. Pairs with the email send. */
    INTERACTIVE_SESSION_INVITE,
    /** Five minutes before a scheduled session is booted by the cron sweep. */
    INTERACTIVE_SESSION_STARTING_SOON,
    /** A top-level comment was posted on a deck this user owns. */
    DECK_COMMENT,
    /** Someone replied to a comment this user wrote. */
    DECK_COMMENT_REPLY,
    /** A new star rating was left on a deck this user owns. */
    DECK_RATING,
    /** Someone favorited a deck this user owns. Throttled: max 1 per (actor, deck) per 24h. */
    DECK_FAVORITED,
    /** This user was invited as a deck collaborator (EDITOR or VIEWER). */
    COLLAB_INVITE,
    /** A collaborator accepted an invite on a deck this user owns. */
    COLLAB_ACCEPTED,
    /** A new {@link cephadex.brainflex.model.user.UserAchievement} was awarded to this user. */
    ACHIEVEMENT,
    /** Reserved — an org-membership invite was sent to this user. */
    ORG_INVITE,
    /** Reserved — someone joined an org this user owns. */
    ORG_JOINED,
    /** Reserved — future @-mention in chat or comments. */
    MENTION,
    /** Platform-wide announcement; actor is null. */
    SYSTEM
}
