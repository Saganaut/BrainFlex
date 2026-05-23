package cephadex.brainflex.dto.shared;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed health status of the API and its dependencies")
public record HealthCheckResponse(
        @Schema(example = "UP") String status,

        @Schema(example = "BrainFlex API is running") String message,

        @Schema(example = "2026-04-29T14:03:39") Instant timestamp,

        @Schema(example = "CONNECTED") String database,

        @Schema(example = "CONNECTED") String redis) {
}