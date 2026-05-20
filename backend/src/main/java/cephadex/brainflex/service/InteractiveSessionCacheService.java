/**
 * Redis cache for active InteractiveSession state.
 * Stores each session as JSON keyed by "game:{roomCode}" with a 2-hour TTL.
 * During an active game, InteractiveSessionService reads/writes here instead of MongoDB on every
 * answer submission — MongoDB is only written at round boundaries and game end.
 *
 * All operations are wrapped in try-catch so a Redis outage degrades gracefully:
 * InteractiveSessionService falls back to MongoDB reads automatically when the cache misses.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.model.InteractiveSession;

@Service
public class InteractiveSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(InteractiveSessionCacheService.class);
    private static final String KEY_PREFIX = "interactiveSession:";
    private static final String REACTION_PREFIX = "reactions:counts:";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public InteractiveSessionCacheService(StringRedisTemplate redis, @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Serialize and store the session; refreshes the TTL on every write. */
    public void put(InteractiveSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redis.opsForValue().set(KEY_PREFIX + session.getRoomCode(), json, TTL);
        } catch (Exception e) {
            log.warn("Redis write failed for room {}: {}", session.getRoomCode(), e.getMessage());
        }
    }

    /** Return the cached session, or empty if not cached or Redis is unavailable. */
    public Optional<InteractiveSession> get(String roomCode) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + roomCode);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, InteractiveSession.class));
        } catch (Exception e) {
            log.warn("Redis read failed for room {}: {}", roomCode, e.getMessage());
            return Optional.empty();
        }
    }

    /** Remove the session from the cache when the game ends or is cancelled. */
    public void evict(String roomCode) {
        try {
            redis.delete(KEY_PREFIX + roomCode);
        } catch (Exception e) {
            log.warn("Redis evict failed for room {}: {}", roomCode, e.getMessage());
        }
    }

    // ---- Reaction aggregation (chunk 11) ----
    //
    // Counts are kept in a Redis hash keyed by (roomCode, elementId). Live
    // hosts read these as the bar-chart overlay on the reveal screen; analytics
    // reads the durable Reaction collection. Falls open on Redis errors —
    // counts simply don't increment, the reaction still broadcasts.

    /** Atomically increment the per-emoji count for a round, returning the new value. */
    public long incrementReactionCount(String roomCode, String elementId, String emoji) {
        try {
            String key = REACTION_PREFIX + roomCode + ":" + elementId;
            Long count = redis.opsForHash().increment(key, emoji, 1);
            redis.expire(key, TTL);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("Reaction count increment failed for room {} elem {}: {}",
                    roomCode, elementId, e.getMessage());
            return 0L;
        }
    }

    /** Read all (emoji -> count) pairs collected for a round. Empty when Redis is unreachable. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Long> getReactionCounts(String roomCode, String elementId) {
        try {
            String key = REACTION_PREFIX + roomCode + ":" + elementId;
            Map entries = redis.opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) return Collections.emptyMap();
            Map<String, Long> out = new HashMap<>(entries.size());
            for (Object entry : entries.entrySet()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
                try {
                    out.put(String.valueOf(e.getKey()), Long.parseLong(String.valueOf(e.getValue())));
                } catch (NumberFormatException ignored) {
                    // Skip malformed entries rather than fail the whole read.
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("Reaction count read failed for room {} elem {}: {}",
                    roomCode, elementId, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
