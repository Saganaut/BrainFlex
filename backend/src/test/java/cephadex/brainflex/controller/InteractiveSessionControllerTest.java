/**
 * Integration tests for InteractiveSessionController REST endpoints.
 * Verifies create, join, get, cancel, and results endpoints for correct
 * HTTP status codes and response shapes across authenticated and unauthenticated paths.
 */
package cephadex.brainflex.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.dto.session.CreateInteractiveSessionRequest;
import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.enums.AnswerSubmissionMode;
import cephadex.brainflex.model.enums.SessionLifecycle;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.InteractiveSessionPlayer;
import cephadex.brainflex.model.session.InteractiveSessionResult;
import cephadex.brainflex.model.session.InteractiveSessionSettings;
import cephadex.brainflex.model.session.PlayerPlacement;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.InteractiveSessionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class InteractiveSessionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private InteractiveSessionService gameService;

        @MockitoBean
        private UserRepository userRepository;

        private User registeredUser;
        private InteractiveSession lobbySession;

        @BeforeEach
        void setUp() {
                registeredUser = new User();
                registeredUser.setId("user1");
                registeredUser.setGoogleId("user"); // matches @WithMockUser principal name
                registeredUser.setUserName("testhost");
                registeredUser.setGuest(false);

                InteractiveSessionSettings settings = new InteractiveSessionSettings();
                settings.setTotalRounds(10);
                settings.setTimePerQuestion(15);
                settings.setAnswerSubmissionMode(AnswerSubmissionMode.SIMULTANEOUS);

                InteractiveSessionPlayer hostPlayer = new InteractiveSessionPlayer();
                hostPlayer.setUser(UserSnapshot.of("user1", "testhost"));

                lobbySession = new InteractiveSession();
                lobbySession.setId("session1");
                lobbySession.setRoomCode("ABCD12");
                lobbySession.setInviteToken("token-uuid");
                lobbySession.setHostUserId("user1");
                lobbySession.getContent().setSettings(settings);
                lobbySession.setStatus(SessionLifecycle.LOBBY);
                lobbySession.setPlayers(new ArrayList<>(List.of(hostPlayer)));
        }

        // ---- createGame ----

        @Test
        void createGame_AsRegisteredUser_ReturnsCreated() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.createInteractiveSession(any(User.class), any(CreateInteractiveSessionRequest.class)))
                                .thenReturn(lobbySession);

                CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest("deck1", null, null, null,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null);
                mockMvc.perform(post("/api/interactive-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                                .andExpect(jsonPath("$.status").value("LOBBY"));
        }

        @Test
        void createGame_AsUnauthenticated_ReturnsForbidden() throws Exception {
                when(userRepository.findByGoogleId(anyString())).thenReturn(Optional.empty());

                CreateInteractiveSessionRequest request = new CreateInteractiveSessionRequest("deck1", null, null, null,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null);
                mockMvc.perform(post("/api/interactive-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void createGame_AsGuest_ReturnsForbidden() throws Exception {
                // Guest principal names start with "guest:" so resolveRegisteredUser returns
                // empty
                mockMvc.perform(post("/api/interactive-sessions")
                                .with(request -> {
                                        request.setUserPrincipal(() -> "guest:guestid");
                                        return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new CreateInteractiveSessionRequest("deck1", null, null, null, null,
                                                                null, null, null, null, null, null, null, null, null,
                                                                null, null, null, null, null, null, null, null, null,
                                                                null, null, null))))
                                .andExpect(status().isForbidden());
        }

        // ---- getSession ----

        @Test
        void getSession_WithValidRoomCode_ReturnsSession() throws Exception {
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/interactive-sessions/ABCD12"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                                .andExpect(jsonPath("$.status").value("LOBBY"))
                                .andExpect(jsonPath("$.players").isArray());
        }

        @Test
        void getSession_WithInvalidRoomCode_ReturnsNotFound() throws Exception {
                when(gameService.getByRoomCode("XXXXXX"))
                                .thenThrow(new ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Game session not found"));

                mockMvc.perform(get("/api/interactive-sessions/XXXXXX"))
                                .andExpect(status().isNotFound());
        }

        // ---- joinByRoomCode ----

        @Test
        void joinByRoomCode_AsRegisteredUser_ReturnsSession() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.joinInteractiveSession("ABCD12", registeredUser, null, null, null))
                                .thenReturn(lobbySession);

                mockMvc.perform(post("/api/interactive-sessions/ABCD12/join"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"));
        }

        @Test
        void joinByRoomCode_AsUnauthenticated_ReturnsUnauthorized() throws Exception {
                when(userRepository.findByGoogleId(anyString())).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/interactive-sessions/ABCD12/join"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void joinByRoomCode_WhenGameStarted_ReturnsConflict() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.joinInteractiveSession(anyString(), any(User.class), any(), any(), any()))
                                .thenThrow(new ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "Game has already started"));

                mockMvc.perform(post("/api/interactive-sessions/ABCD12/join"))
                                .andExpect(status().isConflict());
        }

        // ---- cancelGame ----

        @Test
        void cancelGame_AsHost_ReturnsNoContent() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));

                mockMvc.perform(delete("/api/interactive-sessions/ABCD12"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void cancelGame_AsNonHost_ReturnsForbidden() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                org.mockito.Mockito.doThrow(new ResponseStatusException(
                                org.springframework.http.HttpStatus.FORBIDDEN, "Only the host can cancel"))
                                .when(gameService).cancelInteractiveSession(anyString(), any(User.class));

                mockMvc.perform(delete("/api/interactive-sessions/ABCD12"))
                                .andExpect(status().isForbidden());
        }

        // ---- getResults ----

        @Test
        void getResults_WhenGameFinished_ReturnsResults() throws Exception {
                PlayerPlacement p1 = new PlayerPlacement();
                p1.setUser(UserSnapshot.of("user1", "testhost"));
                p1.setFinalScore(850);
                p1.setPlacement(1);
                p1.setCorrectAnswers(8);
                p1.setTotalQuestions(10);

                InteractiveSessionResult result = new InteractiveSessionResult();
                result.setId("result1");
                result.setInteractiveSessionId("session1");
                result.setPlacements(List.of(p1));

                when(gameService.getResults("ABCD12")).thenReturn(Optional.of(result));
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/interactive-sessions/ABCD12/results"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.placements[0].user.name").value("testhost"))
                                .andExpect(jsonPath("$.placements[0].finalScore").value(850));
        }

        @Test
        void getResults_WhenNoResultsYet_ReturnsNotFound() throws Exception {
                when(gameService.getResults("ABCD12")).thenReturn(Optional.empty());
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/interactive-sessions/ABCD12/results"))
                                .andExpect(status().isNotFound());
        }

        // ---- Team CRUD (chunk 12) ----

        @Test
        void createTeam_AsRegisteredUser_ReturnsCreated() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.createTeam(anyString(), anyString(), anyString(), any(User.class)))
                                .thenReturn(lobbySession);

                mockMvc.perform(post("/api/interactive-sessions/ABCD12/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Red Lions\",\"color\":\"red\"}"))
                                .andExpect(status().isCreated());
        }

        @Test
        void movePlayer_AsRegisteredUser_ReturnsOk() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.movePlayerToTeam(anyString(), anyString(), anyString(), any(User.class)))
                                .thenReturn(lobbySession);

                mockMvc.perform(put("/api/interactive-sessions/ABCD12/players/user2/team")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"teamId\":\"team-1\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void movePlayer_WithBlankTeamId_ReturnsBadRequest() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));

                mockMvc.perform(put("/api/interactive-sessions/ABCD12/players/user2/team")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"teamId\":\"\"}"))
                                .andExpect(status().isBadRequest());
        }
}
