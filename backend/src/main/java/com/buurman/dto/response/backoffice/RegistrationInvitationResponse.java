package com.buurman.dto.response.backoffice;

import java.time.Instant;

public record RegistrationInvitationResponse(
    String identifier,
    String code,
    Integer maxUsages,
    int usageCount,
    Instant expiresAt,
    boolean revoked,
    String status,
    String createdBy,
    Instant createdAt) {}
