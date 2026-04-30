package cephadex.brainflex.controller;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.HealthCheckResponse;

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
    public ResponseEntity<HealthCheckResponse> getHealth() {
        String overallStatus = "UP";
        String dbStatus = "DISCONNECTED";
        String redisStatus = "DISCONNECTED";

        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            dbStatus = "CONNECTED";
        } catch (Exception e) {
            overallStatus = "DEGRADED";
        }
        
        try {
            String ping = redisConnectionFactory.getConnection().ping();
            redisStatus = "PONG".equals(ping) ? "CONNECTED" : "DISCONNECTED";
        } catch (Exception e) {
            overallStatus = "DEGRADED";
        }
        return ResponseEntity.ok(new HealthCheckResponse(
            overallStatus,
            "BrainFlex API is running",
            LocalDateTime.now(),
            dbStatus,
            redisStatus));

    }
}
