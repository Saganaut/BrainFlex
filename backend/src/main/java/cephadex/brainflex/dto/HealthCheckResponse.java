package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed health status of the API and its dependencies")
public record HealthCheckResponse(
    @Schema(example = "UP") 
    String status,
    
    @Schema(example = "BrainFlex API is running") 
    String message,
    
    @Schema(example = "2026-04-29T14:03:39") 
    LocalDateTime timestamp,
    
    @Schema(example = "CONNECTED") 
    String database,
    
    @Schema(example = "CONNECTED") 
    String redis
) {}