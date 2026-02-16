package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.PropertyTenantHistory;

public record PropertyTenantHistoryResponse(
    PropertySummary property,
    Instant movedInAt,
    Instant movedOutAt,
    PropertyTenantHistory.ActionType actionType,
    String performedBy,
    Instant performedAt) {}
