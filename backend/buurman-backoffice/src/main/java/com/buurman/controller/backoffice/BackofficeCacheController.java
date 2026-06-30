package com.buurman.controller.backoffice;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.CacheDetailResponse;
import com.buurman.dto.response.backoffice.CacheEntryResponse;
import com.buurman.dto.response.backoffice.CacheInfoResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.generated.backoffice.api.BackofficeCachesApi;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.SegmentEvaluator;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeCacheController implements BackofficeCachesApi {

  private final FeatureFlagService featureFlagService;
  private final SegmentEvaluator segmentEvaluator;

  @Override
  public List<CacheInfoResponse> listCaches() {
    return List.of(
        featureFlagService.getFlagDefsCacheInfo(),
        featureFlagService.getOverrideCacheInfo(),
        segmentEvaluator.getCacheInfo());
  }

  @Override
  public CacheDetailResponse getCacheDetail(String cacheName) {
    return CacheDetailResponse.of(findCacheInfo(cacheName), findCacheEntries(cacheName));
  }

  @Override
  public void invalidateCache(String cacheName) {
    switch (cacheName) {
      case "flag_definitions", "overrides" -> featureFlagService.invalidateCaches();
      case "segments" -> segmentEvaluator.invalidateCache();
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    }
  }

  private CacheInfoResponse findCacheInfo(String cacheName) {
    return switch (cacheName) {
      case "flag_definitions" -> featureFlagService.getFlagDefsCacheInfo();
      case "overrides" -> featureFlagService.getOverrideCacheInfo();
      case "segments" -> segmentEvaluator.getCacheInfo();
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    };
  }

  private List<CacheEntryResponse> findCacheEntries(String cacheName) {
    return switch (cacheName) {
      case "flag_definitions" -> featureFlagService.getFlagDefsCacheEntries();
      case "overrides" -> featureFlagService.getOverrideCacheEntries();
      case "segments" -> segmentEvaluator.getCacheEntries();
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    };
  }
}
