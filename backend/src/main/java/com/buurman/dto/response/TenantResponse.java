package com.buurman.dto.response;

import java.time.Instant;

public record TenantResponse(
    String identifier,
    String firstName,
    String lastName,
    String email,
    String phone,
    String taxNumber,
    String idNumber,
    String additionalInfo,
    String mainPhotoUrl,
    String mainPhotoThumbnailUrl,
    PropertySummary currentProperty,
    Instant createdAt,
    Instant updatedAt) {}
