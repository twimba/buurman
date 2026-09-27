package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UnitSummaryResponse(
    Sid identifier,
    String unitNumber,
    Optional<String> name,
    UnitType unitType,
    UnitStatus status) {}
