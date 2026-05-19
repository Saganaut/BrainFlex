/**
 * Curates the preset avatar pool players choose from in the showcase lobby.
 *
 * Chunk 13 — Kahoot-style anonymous avatars layered on top of the player's
 * real {@code pictureUrl}. The lobby picker reads {@link #list()}; the
 * showcase service validates incoming {@code avatarKey} values against
 * {@link #has(String)} when a player commits a selection so a client can't
 * post an arbitrary key.
 *
 * The pool is static for now (compile-time constants). When the chunk-19
 * media library lands, this can be replaced with a MongoDB-backed collection
 * + admin CRUD without changing the consumer interface.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class AvatarService {

    /**
     * One preset choice. {@code imageUrl} points at a static asset under the
     * frontend's {@code /assets/images/avatars/} tree — keys match filenames
     * (kebab-case animal + color) so the frontend can render without a
     * server round-trip on every refresh.
     *
     * {@code colorTag} is a design-token name (matches one of the palette
     * tokens in {@code tokens.css}) so the lobby tile + leaderboard chip
     * can both pick up the player's accent color from a single source.
     */
    public record AvatarPreset(String key, String displayName, String imageUrl, String colorTag) {}

    private static final String CDN_PREFIX = "/assets/images/avatars/";

    private static final List<AvatarPreset> PRESETS = List.of(
            new AvatarPreset("fox-orange", "Fox", CDN_PREFIX + "fox-orange.svg", "orange"),
            new AvatarPreset("owl-violet", "Owl", CDN_PREFIX + "owl-violet.svg", "violet"),
            new AvatarPreset("shark-blue", "Shark", CDN_PREFIX + "shark-blue.svg", "blue"),
            new AvatarPreset("panda-pink", "Panda", CDN_PREFIX + "panda-pink.svg", "pink"),
            new AvatarPreset("dragon-green", "Dragon", CDN_PREFIX + "dragon-green.svg", "green"),
            new AvatarPreset("phoenix-yellow", "Phoenix", CDN_PREFIX + "phoenix-yellow.svg", "yellow"),
            new AvatarPreset("wolf-teal", "Wolf", CDN_PREFIX + "wolf-teal.svg", "teal"),
            new AvatarPreset("tiger-red", "Tiger", CDN_PREFIX + "tiger-red.svg", "red"),
            new AvatarPreset("bear-brown", "Bear", CDN_PREFIX + "bear-brown.svg", "brown"),
            new AvatarPreset("rabbit-cream", "Rabbit", CDN_PREFIX + "rabbit-cream.svg", "cream"),
            new AvatarPreset("octopus-purple", "Octopus", CDN_PREFIX + "octopus-purple.svg", "purple"),
            new AvatarPreset("eagle-slate", "Eagle", CDN_PREFIX + "eagle-slate.svg", "slate"),
            new AvatarPreset("whale-navy", "Whale", CDN_PREFIX + "whale-navy.svg", "navy"),
            new AvatarPreset("cat-amber", "Cat", CDN_PREFIX + "cat-amber.svg", "amber"),
            new AvatarPreset("frog-lime", "Frog", CDN_PREFIX + "frog-lime.svg", "lime"),
            new AvatarPreset("penguin-ice", "Penguin", CDN_PREFIX + "penguin-ice.svg", "ice"));

    private static final Map<String, AvatarPreset> INDEX = PRESETS.stream()
            .collect(Collectors.toUnmodifiableMap(AvatarPreset::key, p -> p));

    public List<AvatarPreset> list() {
        return PRESETS;
    }

    public boolean has(String key) {
        return key != null && INDEX.containsKey(key);
    }

    public AvatarPreset get(String key) {
        return INDEX.get(key);
    }
}
