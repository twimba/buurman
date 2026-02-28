package com.buurman.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Team summary with member count")
public record TeamResponse(
    @Schema(description = "Unique team identifier", example = "team_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    String teamName,
    long memberCount,
    Instant createdAt) {}
