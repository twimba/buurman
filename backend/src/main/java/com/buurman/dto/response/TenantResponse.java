package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Tenant details including contact information and active property assignments")
public record TenantResponse(
    @Schema(description = "Unique tenant identifier", example = "tnt_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    String firstName,
    String lastName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<String> additionalInfo,
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,
    List<TenantPropertyAssignment> activeProperties,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
