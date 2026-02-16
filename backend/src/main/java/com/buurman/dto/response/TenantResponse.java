package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;

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
    List<TenantPropertyAssignment> activeProperties,
    Instant createdAt,
    Instant updatedAt) {}
