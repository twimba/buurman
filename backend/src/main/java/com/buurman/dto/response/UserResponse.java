package com.buurman.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Team member details including role and verification status")
public record UserResponse(
    @Schema(description = "Unique user identifier", example = "usr_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    String teamIdentifier,
    String email,
    String firstName,
    String lastName,
    String role,
    boolean emailVerified,
    Instant createdAt) {}
