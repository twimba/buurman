package com.buurman.dto.response.backoffice;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RateLimitConfigSummary(
    String configKey,
    String displayName,
    boolean enabled,
    int maxRequests,
    int periodSeconds,
    long activeBuckets) {}
