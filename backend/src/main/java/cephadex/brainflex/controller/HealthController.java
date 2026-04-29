package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // @Autowired
    private final MongoTemplate mongoTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(MongoTemplate mongoTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.mongoTemplate = mongoTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }


    @GetMapping("/api/health")
    public Map<String, Object> getHealth() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("message", "BrainFlex API is running");
        response.put("timestamp", LocalDateTime.now());

        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            response.put("database", "CONNECTED");
        } catch (Exception e) {
            response.put("status", "DEGRADED");
            response.put("database", "DISCONNECTED");
            response.put("error", e.getMessage());
        }


        try {
            String redisStatus = redisConnectionFactory.getConnection().ping();
            response.put("redis", redisStatus);
        } catch (Exception e) {
            response.put("redis", "DISCONNECTED");
            response.put("status", "DEGRADED");
            response.put("error", e.getMessage());

        }

        return response;
    }
}
