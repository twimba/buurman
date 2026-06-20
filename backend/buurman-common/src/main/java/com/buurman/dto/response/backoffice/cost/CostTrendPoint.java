package com.buurman.dto.response.backoffice.cost;

import com.buurman.util.SkipTestCoverage;

/** Total monthly cost (EUR minor units) for one month, for the trend chart. */
@SkipTestCoverage
public record CostTrendPoint(String month, long totalEurMinor) {}
