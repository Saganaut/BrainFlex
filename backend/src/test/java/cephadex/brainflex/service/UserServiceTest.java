package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.user.RegisterRequest;
import cephadex.brainflex.dto.user.UpdateProfileRequest;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.email.EmailService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthProviderService oAuthProviderService;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private EmailService emailService;

    /** Chunk 20 — register() now calls autoJoinByEmailDomain on the new User
     *  to land them in every org claiming their email domain. Mocked so the
     *  unit test stays a registration-only assertion. */
    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private UserService userService;

    /** Helper — wraps {@link #oAuth2User} in a real OAuth2AuthenticationToken
     *  so tests can call {@code userService.register(token, ...)}. The
     *  registration id is irrelevant here because oAuthProviderService is
     *  itself mocked and never inspects the token's registration. */
    private OAuth2AuthenticationToken googleToken() {
        return new OAuth2AuthenticationToken(
                oAuth2User,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                OAuthProviderService.GOOGLE);
    }

    @Test
    void createGuest_WhenUsernameAvailable_CreatesUser() {
        when(userRepository.findByUserName("guestuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createGuest("guestuser");

        assertEquals("guestuser", result.getUserName());
        assertEquals(true, result.isGuest());
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
        var profile = new OAuthProviderService.ProviderProfile(
                OAuthProviderService.GOOGLE, "google123", "test@example.com", "Test User", "pic.jpg");
        when(oAuthProviderService.profileOf(any())).thenReturn(profile);
        when(oAuthProviderService.findByProviderId(OAuthProviderService.GOOGLE, "google123"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // Mirror what OAuthProviderService.setProviderIdOn would have written
            // so the returned User has its googleId set, matching production behavior.
            u.setGoogleId("google123");
            return u;
        });

        RegisterRequest request = new RegisterRequest("testuser", true);

        User result = userService.register(googleToken(), request);

        assertEquals("google123", result.getGoogleId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getUserName());
        assertEquals(false, result.isGuest());
        assertNotNull(result.getEmailVerifiedAt(),
                "Google OAuth implies a verified email, so the timestamp should be set on register");
    }

    @Test
    void register_WhenReopeningClosedAccount_BackfillsEmailVerifiedAtWhenNull() {
        User closed = new User();
        closed.setId("u1");
        closed.setGoogleId("google123");
        closed.setClosed(true);
        closed.setEmailVerifiedAt(null);

        var profile = new OAuthProviderService.ProviderProfile(
                OAuthProviderService.GOOGLE, "google123", null, "Test User", "pic.jpg");
        when(oAuthProviderService.profileOf(any())).thenReturn(profile);
        when(oAuthProviderService.findByProviderId(OAuthProviderService.GOOGLE, "google123"))
                .thenReturn(java.util.Optional.of(closed));
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("testuser", true);
        User result = userService.register(googleToken(), request);

        assertNotNull(result.getEmailVerifiedAt());
    }

    @Test
    void register_WhenReopeningClosedAccount_PreservesExistingEmailVerifiedAt() {
        Instant original = Instant.parse("2024-01-15T10:30:00Z");
        User closed = new User();
        closed.setId("u1");
        closed.setGoogleId("google123");
        closed.setClosed(true);
        closed.setEmailVerifiedAt(original);

        var profile = new OAuthProviderService.ProviderProfile(
                OAuthProviderService.GOOGLE, "google123", null, "Test User", "pic.jpg");
        when(oAuthProviderService.profileOf(any())).thenReturn(profile);
        when(oAuthProviderService.findByProviderId(OAuthProviderService.GOOGLE, "google123"))
                .thenReturn(java.util.Optional.of(closed));
        when(userRepository.findByUserName("testuser")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("testuser", true);
        User result = userService.register(googleToken(), request);

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
        var profile = new OAuthProviderService.ProviderProfile(
                OAuthProviderService.GOOGLE, "google123", null, null, null);
        when(oAuthProviderService.profileOf(any())).thenReturn(profile);
        when(oAuthProviderService.findByProviderId(OAuthProviderService.GOOGLE, "google123"))
                .thenReturn(java.util.Optional.of(new User()));

        RegisterRequest request = new RegisterRequest("testuser", true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.register(googleToken(), request);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void register_WhenUsernameTaken_ThrowsException() {
        var profile = new OAuthProviderService.ProviderProfile(
                OAuthProviderService.GOOGLE, "google123", null, null, null);
        when(oAuthProviderService.profileOf(any())).thenReturn(profile);
        when(oAuthProviderService.findByProviderId(OAuthProviderService.GOOGLE, "google123"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserName("taken")).thenReturn(java.util.Optional.of(new User()));

        RegisterRequest request = new RegisterRequest("taken", true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.register(googleToken(), request);
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
