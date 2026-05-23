/**
 * Unit tests for InteractiveSessionWebSocketController. The handlers are thin
 * delegations to InteractiveSessionService, so these verify each new chunk-25
 * STOMP mapping forwards the right arguments (room code, payload, principal).
 */
package cephadex.brainflex.controller;

import static org.mockito.Mockito.verify;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import cephadex.brainflex.dto.session.EndSubmitRequest;
import cephadex.brainflex.service.InteractiveSessionService;

@ExtendWith(MockitoExtension.class)
class InteractiveSessionWebSocketControllerTest {

    @Mock
    private InteractiveSessionService service;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private InteractiveSessionWebSocketController controller;
    private final Principal principal = () -> "guest:host1";

    @BeforeEach
    void setUp() {
        controller = new InteractiveSessionWebSocketController(service, messagingTemplate);
    }

    @Test
    void endSubmit_DelegatesToService() {
        controller.endSubmit("ABCD12", new EndSubmitRequest("el-0"), principal);
        verify(service).endSubmitPhase("ABCD12", "el-0", "guest:host1");
    }

    @Test
    void restart_DelegatesToService() {
        controller.restart("ABCD12", principal);
        verify(service).restart("ABCD12", "guest:host1");
    }

    @Test
    void pauseTimer_DelegatesToService() {
        controller.pauseTimer("ABCD12", principal);
        verify(service).pauseTimer("ABCD12", "guest:host1");
    }

    @Test
    void resumeTimer_DelegatesToService() {
        controller.resumeTimer("ABCD12", principal);
        verify(service).resumeTimer("ABCD12", "guest:host1");
    }
}
