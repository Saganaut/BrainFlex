/**
 * Outbound transactional email. A thin abstraction so the implementation
 * can stay SMTP today and swap to SendGrid / Postmark / SES later without
 * touching call sites. All methods are synchronous fire-and-log: the
 * scheduler doesn't block on user retries.
 */
package cephadex.brainflex.service;

import java.util.List;

import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.ScheduledInteractiveSession;

public interface EmailService {

    /**
     * Initial invite — sent when the host first schedules. Includes the
     * scheduled date/time and a calendar-attachment-style "join later" link.
     */
    void sendInitialInvite(ScheduledInteractiveSession schedule,
                           InteractiveSessionInvite invite,
                           String hostName,
                           String deckName);

    /**
     * Reminder / boot — sent when the cron sweep boots the live session.
     * Includes the room code and a one-tap join URL with the invite token.
     */
    void sendBootReminder(ScheduledInteractiveSession schedule,
                          InteractiveSessionInvite invite,
                          String hostName,
                          String deckName,
                          String roomCode);

    /**
     * Notification that the host cancelled the scheduled session. Sent once
     * per invitee. Best-effort — failures are logged but don't block cancel.
     */
    void sendCancelNotice(ScheduledInteractiveSession schedule,
                          List<InteractiveSessionInvite> invites,
                          String hostName,
                          String deckName);
}
