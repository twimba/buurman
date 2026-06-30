package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CacheInfoResponse(
    String name,
    String type,
    String description,
    long maxSize,
    int ttlSeconds,
    String refreshPolicy,
    long entryCount,
    long hitCount,
    long missCount,
    double hitRate,
    long loadCount,
    double averageLoadTimeMs,
    long evictionCount,
    @Nullable String lastInvalidatedAt) {}
