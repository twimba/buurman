package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record BackofficeUserDetailResponse(
    Sid identifier,
    String email,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> phone,
    boolean emailVerified,
    boolean disabled,
    boolean online,
    long teamCount,
    long demoTeamCount,
    Instant createdAt,
    Optional<Instant> updatedAt,
    List<UserTeamMembership> teams) {}
