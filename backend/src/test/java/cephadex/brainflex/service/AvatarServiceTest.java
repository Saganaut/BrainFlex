/**
 * Tests for the static preset-pool accessors. Chunk 13.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class AvatarServiceTest {

    private final AvatarService service = new AvatarService();

    @Test
    void list_ReturnsAtLeastTwelvePresets() {
        // Chunk 13 calls for 12–20 presets. Smoke-check the lower bound so we
        // don't accidentally drop the pool below the floor the lobby UI was
        // designed against.
        assertTrue(service.list().size() >= 12);
    }

    @Test
    void list_AllKeysAreUnique() {
        Set<String> keys = service.list().stream()
                .map(AvatarService.AvatarPreset::key)
                .collect(Collectors.toSet());
        assertEquals(service.list().size(), keys.size());
    }

    @Test
    void has_TrueForKnownKey_FalseForUnknown() {
        AvatarService.AvatarPreset first = service.list().get(0);
        assertTrue(service.has(first.key()));
        assertFalse(service.has("not-a-real-preset"));
        assertFalse(service.has(null));
    }

    @Test
    void get_ReturnsTheRequestedPreset() {
        AvatarService.AvatarPreset first = service.list().get(0);
        AvatarService.AvatarPreset fetched = service.get(first.key());
        assertNotNull(fetched);
        assertEquals(first.key(), fetched.key());
        assertEquals(first.colorTag(), fetched.colorTag());
    }

    @Test
    void get_NullForUnknownKey() {
        assertNull(service.get("not-a-real-preset"));
    }
}
