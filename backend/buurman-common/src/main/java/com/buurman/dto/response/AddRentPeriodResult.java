package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/** Internal result from adding a rent period, wrapping the API response + adjustment info. */
@SkipTestCoverage
public record AddRentPeriodResult(
    RentPeriodResponse rentPeriodResponse, int adjustmentPaymentsCreated) {}
