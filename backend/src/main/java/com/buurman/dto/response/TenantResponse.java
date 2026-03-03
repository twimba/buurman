package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record TenantResponse(
    Ulid identifier,
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
