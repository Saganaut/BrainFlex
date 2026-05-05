package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.RegisterRequest;
import cephadex.brainflex.dto.UpdateProfileRequest;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(OAuth2User oAuth2User, RegisterRequest request) {
        String googleId = oAuth2User.getAttribute("sub");

        var existingByGoogleId = userRepository.findByGoogleId(googleId);
        if (existingByGoogleId.isPresent()) {
            User existing = existingByGoogleId.get();
            if (!Boolean.TRUE.equals(existing.getIsClosed()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already registered");

            // Reopen a closed account — username check excludes the user's own record
            var usernameTaken = userRepository.findByUserName(request.username());
            if (usernameTaken.isPresent() && !usernameTaken.get().getId().equals(existing.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

            existing.setIsClosed(false);
            existing.setClosedAt(null);
            existing.setUserName(request.username());
            existing.setNewsletter(request.newsletter());
            existing.setPictureUrl(oAuth2User.getAttribute("picture"));
            existing.setName(oAuth2User.getAttribute("name"));
            existing.setLastLogin(LocalDateTime.now());
            return userRepository.save(existing);
        }

        if (userRepository.findByUserName(request.username()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(oAuth2User.getAttribute("email"));
        user.setName(oAuth2User.getAttribute("name"));
        user.setPictureUrl(oAuth2User.getAttribute("picture"));
        user.setUserName(request.username());
        user.setIsGuest(false);
        user.setNewsletter(request.newsletter());
        user.setLastLogin(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User createGuest(String username) {
        if (userRepository.findByUserName(username).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

        User user = new User();
        user.setUserName(username);
        user.setIsGuest(true);
        user.setLastLogin(LocalDateTime.now());

        return userRepository.save(user);
    }

    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUserName(username).isEmpty();
    }

    public User updateProfile(User user, UpdateProfileRequest request) {
        if (request.pictureUrl() != null && !request.pictureUrl().isBlank()) {
            user.setPictureUrl(request.pictureUrl());
        }
        if (request.newsletter() != null) {
            user.setNewsletter(request.newsletter());
        }
        return userRepository.save(user);
    }

    public void closeAccount(User user) {
        user.setIsClosed(true);
        user.setClosedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public Optional<User> resolveRegisteredUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return Optional.empty();
        boolean isRegistered = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        if (!isRegistered) return Optional.empty();
        return userRepository.findByGoogleId(authentication.getName());
    }

    public Optional<User> resolveAnyAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return Optional.empty();
        boolean hasRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")
                        || a.getAuthority().equals("ROLE_USER"));
        if (!hasRole) return Optional.empty();
        String name = authentication.getName();
        if (name.startsWith("guest:")) return userRepository.findById(name.substring(6));
        return userRepository.findByGoogleId(name);
    }

    /**
     * Applies end-of-game stat changes to a registered user.
     * Called by GameService after each game finishes; guests are excluded
     * because their accounts are ephemeral and not tracked on the leaderboard.
     */
    public void updateStatsAfterGame(String userId, int finalScore, boolean won) {
        userRepository.findById(userId).ifPresent(user -> {
            var stats = user.getStats();
            stats.setGamesPlayed(stats.getGamesPlayed() + 1);
            stats.setTotalPoints(stats.getTotalPoints() + finalScore);
            if (finalScore > stats.getHighScore()) stats.setHighScore(finalScore);
            stats.setCurrentStreak(won ? stats.getCurrentStreak() + 1 : 0);
            userRepository.save(user);
        });
    }
}
