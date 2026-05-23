/**
 * Unit tests for {@link NotificationService}.
 *
 * Repositories, MongoTemplate, Redis and SimpMessagingTemplate are mocked so
 * this stays a fast unit test. The interesting behaviors:
 *   - send() inserts a row + pushes over STOMP
 *   - self-notifications are skipped (userId == actor.id)
 *   - DECK_FAVORITED is throttled via Redis dedupe
 *   - guests do not receive notifications
 *   - markRead refuses to read another user's row (returns 404)
 */
package cephadex.brainflex.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.NotificationResponse;
import cephadex.brainflex.model.user.Notification;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.repository.NotificationRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private OAuthProviderService oAuthProviderService;
    @Mock private UserImageHydrator userImageHydrator;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> redisValueOps;

    @InjectMocks private NotificationService service;

    private User recipient;
    private User actor;

    @BeforeEach
    void setUp() {
        recipient = registeredUser("u-recipient", "Recipient", "google-1");
        actor = registeredUser("u-actor", "Actor", "google-2");
    }

    @Test
    void send_InsertsRowAndPushesOverStomp() {
        when(userRepository.findById("u-recipient")).thenReturn(Optional.of(recipient));
        when(notificationRepository.insert(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(oAuthProviderService.principalNameFor(recipient)).thenReturn("google-1");

        Map<String, String> meta = new HashMap<>();
        meta.put("deckId", "deck-1");
        Notification saved = service.send(
                "u-recipient", NotificationKind.DECK_COMMENT,
                "Actor commented on \"X\"", "body", "/decks/deck-1", meta, actor);

        assertNotNull(saved);
        ArgumentCaptor<Notification> rowCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).insert(rowCaptor.capture());
        Notification row = rowCaptor.getValue();
        assertEquals("u-recipient", row.getUserId());
        assertEquals(NotificationKind.DECK_COMMENT, row.getKind());
        assertEquals("u-actor", row.getActorUserId());
        assertEquals("Actor", row.getActorName());
        assertEquals("deck-1", row.getMeta().get("deckId"));
        assertEquals(false, row.isRead());

        verify(messagingTemplate).convertAndSendToUser(eq("google-1"),
                eq(NotificationService.USER_DESTINATION), any(NotificationResponse.class));
    }

    @Test
    void send_SkipsWhenActorIsRecipient() {
        Notification saved = service.send(
                "u-actor", NotificationKind.DECK_COMMENT, "self", null, "/", null, actor);
        assertNull(saved);
        verify(notificationRepository, never()).insert(any(Notification.class));
        verify(messagingTemplate, never())
                .convertAndSendToUser(anyString(), anyString(), any(Object.class));
    }

    @Test
    void send_SkipsGuests() {
        User guest = new User();
        guest.setId("guest-1");
        guest.setGuest(true);
        when(userRepository.findById("guest-1")).thenReturn(Optional.of(guest));

        Notification saved = service.send(
                "guest-1", NotificationKind.ACHIEVEMENT, "title", null, "/", null, null);
        assertNull(saved);
        verify(notificationRepository, never()).insert(any(Notification.class));
    }

    @Test
    void send_DeckFavorited_ClaimsThrottleSlot() {
        when(redis.opsForValue()).thenReturn(redisValueOps);
        when(redisValueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(true);
        when(userRepository.findById("u-recipient")).thenReturn(Optional.of(recipient));
        when(notificationRepository.insert(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(oAuthProviderService.principalNameFor(recipient)).thenReturn("google-1");

        Map<String, String> meta = new HashMap<>();
        meta.put("deckId", "deck-1");
        Notification saved = service.send(
                "u-recipient", NotificationKind.DECK_FAVORITED,
                "favorited", null, "/decks/deck-1", meta, actor);
        assertNotNull(saved);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisValueOps).setIfAbsent(keyCaptor.capture(), eq("1"), any(java.time.Duration.class));
        assertTrue(keyCaptor.getValue().startsWith("notif:favorited:u-recipient:u-actor:deck-1"));
    }

    @Test
    void send_DeckFavorited_SkipsWhenThrottleHot() {
        when(redis.opsForValue()).thenReturn(redisValueOps);
        when(redisValueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(false);

        Map<String, String> meta = new HashMap<>();
        meta.put("deckId", "deck-1");
        Notification saved = service.send(
                "u-recipient", NotificationKind.DECK_FAVORITED,
                "favorited", null, "/decks/deck-1", meta, actor);
        assertNull(saved);
        verify(notificationRepository, never()).insert(any(Notification.class));
    }

    @Test
    void markRead_FlipsRowAndReturnsDto() {
        Notification row = persistedRow("n-1", "u-recipient", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(row));

        NotificationResponse dto = service.markRead("n-1", "u-recipient");

        assertTrue(dto.read());
        assertNotNull(dto.readAt());
        verify(mongoTemplate).updateFirst(any(), any(), eq(Notification.class));
    }

    @Test
    void markRead_RowOwnedByAnotherUser_Returns404() {
        Notification row = persistedRow("n-1", "u-someone-else", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(row));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.markRead("n-1", "u-recipient"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unreadCount_DelegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse("u-recipient")).thenReturn(7L);
        assertEquals(7L, service.unreadCount("u-recipient"));
    }

    // ---- helpers ----

    private static User registeredUser(String id, String name, String googleId) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setGuest(false);
        u.setGoogleId(googleId);
        return u;
    }

    private static Notification persistedRow(String id, String userId, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setKind(NotificationKind.DECK_COMMENT);
        n.setTitle("hi");
        n.setRead(read);
        return n;
    }

}
