/**
 * Integration tests for GameController REST endpoints.
 * Verifies create, join, get, cancel, and results endpoints for correct
 * HTTP status codes and response shapes across authenticated and unauthenticated paths.
 */
package cephadex.brainflex.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.dto.CreateGameRequest;
import cephadex.brainflex.model.GameResult;
import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.GameSettings;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.SessionPlayer;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.GameMode;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.GameService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class GameControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private GameService gameService;

        @MockitoBean
        private UserRepository userRepository;

        private User registeredUser;
        private GameSession lobbySession;

        @BeforeEach
        void setUp() {
                registeredUser = new User();
                registeredUser.setId("user1");
                registeredUser.setGoogleId("user"); // matches @WithMockUser principal name
                registeredUser.setUserName("testhost");
                registeredUser.setIsGuest(false);

                GameSettings settings = new GameSettings();
                settings.setTotalRounds(10);
                settings.setTimePerQuestion(15);
                settings.setGameMode(GameMode.SIMULTANEOUS);

                SessionPlayer hostPlayer = new SessionPlayer();
                hostPlayer.setUserId("user1");
                hostPlayer.setUserName("testhost");

                lobbySession = new GameSession();
                lobbySession.setId("session1");
                lobbySession.setRoomCode("ABCD12");
                lobbySession.setInviteToken("token-uuid");
                lobbySession.setHostUserId("user1");
                lobbySession.setSettings(settings);
                lobbySession.setStatus(GameStatus.LOBBY);
                lobbySession.setPlayers(new ArrayList<>(List.of(hostPlayer)));
        }

        // ---- createGame ----

        @Test
        void createGame_AsRegisteredUser_ReturnsCreated() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.createSession(any(User.class), any(CreateGameRequest.class)))
                                .thenReturn(lobbySession);

                CreateGameRequest request = new CreateGameRequest("pack1", null, null, null, null, null, null);
                mockMvc.perform(post("/api/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                                .andExpect(jsonPath("$.status").value("LOBBY"));
        }

        @Test
        void createGame_AsUnauthenticated_ReturnsForbidden() throws Exception {
                when(userRepository.findByGoogleId(anyString())).thenReturn(Optional.empty());

                CreateGameRequest request = new CreateGameRequest("pack1", null, null, null, null, null, null);
                mockMvc.perform(post("/api/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void createGame_AsGuest_ReturnsForbidden() throws Exception {
                // Guest principal names start with "guest:" so resolveRegisteredUser returns
                // empty
                mockMvc.perform(post("/api/games")
                                .with(request -> {
                                        request.setUserPrincipal(() -> "guest:guestid");
                                        return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new CreateGameRequest("pack1", null, null, null, null, null, null))))
                                .andExpect(status().isForbidden());
        }

        // ---- getSession ----

        @Test
        void getSession_WithValidRoomCode_ReturnsSession() throws Exception {
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/games/ABCD12"))
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

                mockMvc.perform(get("/api/games/XXXXXX"))
                                .andExpect(status().isNotFound());
        }

        // ---- joinByRoomCode ----

        @Test
        void joinByRoomCode_AsRegisteredUser_ReturnsSession() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.joinSession("ABCD12", registeredUser)).thenReturn(lobbySession);

                mockMvc.perform(post("/api/games/ABCD12/join"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.roomCode").value("ABCD12"));
        }

        @Test
        void joinByRoomCode_AsUnauthenticated_ReturnsUnauthorized() throws Exception {
                when(userRepository.findByGoogleId(anyString())).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/games/ABCD12/join"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void joinByRoomCode_WhenGameStarted_ReturnsConflict() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                when(gameService.joinSession(anyString(), any(User.class)))
                                .thenThrow(new ResponseStatusException(
                                                org.springframework.http.HttpStatus.CONFLICT,
                                                "Game has already started"));

                mockMvc.perform(post("/api/games/ABCD12/join"))
                                .andExpect(status().isConflict());
        }

        // ---- cancelGame ----

        @Test
        void cancelGame_AsHost_ReturnsNoContent() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));

                mockMvc.perform(delete("/api/games/ABCD12"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void cancelGame_AsNonHost_ReturnsForbidden() throws Exception {
                when(userRepository.findByGoogleId("user")).thenReturn(Optional.of(registeredUser));
                org.mockito.Mockito.doThrow(new ResponseStatusException(
                                org.springframework.http.HttpStatus.FORBIDDEN, "Only the host can cancel"))
                                .when(gameService).cancelSession(anyString(), any(User.class));

                mockMvc.perform(delete("/api/games/ABCD12"))
                                .andExpect(status().isForbidden());
        }

        // ---- getResults ----

        @Test
        void getResults_WhenGameFinished_ReturnsResults() throws Exception {
                PlayerPlacement p1 = new PlayerPlacement();
                p1.setUserId("user1");
                p1.setUserName("testhost");
                p1.setFinalScore(850);
                p1.setPlacement(1);
                p1.setCorrectAnswers(8);
                p1.setTotalQuestions(10);

                GameResult result = new GameResult();
                result.setId("result1");
                result.setGameSessionId("session1");
                result.setPlacements(List.of(p1));

                when(gameService.getResults("ABCD12")).thenReturn(Optional.of(result));
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/games/ABCD12/results"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.placements[0].userName").value("testhost"))
                                .andExpect(jsonPath("$.placements[0].finalScore").value(850));
        }

        @Test
        void getResults_WhenNoResultsYet_ReturnsNotFound() throws Exception {
                when(gameService.getResults("ABCD12")).thenReturn(Optional.empty());
                when(gameService.getByRoomCode("ABCD12")).thenReturn(lobbySession);

                mockMvc.perform(get("/api/games/ABCD12/results"))
                                .andExpect(status().isNotFound());
        }
}
