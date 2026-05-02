package cephadex.brainflex.controller;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Test
    void getCurrentUser_WhenNotAuthenticated_ReturnsGuestStub() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("0"))
                .andExpect(jsonPath("$.userName").value("Guest"))
                .andExpect(jsonPath("$.isGuest").value(true));
    }

    @Test
    @WithMockUser(username = "google123")
    void getCurrentUser_WhenAuthenticated_ReturnsUser() throws Exception {
        User user = new User();
        user.setId("1");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setUserName("testuser");
        user.setIsGuest(false);
        user.setGoogleId("google123");
        user.setStats(new PlayerStats());

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void guestLogin_WhenValidUsername_CreatesGuest() throws Exception {
        User user = new User();
        user.setId("1");
        user.setUserName("guestuser");
        user.setIsGuest(true);
        user.setStats(new PlayerStats());

        when(userService.createGuest("guestuser")).thenReturn(user);

        UserDTO.GuestLoginRequest request = new UserDTO.GuestLoginRequest("guestuser");

        mockMvc.perform(post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.userName").value("guestuser"))
                .andExpect(jsonPath("$.isGuest").value(true));
    }

    @Test
    @WithMockUser(username = "google123")
    void register_WhenAuthenticated_RegistersUser() throws Exception {
        User user = new User();
        user.setId("1");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setUserName("testuser");
        user.setIsGuest(false);

        RegisterRequest request = new RegisterRequest("testuser", true);

        when(userService.register(null, request)).thenReturn(user); // Mock OAuth2User as null for simplicity

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void register_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", true);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}