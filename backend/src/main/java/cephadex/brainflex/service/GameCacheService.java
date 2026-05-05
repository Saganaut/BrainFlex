/**
 * Redis cache for active GameSession state.
 * Stores each session as JSON keyed by "game:{roomCode}" with a 2-hour TTL.
 * During an active game, GameService reads/writes here instead of MongoDB on every
 * answer submission — MongoDB is only written at round boundaries and game end.
 *
 * All operations are wrapped in try-catch so a Redis outage degrades gracefully:
 * GameService falls back to MongoDB reads automatically when the cache misses.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.model.GameSession;

@Service
public class GameCacheService {

    private static final Logger log = LoggerFactory.getLogger(GameCacheService.class);
    private static final String KEY_PREFIX = "game:";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GameCacheService(StringRedisTemplate redis, @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Serialize and store the session; refreshes the TTL on every write. */
    public void put(GameSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redis.opsForValue().set(KEY_PREFIX + session.getRoomCode(), json, TTL);
        } catch (Exception e) {
            log.warn("Redis write failed for room {}: {}", session.getRoomCode(), e.getMessage());
        }
    }

    /** Return the cached session, or empty if not cached or Redis is unavailable. */
    public Optional<GameSession> get(String roomCode) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + roomCode);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, GameSession.class));
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
}
