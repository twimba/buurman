package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String identifier,
        UUID teamId,
        String name,
        String email,
        String phone,
        String taxNumber,
        String idNumber,
        String additionalInfo,
        String mainPhotoUrl,
        PropertySummary currentProperty,
        Instant createdAt,
        Instant updatedAt
) {}
