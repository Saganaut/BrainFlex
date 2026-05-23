package cephadex.brainflex.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UpdateProfileRequest;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.OAuthProviderService.ProviderProfile;
import cephadex.brainflex.service.email.EmailCategory;
import cephadex.brainflex.service.email.EmailJob;
import cephadex.brainflex.service.email.EmailService;
import cephadex.brainflex.service.email.EmailTemplate;
import cephadex.brainflex.model.user.NotificationPrefs;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final OAuthProviderService oAuthProviderService;
    private final EmailService emailService;
    private final OrganizationService organizationService;

    /**
     * {@code @Lazy} on {@link OrganizationService} keeps Spring's bean graph
     * acyclic — OrganizationService injects UserRepository directly, while the
     * OAuth success handler in SecurityConfig wires both services together
     * through constructor injection on UserService. Lazy resolution closes
     * the would-be cycle without forcing a setter-based wiring.
     */
    public UserService(UserRepository userRepository,
            OAuthProviderService oAuthProviderService,
            EmailService emailService,
            @Lazy OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.oAuthProviderService = oAuthProviderService;
        this.emailService = emailService;
        this.organizationService = organizationService;
    }

    public User register(OAuth2AuthenticationToken token, RegisterRequest request) {
        ProviderProfile profile = oAuthProviderService.profileOf(token);

        var existingByProvider = oAuthProviderService
                .findByProviderId(profile.provider(), profile.providerId());
        if (existingByProvider.isPresent()) {
            User existing = existingByProvider.get();
            if (!existing.isClosed())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already registered");

            // Reopen a closed account — username check excludes the user's own record
            var usernameTaken = userRepository.findByUserName(request.username());
            if (usernameTaken.isPresent() && !usernameTaken.get().getId().equals(existing.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

            existing.setClosed(false);
            existing.setClosedAt(null);
            existing.setUserName(request.username());
            existing.setNewsletter(request.newsletter());
            existing.setPictureUrl(profile.picture());
            existing.setName(profile.name());
            existing.setLastLogin(Instant.now());
            if (existing.getEmailVerifiedAt() == null)
                existing.setEmailVerifiedAt(Instant.now());
            User reopened = userRepository.save(existing);
            // Reopening sends the welcome email again — the user just went
            // through the same flow as a fresh sign-up and seeing a "welcome
            // back" beat reads better than silence.
            sendWelcomeEmail(reopened);
            return reopened;
        }

        if (userRepository.findByUserName(request.username()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

        User user = new User();
        oAuthProviderService.setProviderIdOn(user, profile);
        user.setEmail(profile.email());
        user.setName(profile.name());
        user.setPictureUrl(profile.picture());
        user.setUserName(request.username());
        user.setGuest(false);
        user.setNewsletter(request.newsletter());
        user.setLastLogin(Instant.now());
        user.setEmailVerifiedAt(Instant.now());

        // Chunk 20 — materialise prefs at registration so reads never have to
        // chain through withDefaults() once the migration backfills legacy users.
        NotificationPrefs prefs = NotificationPrefs.withDefaults();
        prefs.setMarketingEmail(request.newsletter());
        user.setNotificationPrefs(prefs);

        // Chunk 20 — propagate displayName off the OAuth-supplied name so the
        // user record carries a renderable handle on day one.
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(firstNonBlank(profile.name(), request.username()));
        }

        User saved = userRepository.save(user);
        // Chunk 20 — sweep email-domain-claimed orgs on first registration so
        // the lobby's "Your orgs" view is populated when the user lands on it.
        organizationService.autoJoinByEmailDomain(saved);
        sendWelcomeEmail(saved);
        return saved;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank())
            return a;
        if (b != null && !b.isBlank())
            return b;
        return null;
    }

    public User createGuest(String username) {
        if (userRepository.findByUserName(username).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

        User user = new User();
        user.setUserName(username);
        user.setGuest(true);
        user.setLastLogin(Instant.now());

        return userRepository.save(user);
    }

    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUserName(username).isEmpty();
    }

    public User updateProfile(User user, UpdateProfileRequest request) {
        if (request.pictureUrl() != null && !request.pictureUrl().isBlank()) {
            user.setPictureUrl(request.pictureUrl());
            // Symmetric with AccountController.uploadProfileImage, which clears
            // pictureUrl when storing uploaded variants. The hydrator prefers
            // variants over pictureUrl, so leaving stale variants here would
            // hide the user's freshly-picked built-in avatar.
            user.getPictureVariants().clear();
        }
        if (request.newsletter() != null) {
            user.setNewsletter(request.newsletter());
        }
        if (request.activeThemeId() != null) {
            user.setActiveThemeId(request.activeThemeId().isBlank() ? null : request.activeThemeId());
        }
        // Empty string clears the override (back to "no preference / UTC fallback");
        // null means "client did not send the field" and is a no-op.
        if (request.timezone() != null) {
            user.setTimezone(request.timezone().isBlank() ? null : request.timezone());
        }
        return userRepository.save(user);
    }

    public void closeAccount(User user) {
        user.setClosed(true);
        user.setClosedAt(Instant.now());
        userRepository.save(user);
        sendAccountClosedEmail(user);
    }

    private void sendWelcomeEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank())
            return;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("displayName", displayNameOf(user));
            emailService.enqueue(EmailJob.builder()
                    .recipient(user.getEmail())
                    .userId(user.getId())
                    .category(EmailCategory.TRANSACTIONAL)
                    .template(EmailTemplate.WELCOME)
                    .model(model)
                    .build());
        } catch (Exception e) {
            // Best-effort — registration must not fail because the outbox is down.
            log.warn("Welcome email enqueue failed for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendAccountClosedEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank())
            return;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("displayName", displayNameOf(user));
            emailService.enqueue(EmailJob.builder()
                    .recipient(user.getEmail())
                    .userId(user.getId())
                    .category(EmailCategory.TRANSACTIONAL)
                    .template(EmailTemplate.ACCOUNT_CLOSED)
                    .model(model)
                    .build());
        } catch (Exception e) {
            log.warn("Account-closed email enqueue failed for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private static String displayNameOf(User user) {
        if (user.getName() != null && !user.getName().isBlank())
            return user.getName();
        if (user.getUserName() != null && !user.getUserName().isBlank())
            return user.getUserName();
        return "there";
    }

    // For OAuth2-authenticated callers, the principal lookup is delegated to
    // OAuthProviderService, which uses the OAuth2AuthenticationToken's
    // registration id to pick the right provider column (googleId, discordId,
    // microsoftId). Guests carry a `guest:<internalId>` name and are handled
    // separately in resolveAnyAuthenticatedUser.
    public Optional<User> resolveRegisteredUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return Optional.empty();
        boolean isRegistered = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        if (!isRegistered)
            return Optional.empty();
        return oAuthProviderService.findByOAuthAuthentication(authentication);
    }

    public Optional<User> resolveAnyAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return Optional.empty();
        boolean hasRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")
                        || a.getAuthority().equals("ROLE_USER"));
        if (!hasRole)
            return Optional.empty();
        String name = authentication.getName();
        if (name != null && name.startsWith("guest:"))
            return userRepository.findById(name.substring(6));
        return oAuthProviderService.findByOAuthAuthentication(authentication);
    }

    /**
     * Applies end-of-game stat changes to a registered user.
     * Called by InteractiveSessionService after each game finishes; guests are
     * excluded
     * because their accounts are ephemeral and not tracked on the leaderboard.
     */
    public void updateStatsAfterGame(String userId, int finalScore, boolean won) {
        userRepository.findById(userId).ifPresent(user -> {
            var stats = user.getStats();
            stats.setGamesPlayed(stats.getGamesPlayed() + 1);
            stats.setTotalPoints(stats.getTotalPoints() + finalScore);
            if (finalScore > stats.getHighScore())
                stats.setHighScore(finalScore);
            stats.setDailyLoginStreak(won ? stats.getDailyLoginStreak() + 1 : 0);
            userRepository.save(user);
        });
    }
}
