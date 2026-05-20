/**
 * Unit tests for ScheduledInteractiveSessionService. Verifies the schedule
 * lifecycle (schedule / cancel / boot / complete / redeem), invite token
 * generation + dedupe, and ownership checks. Email sends are exercised
 * against a captured mock {@link EmailService}.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.AddInviteRequest;
import cephadex.brainflex.dto.CreateScheduledInteractiveSessionRequest;
import cephadex.brainflex.dto.RedeemInviteResponse;
import cephadex.brainflex.dto.UpdateScheduledInteractiveSessionRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionInvite;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.InteractiveSessionInviteRepository;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.ScheduledInteractiveSessionRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ScheduledInteractiveSessionServiceTest {

    @Mock private ScheduledInteractiveSessionRepository scheduleRepository;
    @Mock private InteractiveSessionInviteRepository inviteRepository;
    @Mock private InteractiveSessionRepository interactiveSessionRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private UserRepository userRepository;
    @Mock private InteractiveSessionService interactiveSessionService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ScheduledInteractiveSessionService service;

    private User host;
    private Deck deck;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId("host1");
        host.setName("Aragorn");
        host.setUserName("aragorn");

        deck = new Deck();
        deck.setId("deck1");
        deck.setName("Lore of Middle Earth");
        deck.setDefaultSettings(new InteractiveSessionSettings());

        lenient().when(scheduleRepository.save(any(ScheduledInteractiveSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(inviteRepository.save(any(InteractiveSessionInvite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- schedule() ----

    @Test
    void schedule_CreatesRowAndInvites_ForEachUniqueEmail() {
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        CreateScheduledInteractiveSessionRequest request = new CreateScheduledInteractiveSessionRequest(
                "deck1",
                LocalDateTime.now().plusHours(2),
                null,
                null,
                "Hope to see you there!",
                List.of("Alice@Example.com", "  bob@example.com  ", "alice@example.com"));

        ScheduledInteractiveSession saved = service.schedule(host, request);

        assertEquals(ScheduleStatus.SCHEDULED, saved.getStatus());
        assertEquals(host.getId(), saved.getHostUserId());
        assertEquals(List.of("alice@example.com", "bob@example.com"), saved.getInvitedEmails());

        ArgumentCaptor<InteractiveSessionInvite> captor = ArgumentCaptor.forClass(InteractiveSessionInvite.class);
        verify(inviteRepository, times(2)).save(captor.capture());
        for (InteractiveSessionInvite invite : captor.getAllValues()) {
            assertNotNull(invite.getInviteToken());
            assertNotNull(invite.getExpiresAt());
            assertEquals(host.getId(), invite.getInvitedByUserId());
        }
        verify(emailService, times(2)).sendInitialInvite(any(), any(), eq("Aragorn"), eq("Lore of Middle Earth"));
    }

    @Test
    void schedule_DefaultsSettingsFromDeck_WhenNullProvided() {
        InteractiveSessionSettings deckDefaults = new InteractiveSessionSettings();
        deckDefaults.setMaxPlayers(50);
        deck.setDefaultSettings(deckDefaults);
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));

        ScheduledInteractiveSession saved = service.schedule(host, new CreateScheduledInteractiveSessionRequest(
                "deck1", LocalDateTime.now().plusHours(1), null, null, null, List.of()));

        assertEquals(50, saved.getSettings().getMaxPlayers());
    }

    @Test
    void schedule_404_WhenDeckMissing() {
        when(deckRepository.findById(anyString())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.schedule(host, new CreateScheduledInteractiveSessionRequest(
                        "missing", LocalDateTime.now().plusHours(1), null, null, null, List.of())));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- cancel() ----

    @Test
    void cancel_SetsStatusAndNotifiesInvitees() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setEmail("alice@example.com");
        when(inviteRepository.findByScheduledInteractiveSessionId("sched1"))
                .thenReturn(List.of(invite));

        ScheduledInteractiveSession cancelled = service.cancel("sched1", host);

        assertEquals(ScheduleStatus.CANCELLED, cancelled.getStatus());
        verify(emailService).sendCancelNotice(any(), eq(List.of(invite)),
                eq("Aragorn"), eq("Lore of Middle Earth"));
    }

    @Test
    void cancel_RejectsNonHosts() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        User intruder = new User(); intruder.setId("other");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.cancel("sched1", intruder));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancel_ConflictOnAlreadyLive() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.cancel("sched1", host));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- boot() ----

    @Test
    void boot_TransitionsToLiveAndMailsReminders() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(userRepository.findById("host1")).thenReturn(Optional.of(host));
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));

        InteractiveSession live = new InteractiveSession();
        live.setId("live1");
        live.setRoomCode("ABCD12");
        when(interactiveSessionService.createInteractiveSession(eq(host), any()))
                .thenReturn(live);

        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setEmail("alice@example.com");
        when(inviteRepository.findByScheduledInteractiveSessionId("sched1"))
                .thenReturn(new ArrayList<>(List.of(invite)));

        InteractiveSession result = service.boot("sched1");

        assertEquals("live1", result.getId());
        ArgumentCaptor<ScheduledInteractiveSession> scheduleCaptor =
                ArgumentCaptor.forClass(ScheduledInteractiveSession.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertEquals(ScheduleStatus.LIVE, scheduleCaptor.getValue().getStatus());
        assertEquals("live1", scheduleCaptor.getValue().getCreatedInteractiveSessionId());
        assertEquals("live1", invite.getInteractiveSessionId());
        verify(emailService).sendBootReminder(any(), eq(invite), eq("Aragorn"),
                eq("Lore of Middle Earth"), eq("ABCD12"));
    }

    @Test
    void boot_IsIdempotent_WhenAlreadyLive() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        schedule.setCreatedInteractiveSessionId("live1");
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        InteractiveSession live = new InteractiveSession();
        live.setId("live1");
        when(interactiveSessionRepository.findById("live1")).thenReturn(Optional.of(live));

        InteractiveSession result = service.boot("sched1");
        assertEquals("live1", result.getId());
        verify(interactiveSessionService, never()).createInteractiveSession(any(), any());
    }

    @Test
    void boot_ConflictWhenCancelled() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.CANCELLED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.boot("sched1"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- markComplete() ----

    @Test
    void markComplete_TransitionsLiveScheduleToCompleted() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        schedule.setCreatedInteractiveSessionId("live1");
        when(scheduleRepository.findByCreatedInteractiveSessionId("live1"))
                .thenReturn(Optional.of(schedule));

        service.markComplete("live1");

        ArgumentCaptor<ScheduledInteractiveSession> captor =
                ArgumentCaptor.forClass(ScheduledInteractiveSession.class);
        verify(scheduleRepository).save(captor.capture());
        assertEquals(ScheduleStatus.COMPLETED, captor.getValue().getStatus());
    }

    @Test
    void markComplete_NoOpForUnrelatedSessions() {
        when(scheduleRepository.findByCreatedInteractiveSessionId("live1"))
                .thenReturn(Optional.empty());

        service.markComplete("live1");

        verify(scheduleRepository, never()).save(any());
    }

    // ---- redeem() ----

    @Test
    void redeem_StampsRedeemedAtAndReturnsRoomCode_WhenAlreadyBooted() {
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setInviteToken("tok");
        invite.setInteractiveSessionId("live1");
        invite.setScheduledInteractiveSessionId("sched1");
        invite.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(inviteRepository.findByInviteToken("tok")).thenReturn(Optional.of(invite));

        InteractiveSession live = new InteractiveSession();
        live.setId("live1");
        live.setRoomCode("ABCD12");
        when(interactiveSessionRepository.findById("live1")).thenReturn(Optional.of(live));

        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(userRepository.findById("host1")).thenReturn(Optional.of(host));

        RedeemInviteResponse response = service.redeem("tok", "redeemer1");

        assertEquals("ABCD12", response.roomCode());
        assertEquals("Lore of Middle Earth", response.deckName());
        assertEquals("Aragorn", response.hostName());
        assertEquals("redeemer1", invite.getResolvedUserId());
        assertNotNull(invite.getRedeemedAt());
    }

    @Test
    void redeem_ReturnsNullRoomCode_WhenScheduleStillSCHEDULED() {
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setInviteToken("tok");
        invite.setScheduledInteractiveSessionId("sched1");
        invite.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(inviteRepository.findByInviteToken("tok")).thenReturn(Optional.of(invite));

        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(userRepository.findById("host1")).thenReturn(Optional.of(host));

        RedeemInviteResponse response = service.redeem("tok", null);
        assertNull(response.roomCode());
        assertNotNull(invite.getRedeemedAt());
    }

    @Test
    void redeem_410_WhenExpired() {
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setInviteToken("tok");
        invite.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(inviteRepository.findByInviteToken("tok")).thenReturn(Optional.of(invite));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.redeem("tok", null));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void redeem_404_WhenTokenUnknown() {
        when(inviteRepository.findByInviteToken("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.redeem("missing", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---- addInvite() ----

    @Test
    void addInvite_AddsToListAndMailsInitial() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        when(inviteRepository.existsByEmailAndScheduledInteractiveSessionId(anyString(), anyString()))
                .thenReturn(false);

        InteractiveSessionInvite created = service.addInvite("sched1", host, "Frodo@Shire.org");

        assertEquals("frodo@shire.org", created.getEmail());
        assertEquals("frodo@shire.org", schedule.getInvitedEmails().get(0));
        verify(emailService).sendInitialInvite(any(), any(), eq("Aragorn"),
                eq("Lore of Middle Earth"));
    }

    @Test
    void addInvite_409_OnDuplicate() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(inviteRepository.existsByEmailAndScheduledInteractiveSessionId("frodo@shire.org", "sched1"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.addInvite("sched1", host, "frodo@shire.org"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void addInvite_ConflictOnceLive() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.addInvite("sched1", host, "frodo@shire.org"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- update() ----

    @Test
    void update_AppliesProvidedFields() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        LocalDateTime newStart = LocalDateTime.now().plusDays(1);
        UpdateScheduledInteractiveSessionRequest update = new UpdateScheduledInteractiveSessionRequest(
                newStart, null, null, "Reminder body!");

        ScheduledInteractiveSession out = service.update("sched1", host, update);

        assertEquals(newStart, out.getScheduledStartAt());
        assertEquals("Reminder body!", out.getReminderEmailTemplate());
    }

    @Test
    void update_ConflictOnceLive() {
        ScheduledInteractiveSession schedule = buildSchedule(ScheduleStatus.LIVE);
        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.update("sched1", host,
                        new UpdateScheduledInteractiveSessionRequest(null, null, null, null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- helpers ----

    private ScheduledInteractiveSession buildSchedule(ScheduleStatus status) {
        ScheduledInteractiveSession schedule = new ScheduledInteractiveSession();
        schedule.setId("sched1");
        schedule.setHostUserId(host.getId());
        schedule.setDeckId(deck.getId());
        schedule.setStatus(status);
        schedule.setScheduledStartAt(LocalDateTime.now().plusHours(1));
        schedule.setInvitedEmails(new ArrayList<>());
        return schedule;
    }

    @SuppressWarnings("unused") // kept for parity with helper signature
    private static InteractiveSessionInvite tokenInvite(String token) {
        InteractiveSessionInvite invite = new InteractiveSessionInvite();
        invite.setInviteToken(token);
        return invite;
    }
}
