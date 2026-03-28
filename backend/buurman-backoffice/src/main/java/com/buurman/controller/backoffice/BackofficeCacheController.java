package com.buurman.controller.backoffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

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
  public List<Map<String, Object>> listCaches() {
    List<Map<String, Object>> caches = new ArrayList<>();
    caches.add(featureFlagService.getFlagDefsCacheInfo());
    caches.add(featureFlagService.getOverrideCacheInfo());
    caches.add(segmentEvaluator.getCacheInfo());
    return caches;
  }

  @Override
  public Map<String, Object> getCacheDetail(String cacheName) {
    Map<String, Object> info = findCacheInfo(cacheName);
    info.put("entries", findCacheEntries(cacheName));
    return info;
  }

  @Override
  public void invalidateCache(String cacheName) {
    switch (cacheName) {
      case "flag_definitions" -> featureFlagService.invalidateCaches();
      case "overrides" -> featureFlagService.invalidateCaches();
      case "segments" -> segmentEvaluator.invalidateCache();
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    }
  }

  private Map<String, Object> findCacheInfo(String cacheName) {
    return switch (cacheName) {
      case "flag_definitions" -> new LinkedHashMap<>(featureFlagService.getFlagDefsCacheInfo());
      case "overrides" -> new LinkedHashMap<>(featureFlagService.getOverrideCacheInfo());
      case "segments" -> new LinkedHashMap<>(segmentEvaluator.getCacheInfo());
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    };
  }

  private List<Map<String, Object>> findCacheEntries(String cacheName) {
    return switch (cacheName) {
      case "flag_definitions" -> featureFlagService.getFlagDefsCacheEntries();
      case "overrides" -> featureFlagService.getOverrideCacheEntries();
      case "segments" -> segmentEvaluator.getCacheEntries();
      default -> throw new NotFoundException("Cache '%s' not found".formatted(cacheName));
    };
  }
}
