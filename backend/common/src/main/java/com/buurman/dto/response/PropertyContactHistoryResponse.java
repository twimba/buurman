package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.PropertyContactHistory;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertyContactHistoryResponse(
    PropertySummary property,
    Optional<Instant> movedInAt,
    Optional<Instant> movedOutAt,
    PropertyContactHistory.ActionType actionType,
    Optional<String> performedBy,
    Instant performedAt) {}
