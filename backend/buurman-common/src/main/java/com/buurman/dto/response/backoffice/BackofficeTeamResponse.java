package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record BackofficeTeamResponse(
    Sid identifier,
    String teamName,
    boolean demo,
    long memberCount,
    long propertyCount,
    long unitCount,
    Optional<String> ownerEmail,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
