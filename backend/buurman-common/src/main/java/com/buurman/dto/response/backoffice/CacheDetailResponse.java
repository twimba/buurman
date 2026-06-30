package com.buurman.dto.response.backoffice;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CacheDetailResponse(
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
    @Nullable String lastInvalidatedAt,
    List<CacheEntryResponse> entries) {

  /** Combine cache info with its entries into a detail view. */
  public static CacheDetailResponse of(CacheInfoResponse info, List<CacheEntryResponse> entries) {
    return new CacheDetailResponse(
        info.name(),
        info.type(),
        info.description(),
        info.maxSize(),
        info.ttlSeconds(),
        info.refreshPolicy(),
        info.entryCount(),
        info.hitCount(),
        info.missCount(),
        info.hitRate(),
        info.loadCount(),
        info.averageLoadTimeMs(),
        info.evictionCount(),
        info.lastInvalidatedAt(),
        entries);
  }
}
