package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.PropertyTenantHistory;

public record PropertyTenantHistoryResponse(
    PropertySummary property,
    Optional<Instant> movedInAt,
    Optional<Instant> movedOutAt,
    PropertyTenantHistory.ActionType actionType,
    Optional<String> performedBy,
    Instant performedAt) {}
