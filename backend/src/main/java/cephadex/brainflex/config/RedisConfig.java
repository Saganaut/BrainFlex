/**
 * Redis configuration for game state caching.
 * Defines a dedicated ObjectMapper bean (separate from the global Spring MVC one)
 * so we can control exactly how GameSession is serialized to/from JSON in Redis
 * without affecting HTTP response serialization elsewhere.
 */
package cephadex.brainflex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    /**
     * ObjectMapper used exclusively by GameCacheService for Redis serialization.
     * Uses field-level access (not getters) so Lombok @Data classes with boolean
     * "isXxx" fields round-trip correctly without getter/setter name mismatches.
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // Field-level access avoids the Lombok isXxx/setXxx boolean naming quirk
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
}
