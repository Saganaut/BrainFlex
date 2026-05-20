/**
 * Fallback EmailService used when `spring.mail.host` is not configured.
 * Logs at DEBUG so local-dev / test runs don't spam the console, and lets
 * the scheduler + REST flows run end-to-end without a real SMTP relay.
 */
package cephadex.brainflex.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.ScheduledInteractiveSession;

@Service
@ConditionalOnMissingBean(SmtpEmailService.class)
@ConditionalOnProperty(name = "spring.mail.host", havingValue = "", matchIfMissing = true)
public class NoOpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendInitialInvite(ScheduledInteractiveSession schedule,
                                  InteractiveSessionInvite invite,
                                  String hostName,
                                  String deckName) {
        log.debug("NoOp invite: {} invited {} to {} @ {}",
                hostName, invite.getEmail(), deckName, schedule.getScheduledStartAt());
    }

    @Override
    public void sendBootReminder(ScheduledInteractiveSession schedule,
                                 InteractiveSessionInvite invite,
                                 String hostName,
                                 String deckName,
                                 String roomCode) {
        log.debug("NoOp boot reminder: {} → {} (room {}) for {}",
                hostName, invite.getEmail(), roomCode, deckName);
    }

    @Override
    public void sendCancelNotice(ScheduledInteractiveSession schedule,
                                 List<InteractiveSessionInvite> invites,
                                 String hostName,
                                 String deckName) {
        log.debug("NoOp cancel notice: {} cancelled {} for {} invitees",
                hostName, deckName, invites.size());
    }
}
