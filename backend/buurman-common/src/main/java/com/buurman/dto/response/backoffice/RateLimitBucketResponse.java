package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RateLimitBucketResponse(
    String bucketId,
    String configKey,
    String clientIdentifier,
    @Nullable String expiresAt,
    @Nullable Long availableTokens,
    @Nullable Long maxTokens,
    @Nullable Integer refillPeriodSeconds,
    @Nullable Boolean configEnabled,
    @Nullable String configDescription) {}
