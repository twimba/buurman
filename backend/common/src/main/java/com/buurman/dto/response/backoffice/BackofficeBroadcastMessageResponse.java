package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record BackofficeBroadcastMessageResponse(
    Sid identifier,
    String title,
    String body,
    String severity,
    String scope,
    Instant startAt,
    Optional<Instant> endAt,
    boolean showOnLogin,
    boolean showOnRegister,
    boolean showInApp,
    List<String> targetTeamIdentifiers,
    List<String> targetUserIdentifiers,
    Instant createdAt,
    Instant updatedAt) {}
