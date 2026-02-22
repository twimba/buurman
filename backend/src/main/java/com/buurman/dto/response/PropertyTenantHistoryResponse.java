package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.PropertyTenantHistory;

public record PropertyTenantHistoryResponse(
    PropertySummary property,
    @Nullable Instant movedInAt,
    @Nullable Instant movedOutAt,
    PropertyTenantHistory.ActionType actionType,
    @Nullable String performedBy,
    Instant performedAt) {}
