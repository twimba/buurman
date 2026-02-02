package com.buurman.dto.response;

import com.buurman.domain.PropertyTenantHistory;

import java.time.Instant;
import java.util.UUID;

public record PropertyTenantHistoryResponse(
        UUID id,
        PropertySummary property,
        Instant movedInAt,
        Instant movedOutAt,
        PropertyTenantHistory.ActionType actionType,
        String performedBy,
        Instant performedAt
) {}
