package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Test
    void getLeaderboard_ReturnsGuestUsers() throws Exception {
        User user = new User();
        user.setId("1");
        user.setUserName("testuser");
        user.setIsGuest(true);
        user.setPictureUrl("pic.jpg");
        PlayerStats stats = new PlayerStats();
        stats.setGamesPlayed(10);
        user.setStats(stats);

        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].userName").value("testuser"))
                .andExpect(jsonPath("$[0].isGuest").value(true));
    }

    @Test
    void checkUsername_ReturnsAvailability() throws Exception {
        when(userService.isUsernameAvailable("testuser")).thenReturn(true);

        mockMvc.perform(get("/api/users/check-username").param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getUserProfile_WhenUserExists_ReturnsUser() throws Exception {
        User user = new User();
        user.setId("1");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setUserName("testuser");
        user.setIsGuest(false);
        user.setGoogleId("google123");
        user.setPictureUrl("pic.jpg");
        user.setStats(new PlayerStats());
        user.setLastLogin(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserProfile_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isNotFound());
    }
}