package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyOutdoorAreaResponse(
    String identifier,
    String type,
    BigDecimal areaValue,
    String areaUnit,
    Instant createdAt,
    Instant updatedAt) {}
