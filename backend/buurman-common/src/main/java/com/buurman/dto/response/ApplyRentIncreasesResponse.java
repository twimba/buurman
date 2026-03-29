package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ApplyRentIncreasesResponse(
    List<RentIncreaseResult> results, RentIncreaseSummary summary) {}
