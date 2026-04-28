package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, Object> getHealth() {

        return Map.of(
            "status", "UP",
            "message", "BrainFlex API is running",
            "timestamp", LocalDateTime.now()
    );
    }
}
