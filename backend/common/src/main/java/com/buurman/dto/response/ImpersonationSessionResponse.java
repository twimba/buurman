package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ImpersonationSessionResponse(
    Sid identifier,
    String adminEmail,
    String adminName,
    Sid targetUserIdentifier,
    String targetUserEmail,
    Sid targetTeamIdentifier,
    String mode,
    String reason,
    String status,
    Instant createdAt,
    Optional<Instant> activatedAt,
    Instant expiresAt,
    Optional<Instant> endedAt,
    Optional<String> endReason) {}
