package com.buurman.dto.response.backoffice;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record RegistrationInvitationResponse(
    String identifier,
    String code,
    @Nullable Integer maxUsages,
    int usageCount,
    @Nullable Instant expiresAt,
    boolean revoked,
    String status,
    String createdBy,
    Instant createdAt,
    boolean hasNote) {}
