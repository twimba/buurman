package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record PropertyOutdoorAreaResponse(
    Ulid identifier,
    String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
