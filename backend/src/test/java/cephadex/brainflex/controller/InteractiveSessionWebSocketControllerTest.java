/**
 * Unit tests for InteractiveSessionWebSocketController. The handlers are thin
 * delegations to InteractiveSessionService, so these verify each new chunk-25
 * STOMP mapping forwards the right arguments (room code, payload, principal).
 */
package cephadex.brainflex.controller;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import cephadex.brainflex.dto.session.EndSubmitRequest;
import cephadex.brainflex.dto.session.message.InteractiveSessionErrorMessage;
import cephadex.brainflex.exception.ApiErrors;
import cephadex.brainflex.exception.NotFoundException;
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
    @SuppressWarnings("unused")
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

    // ── handleException: REST-parity error envelope on /user/queue/errors ──

    @Test
    void handleException_UnexpectedError_MasksAs500WithoutLeaking() {
        controller.handleException(
                new IllegalStateException("secret internal detail"),
                principal, null, "/app/interactive-session/ABCD12/start");

        InteractiveSessionErrorMessage payload = captureError();
        assertEquals(500, payload.status());
        assertEquals("INTERNAL_ERROR", payload.code());
        assertEquals(ApiErrors.GENERIC_5XX_MESSAGE, payload.message());
        assertFalse(payload.message().contains("secret"), "5xx must not leak the cause message");
        assertEquals("start", payload.operation());
        assertEquals("ABCD12", payload.roomCode());
    }

    @Test
    void handleException_ApiException_MapsStatusAndCode() {
        controller.handleException(
                new NotFoundException("SESSION_NOT_FOUND", "Interactive session not found"),
                principal, null, "/app/interactive-session/ABCD12/answer");

        InteractiveSessionErrorMessage payload = captureError();
        assertEquals(404, payload.status());
        assertEquals("SESSION_NOT_FOUND", payload.code());
        assertEquals("Interactive session not found", payload.message());
        assertEquals("answer", payload.operation());
    }

    private InteractiveSessionErrorMessage captureError() {
        ArgumentCaptor<InteractiveSessionErrorMessage> captor =
                ArgumentCaptor.forClass(InteractiveSessionErrorMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq("guest:host1"), eq("/queue/errors"), captor.capture());
        return captor.getValue();
    }
}
