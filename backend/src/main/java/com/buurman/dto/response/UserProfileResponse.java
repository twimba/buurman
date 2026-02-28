package com.buurman.dto.response;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated user profile with contact and verification details")
public record UserProfileResponse(
    @Schema(description = "Unique user identifier", example = "usr_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    String email,
    String firstName,
    String lastName,
    Optional<String> phone,
    boolean phoneVerified) {}
