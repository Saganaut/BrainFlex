/**
 * Configures STOMP over WebSocket for real-time game sessions.
 * Registers the /ws endpoint with a SockJS fallback for browsers that
 * don't support native WebSocket, and sets up the in-memory message broker
 * that routes /topic broadcasts and /app client-to-server messages.
 */
package cephadex.brainflex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // allowedOriginPatterns required when credentials are used with SockJS
                .setAllowedOriginPatterns("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client-to-server messages are routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Server-to-client broadcasts go through the in-memory broker on /topic
        // Replace with a Redis-backed broker here when scaling horizontally
        config.enableSimpleBroker("/topic");
    }
}
