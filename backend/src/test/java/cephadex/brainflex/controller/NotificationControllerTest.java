/**
 * Smoke tests for {@link NotificationController}.
 *
 * The {@link NotificationService} is mocked via {@code @MockitoBean} so this
 * stays a fast controller-slice test — the underlying insert / push / dedupe
 * behavior is covered in {@code NotificationServiceTest}.
 */
package cephadex.brainflex.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cephadex.brainflex.dto.user.NotificationResponse;
import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.model.user.Notification;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.service.NotificationService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private UserService userService;

    private User caller;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        caller = new User();
        caller.setId("user-1");
        caller.setUserName("kevin");
        caller.setGuest(false);
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(caller));
    }

    @Test
    void list_ReturnsPageEnvelope() throws Exception {
        Notification row = sampleRow("n-1", "user-1");
        Page<Notification> page = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
        when(notificationService.listForUser(eq("user-1"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("n-1"))
                .andExpect(jsonPath("$.items[0].kind").value("DECK_COMMENT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void list_WhenGuest_Returns403() throws Exception {
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unreadCount_ReturnsCount() throws Exception {
        when(notificationService.unreadCount("user-1")).thenReturn(4L);
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void markRead_FlipsRow() throws Exception {
        NotificationResponse dto = NotificationResponse.from(sampleRow("n-1", "user-1"));
        when(notificationService.markRead("n-1", "user-1")).thenReturn(dto);

        mockMvc.perform(put("/api/notifications/n-1/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("n-1"));
    }

    @Test
    void markAllRead_EchoesUnreadCount() throws Exception {
        when(notificationService.markAllRead("user-1")).thenReturn(3L);
        when(notificationService.unreadCount("user-1")).thenReturn(0L);

        mockMvc.perform(put("/api/notifications/read-all").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
        verify(notificationService).markAllRead("user-1");
    }

    @Test
    void dismiss_DelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/notifications/n-1").with(csrf()))
                .andExpect(status().isOk());
        verify(notificationService).dismiss("n-1", "user-1");
    }

    private Notification sampleRow(String id, String userId) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setKind(NotificationKind.DECK_COMMENT);
        n.setTitle("Someone commented on \"X\"");
        n.setLink("/decks/deck-1");
        n.setRead(false);
        return n;
    }
}
