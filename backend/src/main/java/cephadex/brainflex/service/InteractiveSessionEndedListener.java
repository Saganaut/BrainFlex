/**
 * Bridges {@link InteractiveSessionEndedEvent} → {@link
 * ScheduledInteractiveSessionService#markComplete}. Kept as its own bean so
 * the listener can be excluded / mocked independently of the schedule
 * service in tests that don't care about the event lifecycle.
 */
package cephadex.brainflex.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InteractiveSessionEndedListener {

    private final ScheduledInteractiveSessionService scheduledInteractiveSessionService;

    public InteractiveSessionEndedListener(ScheduledInteractiveSessionService scheduledInteractiveSessionService) {
        this.scheduledInteractiveSessionService = scheduledInteractiveSessionService;
    }

    @EventListener
    public void onInteractiveSessionEnded(InteractiveSessionEndedEvent event) {
        scheduledInteractiveSessionService.markComplete(event.interactiveSessionId());
    }
}
