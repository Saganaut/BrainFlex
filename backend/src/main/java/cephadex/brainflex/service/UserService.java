package cephadex.brainflex.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
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
}
