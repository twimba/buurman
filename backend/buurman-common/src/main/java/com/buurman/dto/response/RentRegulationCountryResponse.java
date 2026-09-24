package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RentRegulationCountryResponse(
    Sid identifier,
    String countryCode,
    String countryName,
    boolean hasRegionalRegulations,
    Optional<String> summary,
    Optional<Instant> lastReviewedAt,
    boolean stale,
    LateFeePolicy lateFeePolicy,
    Optional<BigDecimal> lateFeeMaxPercentage,
    Optional<String> lateFeeNotes) {}
