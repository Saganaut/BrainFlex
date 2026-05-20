package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UpdateProfileRequest;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private UserService userService;

    @Test
    void createGuest_WhenUsernameAvailable_CreatesUser() {
        when(userRepository.findByUserName("guestuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createGuest("guestuser");

        assertEquals("guestuser", result.getUserName());
        assertEquals(true, result.getIsGuest());
    }

    @Test
    void createGuest_WhenUsernameTaken_ThrowsException() {
        when(userRepository.findByUserName("taken")).thenReturn(java.util.Optional.of(new User()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.createGuest("taken");
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void register_WhenValid_CreatesUser() {
        when(oAuth2User.getAttribute("sub")).thenReturn("google123");
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("picture")).thenReturn("pic.jpg");
        when(userRepository.findByGoogleId("google123")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("testuser", true);

        User result = userService.register(oAuth2User, request);

        assertEquals("google123", result.getGoogleId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getUserName());
        assertEquals(false, result.getIsGuest());
        assertNotNull(result.getEmailVerifiedAt(),
                "Google OAuth implies a verified email, so the timestamp should be set on register");
    }

    @Test
    void register_WhenReopeningClosedAccount_BackfillsEmailVerifiedAtWhenNull() {
        User closed = new User();
        closed.setId("u1");
        closed.setGoogleId("google123");
        closed.setIsClosed(true);
        closed.setEmailVerifiedAt(null);

        when(oAuth2User.getAttribute("sub")).thenReturn("google123");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("picture")).thenReturn("pic.jpg");
        when(userRepository.findByGoogleId("google123")).thenReturn(java.util.Optional.of(closed));
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("testuser", true);
        User result = userService.register(oAuth2User, request);

        assertNotNull(result.getEmailVerifiedAt());
    }

    @Test
    void register_WhenReopeningClosedAccount_PreservesExistingEmailVerifiedAt() {
        LocalDateTime original = LocalDateTime.of(2024, 1, 15, 10, 30);
        User closed = new User();
        closed.setId("u1");
        closed.setGoogleId("google123");
        closed.setIsClosed(true);
        closed.setEmailVerifiedAt(original);

        when(oAuth2User.getAttribute("sub")).thenReturn("google123");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("picture")).thenReturn("pic.jpg");
        when(userRepository.findByGoogleId("google123")).thenReturn(java.util.Optional.of(closed));
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("testuser", true);
        User result = userService.register(oAuth2User, request);

        assertEquals(original, result.getEmailVerifiedAt(),
                "Once stamped, emailVerifiedAt is immutable — reopening should not overwrite it");
    }

    @Test
    void updateProfile_AppliesTimezoneWhenProvided() {
        User user = new User();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(
                user, new UpdateProfileRequest(null, null, null, "America/Los_Angeles"));

        assertEquals("America/Los_Angeles", result.getTimezone());
    }

    @Test
    void updateProfile_ClearsTimezoneWhenBlank() {
        User user = new User();
        user.setTimezone("America/Los_Angeles");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(
                user, new UpdateProfileRequest(null, null, null, ""));

        assertNull(result.getTimezone(),
                "Blank string clears the override so the read path can fall back to UTC");
    }

    @Test
    void updateProfile_LeavesTimezoneUnchangedWhenNull() {
        User user = new User();
        user.setTimezone("America/Los_Angeles");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // null means "client did not send the field" — preserves existing value
        User result = userService.updateProfile(
                user, new UpdateProfileRequest(null, true, null, null));

        assertEquals("America/Los_Angeles", result.getTimezone());
    }

    @Test
    void register_WhenGoogleIdExists_ThrowsException() {
        when(oAuth2User.getAttribute("sub")).thenReturn("google123");
        when(userRepository.findByGoogleId("google123")).thenReturn(java.util.Optional.of(new User()));

        RegisterRequest request = new RegisterRequest("testuser", true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.register(oAuth2User, request);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void register_WhenUsernameTaken_ThrowsException() {
        when(oAuth2User.getAttribute("sub")).thenReturn("google123");
        when(userRepository.findByGoogleId("google123")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserName("taken")).thenReturn(java.util.Optional.of(new User()));

        RegisterRequest request = new RegisterRequest("taken", true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.register(oAuth2User, request);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void isUsernameAvailable_WhenAvailable_ReturnsTrue() {
        when(userRepository.findByUserName("available")).thenReturn(java.util.Optional.empty());

        boolean result = userService.isUsernameAvailable("available");

        assertEquals(true, result);
    }

    @Test
    void isUsernameAvailable_WhenTaken_ReturnsFalse() {
        when(userRepository.findByUserName("taken")).thenReturn(java.util.Optional.of(new User()));

        boolean result = userService.isUsernameAvailable("taken");

        assertEquals(false, result);
    }
}