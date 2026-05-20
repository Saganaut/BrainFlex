/**
 * Bridges Spring's STOMP session lifecycle events into PresenceService.
 *
 * SessionConnectedEvent fires once the STOMP CONNECT frame is accepted and the
 * principal is bound to the WebSocket session. SessionDisconnectEvent fires on the
 * DISCONNECT frame or socket close. Principal naming mirrors InteractiveSessionService:
 * registered users use their Google ID; guests use "guest:<id>".
 */
package cephadex.brainflex.config;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.PresenceService;

@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    public PresenceEventListener(PresenceService presenceService, UserRepository userRepository) {
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        String userId = resolveUserId(event.getUser());
        if (userId != null) presenceService.onConnect(userId);
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        String userId = resolveUserId(event.getUser());
        if (userId != null) presenceService.onDisconnect(userId);
    }

    /**
     * Translates a Spring Security principal to the User.id used everywhere else.
     * Returns null for anonymous sessions (which we don't track presence for).
     */
    private String resolveUserId(Principal principal) {
        if (principal == null) return null;
        String name = principal.getName();
        if (name == null) return null;
        if (name.startsWith("guest:")) return name.substring(6);
        return userRepository.findByGoogleId(name)
                .map(User::getId)
                .orElse(null);
    }
}
