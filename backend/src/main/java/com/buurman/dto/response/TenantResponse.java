package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record TenantResponse(
    String identifier,
    String firstName,
    String lastName,
    @Nullable String email,
    @Nullable String phone,
    @Nullable String taxNumber,
    @Nullable String idNumber,
    @Nullable String additionalInfo,
    @Nullable String mainPhotoUrl,
    @Nullable String mainPhotoThumbnailUrl,
    List<TenantPropertyAssignment> activeProperties,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
