package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RegistrationInvitationResponse(
    Sid identifier,
    String code,
    Optional<Integer> maxUsages,
    int usageCount,
    Optional<Instant> expiresAt,
    boolean revoked,
    String status,
    String createdBy,
    Instant createdAt,
    boolean hasNote) {}
