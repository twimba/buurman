package com.buurman.dto.response.backoffice;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RateLimitBucketSummaryResponse(
    List<RateLimitConfigSummary> configs, long totalActiveBuckets) {}
