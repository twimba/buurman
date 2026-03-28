package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RentRegulationRegionResponse(
    Sid identifier, String regionCode, String regionName, Optional<String> summary) {}
