/**
 * Per-player sliding-window rate limiter for audience-engagement actions in an
 * active InteractiveSession. Stores recent timestamps in a Redis ZSET keyed by
 * {@code rl:{kind}:{roomCode}:{userId}}: every action ZADDs the current
 * millisecond, ZREMRANGEBYSCORE prunes entries past the window, and ZCARD
 * gives the count for the limit check.
 *
 * Two action kinds today: {@link Kind#REACTION} (30 / minute) and
 * {@link Kind#CHAT} (20 / minute). Returns true when the action should be
 * accepted, false when it should be rejected as over the limit. Falls open on
 * Redis errors so a Redis outage degrades engagement (no rate limits) rather
 * than blocking participation entirely.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InteractiveSessionRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InteractiveSessionRateLimiter.class);

    public enum Kind {
        REACTION("reaction", 30),
        CHAT("chat", 20);

        final String label;
        final int maxPerMinute;

        Kind(String label, int maxPerMinute) {
            this.label = label;
            this.maxPerMinute = maxPerMinute;
        }
    }

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration KEY_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redis;

    public InteractiveSessionRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Records an action and returns true if it should be allowed. The
     * timestamp is appended unconditionally — over-limit attempts still count
     * toward the window so a flood doesn't roll back into the allowed range
     * the instant the window slides.
     */
    public boolean allow(Kind kind, String roomCode, String userId) {
        if (userId == null || roomCode == null) return true;
        String key = "rl:" + kind.label + ":" + roomCode + ":" + userId;
        long nowMs = System.currentTimeMillis();
        long windowStart = nowMs - WINDOW.toMillis();
        try {
            // Unique member per call so identical-millisecond bursts both count.
            redis.opsForZSet().add(key, UUID.randomUUID().toString() + ":" + nowMs, nowMs);
            redis.opsForZSet().removeRangeByScore(key, 0, windowStart);
            Long size = redis.opsForZSet().zCard(key);
            redis.expire(key, KEY_TTL);
            return size != null && size <= kind.maxPerMinute;
        } catch (Exception e) {
            log.warn("Rate limit check failed for {} {} {}: {}",
                    kind.label, roomCode, userId, e.getMessage());
            return true;
        }
    }
}
