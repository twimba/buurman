package com.buurman.dto.response;

import com.buurman.domain.PropertyTenantHistory;

import java.time.Instant;

public record PropertyTenantHistoryResponse(
        PropertySummary property,
        Instant movedInAt,
        Instant movedOutAt,
        PropertyTenantHistory.ActionType actionType,
        String performedBy,
        Instant performedAt
) {}
