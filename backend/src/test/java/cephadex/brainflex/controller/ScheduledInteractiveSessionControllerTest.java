/**
 * Integration tests for ScheduledInteractiveSessionController and
 * InviteController. Mocks the underlying service so we exercise routing,
 * security, and DTO mapping without standing up Mongo.
 */
package cephadex.brainflex.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.dto.AddInviteRequest;
import cephadex.brainflex.dto.CreateScheduledInteractiveSessionRequest;
import cephadex.brainflex.dto.RedeemInviteResponse;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.model.session.InteractiveSessionInvite;
import cephadex.brainflex.model.session.ScheduledInteractiveSession;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.ScheduledInteractiveSessionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class ScheduledInteractiveSessionControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ScheduledInteractiveSessionService scheduleService;
        @MockitoBean
        private UserRepository userRepository;
        @MockitoBean
        private DeckRepository deckRepository;

        private User host;
        private Deck deck;

        @BeforeEach
        void setUp() {
                host = new User();
                host.setId("host1");
                host.setGoogleId("user"); // matches @WithMockUser principal
                host.setUserName("aragorn");
                host.setName("Aragorn");
                host.setGuest(false);

                deck = new Deck();
                deck.setId("deck1");
                deck.getContent().setName("Middle Earth");

                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(host));
                when(deckRepository.findById("deck1")).thenReturn(Optional.of(deck));
        }

        @Test
        void listMine_ReturnsHostsScheduledRows() throws Exception {
                when(scheduleService.listMine("host1")).thenReturn(List.of(buildSchedule()));

                mockMvc.perform(get("/api/scheduled-interactive-sessions/mine"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("sched1"))
                                .andExpect(jsonPath("$[0].hostName").value("Aragorn"))
                                .andExpect(jsonPath("$[0].deckName").value("Middle Earth"));
        }

        @Test
        void create_AcceptsValidRequest() throws Exception {
                when(scheduleService.schedule(any(User.class), any(CreateScheduledInteractiveSessionRequest.class)))
                                .thenReturn(buildSchedule());

                CreateScheduledInteractiveSessionRequest body = new CreateScheduledInteractiveSessionRequest(
                                "deck1",
                                Instant.now().plus(Duration.ofHours(2)),
                                null, null, null,
                                List.of("a@b.com"));

                mockMvc.perform(post("/api/scheduled-interactive-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("sched1"))
                                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }

        @Test
        void create_400_OnMissingDeckId() throws Exception {
                String bad = """
                                {"scheduledStartAt": "2099-01-01T10:00:00"}
                                """;
                mockMvc.perform(post("/api/scheduled-interactive-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bad))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getOne_403_WhenCallerNotHost() throws Exception {
                ScheduledInteractiveSession schedule = buildSchedule();
                schedule.setHostUserId("other");
                when(scheduleService.getById("sched1")).thenReturn(schedule);

                mockMvc.perform(get("/api/scheduled-interactive-sessions/sched1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void cancel_ReturnsCancelledDTO() throws Exception {
                ScheduledInteractiveSession schedule = buildSchedule();
                schedule.setStatus(ScheduleStatus.CANCELLED);
                when(scheduleService.cancel(eq("sched1"), any(User.class))).thenReturn(schedule);

                mockMvc.perform(post("/api/scheduled-interactive-sessions/sched1/cancel"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void addInvite_201() throws Exception {
                InteractiveSessionInvite invite = new InteractiveSessionInvite();
                invite.setId("inv1");
                invite.setEmail("frodo@shire.org");
                when(scheduleService.addInvite(eq("sched1"), any(User.class), eq("frodo@shire.org")))
                                .thenReturn(invite);

                mockMvc.perform(post("/api/scheduled-interactive-sessions/sched1/invite")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AddInviteRequest("frodo@shire.org"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("inv1"))
                                .andExpect(jsonPath("$.email").value("frodo@shire.org"));
        }

        // ---- InviteController.redeem ----

        @Test
        void redeem_IsPublic_AndReturnsRoomCode() throws Exception {
                when(scheduleService.redeem(eq("tok"), any()))
                                .thenReturn(new RedeemInviteResponse(
                                                "sched1", "live1", "ABCD12", "Middle Earth", "Aragorn",
                                                Instant.now().plus(Duration.ofHours(1))));

                mockMvc.perform(post("/api/invites/tok/redeem"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                                .andExpect(jsonPath("$.deckName").value("Middle Earth"));
        }

        private ScheduledInteractiveSession buildSchedule() {
                ScheduledInteractiveSession s = new ScheduledInteractiveSession();
                s.setId("sched1");
                s.setHostUserId("host1");
                s.setDeckId("deck1");
                s.setStatus(ScheduleStatus.SCHEDULED);
                s.setScheduledStartAt(Instant.now().plus(Duration.ofHours(2)));
                return s;
        }
}
