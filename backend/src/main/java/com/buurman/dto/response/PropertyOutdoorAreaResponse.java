package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record PropertyOutdoorAreaResponse(
    String identifier,
    String type,
    @Nullable BigDecimal areaValue,
    @Nullable String areaUnit,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
