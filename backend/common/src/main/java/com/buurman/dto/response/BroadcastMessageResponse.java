package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record BroadcastMessageResponse(
    Sid identifier,
    String title,
    String body,
    String severity,
    Instant startAt,
    Optional<Instant> endAt) {}
