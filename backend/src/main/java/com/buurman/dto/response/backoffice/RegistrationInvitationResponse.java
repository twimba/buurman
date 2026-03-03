package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record RegistrationInvitationResponse(
    Ulid identifier,
    String code,
    Optional<Integer> maxUsages,
    int usageCount,
    Optional<Instant> expiresAt,
    boolean revoked,
    String status,
    String createdBy,
    Instant createdAt,
    boolean hasNote) {}
