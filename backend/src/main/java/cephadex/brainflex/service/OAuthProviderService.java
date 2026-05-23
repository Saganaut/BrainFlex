/**
 * Central knowledge of every OAuth2 / OIDC provider BrainFlex supports.
 *
 * The rest of the app talks about "registered users" and "principal names" in
 * the abstract; this service is the only place that knows that Google stores
 * its subject in the {@code sub} claim, Discord uses {@code id} plus an avatar
 * hash, and Microsoft (OIDC) emits {@code sub} but no picture URL. Every
 * provider id ends up in a dedicated column on {@link User}
 * ({@code googleId} / {@code discordId} / {@code microsoftId}) so a single
 * BrainFlex account is always tied to exactly one external identity.
 */
package cephadex.brainflex.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;

@Service
public class OAuthProviderService {

    public static final String GOOGLE = "google";
    public static final String DISCORD = "discord";
    public static final String MICROSOFT = "microsoft";

    private final UserRepository userRepository;

    public OAuthProviderService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the registered user behind an {@link OAuth2AuthenticationToken}.
     * Falls back to a cross-provider id scan for non-OAuth2 authentications so
     * 
     * @WithMockUser-style tests (which build a UsernamePasswordAuthenticationToken
     *                     with the principal name set to a provider id) keep
     *                     working.
     */
    public Optional<User> findByOAuthAuthentication(Authentication authentication) {
        if (authentication == null)
            return Optional.empty();
        if (authentication instanceof OAuth2AuthenticationToken oauth) {
            return findByProviderId(oauth.getAuthorizedClientRegistrationId(), oauth.getName());
        }
        return findByAnyProviderId(authentication.getName());
    }

    public Optional<User> findByProviderId(String provider, String providerId) {
        if (provider == null || providerId == null)
            return Optional.empty();
        return switch (provider) {
            case GOOGLE -> userRepository.findByGoogleId(providerId);
            case DISCORD -> userRepository.findByDiscordId(providerId);
            case MICROSOFT -> userRepository.findByMicrosoftId(providerId);
            default -> Optional.empty();
        };
    }

    /**
     * Tries every provider id column until one matches. Used where the caller
     * only has a bare principal name string (STOMP routing, WebSocket presence)
     * and can't ask the {@link OAuth2AuthenticationToken} which provider it came
     * from. The id formats are distinct enough in practice that collisions
     * across providers don't occur.
     */
    public Optional<User> findByAnyProviderId(String providerId) {
        if (providerId == null)
            return Optional.empty();
        var byGoogle = userRepository.findByGoogleId(providerId);
        if (byGoogle.isPresent())
            return byGoogle;
        var byDiscord = userRepository.findByDiscordId(providerId);
        if (byDiscord.isPresent())
            return byDiscord;
        return userRepository.findByMicrosoftId(providerId);
    }

    /**
     * Normalizes the OAuth2 / OIDC attributes a provider hands back into a
     * shape the rest of the auth flow can consume. Each provider exposes
     * identity under a different attribute name (Google + Microsoft use the
     * OIDC {@code sub} claim; Discord uses {@code id}), and only Google +
     * Discord supply a usable picture URL out of the box.
     */
    public ProviderProfile profileOf(OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User user = token.getPrincipal();
        return switch (provider) {
            case GOOGLE -> new ProviderProfile(
                    GOOGLE,
                    str(user, "sub"),
                    str(user, "email"),
                    str(user, "name"),
                    str(user, "picture"));
            case DISCORD -> {
                String id = str(user, "id");
                String avatarHash = str(user, "avatar");
                // Discord doesn't ship the avatar URL — clients build it from
                // the user id and the avatar hash. `null` hash means the user
                // is on Discord's default avatar; we leave picture blank and
                // let UserImageHydrator fall back to its placeholder.
                String picture = (avatarHash != null && id != null)
                        ? "https://cdn.discordapp.com/avatars/" + id + "/" + avatarHash + ".png"
                        : null;
                String name = firstNonBlank(str(user, "global_name"), str(user, "username"));
                yield new ProviderProfile(DISCORD, id, str(user, "email"), name, picture);
            }
            case MICROSOFT -> new ProviderProfile(
                    MICROSOFT,
                    str(user, "sub"),
                    // Microsoft OIDC only emits {@code email} for accounts that
                    // have one set; personal MSA logins commonly land in
                    // {@code preferred_username} instead. Fetching a picture
                    // requires a Graph API call which we skip for now.
                    firstNonBlank(str(user, "email"), str(user, "preferred_username")),
                    str(user, "name"),
                    null);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };
    }

    /**
     * Writes the provider id into the matching column on {@code user}. Single
     * source of truth for the provider→column mapping so adding a fourth
     * provider only touches this class.
     */
    public void setProviderIdOn(User user, ProviderProfile profile) {
        switch (profile.provider()) {
            case GOOGLE -> user.setGoogleId(profile.providerId());
            case DISCORD -> user.setDiscordId(profile.providerId());
            case MICROSOFT -> user.setMicrosoftId(profile.providerId());
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + profile.provider());
        }
    }

    /**
     * Returns the BrainFlex principal name used as the STOMP / Spring Security
     * routing key for {@code user}. Guests use {@code guest:<id>}; registered
     * users use whichever provider id column is populated. Returns null for
     * registered users whose provider id columns are all empty (defensive —
     * the broadcast paths skip per-user delivery on null).
     */
    public String principalNameFor(User user) {
        if (user.isGuest())
            return "guest:" + user.getId();
        if (user.getGoogleId() != null)
            return user.getGoogleId();
        if (user.getDiscordId() != null)
            return user.getDiscordId();
        return user.getMicrosoftId();
    }

    public record ProviderProfile(
            String provider,
            String providerId,
            String email,
            String name,
            String picture) {
    }

    private static String str(OAuth2User user, String key) {
        Object v = user.getAttribute(key);
        return v == null ? null : v.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank())
                return v;
        }
        return null;
    }
}
