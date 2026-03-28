package com.buurman.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.RateLimitConfig;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.RateLimitConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitConfigService {

  private final RateLimitConfigRepository configRepository;
  private final MetricsService metricsService;

  private volatile @Nullable Map<String, RateLimitConfig> cachedConfigs;

  public Optional<RateLimitConfig> getConfig(String key) {
    Map<String, RateLimitConfig> configs = loadConfigs();
    return Optional.ofNullable(configs.get(key));
  }

  public List<RateLimitConfig> getAllConfigs() {
    return List.copyOf(loadConfigs().values());
  }

  @Transactional
  public RateLimitConfig updateConfig(
      String key, int maxRequests, int periodSeconds, boolean enabled, String updatedBy) {
    RateLimitConfig config =
        configRepository
            .findByKey(key)
            .orElseThrow(() -> new NotFoundException("Rate limit config not found: " + key));

    config.setMaxRequests(maxRequests);
    config.setPeriodSeconds(periodSeconds);
    config.setEnabled(enabled);
    config.setUpdatedBy(Optional.of(updatedBy));

    RateLimitConfig saved = configRepository.save(config);
    cachedConfigs = null;
    metricsService.incrementCounter("ratelimit.config.reload.total");
    log.info(
        "Rate limit config '{}' updated by {} — maxRequests={}, periodSeconds={}, enabled={}",
        key,
        updatedBy,
        maxRequests,
        periodSeconds,
        enabled);
    return saved;
  }

  private Map<String, RateLimitConfig> loadConfigs() {
    Map<String, RateLimitConfig> configs = cachedConfigs;
    if (configs != null) {
      return configs;
    }
    configs =
        configRepository.findAll().stream()
            .collect(Collectors.toMap(RateLimitConfig::getKey, Function.identity()));
    cachedConfigs = configs;
    return configs;
  }
}
