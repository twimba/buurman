package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public record PropertyOutdoorAreaResponse(
    String identifier,
    String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
