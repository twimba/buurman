package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

public record RegistrationInvitationResponse(
    String identifier,
    String code,
    Optional<Integer> maxUsages,
    int usageCount,
    Optional<Instant> expiresAt,
    boolean revoked,
    String status,
    String createdBy,
    Instant createdAt,
    boolean hasNote) {}
