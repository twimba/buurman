package com.buurman.service.backoffice;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooq.Record;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.RateLimitConfig;
import com.buurman.dto.response.backoffice.RateLimitBucketDeleteResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketPageResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketSummaryResponse;
import com.buurman.dto.response.backoffice.RateLimitConfigSummary;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.backoffice.RateLimitBucketRepository;
import com.buurman.repository.backoffice.RateLimitBucketRepository.BucketRow;
import com.buurman.service.RateLimitConfigService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeRateLimitBucketService {

  private final RateLimitBucketRepository bucketRepository;
  private final RateLimitConfigService configService;
  private final ProxyManager<String> rateLimitProxyManager;

  public RateLimitBucketPageResponse listBuckets(
      Optional<String> configKey, Optional<String> clientIp, int page, int size) {
    List<BucketRow> rows = bucketRepository.findActive(configKey, clientIp, page, size);
    long total = bucketRepository.countActive(configKey, clientIp);

    List<RateLimitBucketResponse> content = rows.stream().map(this::toBucketResponse).toList();

    return new RateLimitBucketPageResponse(content, page, size, total);
  }

  public RateLimitBucketResponse getBucket(String bucketId) {
    if (!bucketRepository.exists(bucketId)) {
      throw new NotFoundException("Bucket not found: " + bucketId);
    }

    String[] parts = bucketId.split(":", 2);
    String configKey = parts[0];
    String clientIdentifier = parts.length > 1 ? parts[1] : "";

    TokenInfo tokenInfo = resolveTokenInfo(bucketId, configKey);

    Optional<RateLimitConfig> configOpt = configService.getConfig(configKey);
    Boolean configEnabled = configOpt.map(RateLimitConfig::isEnabled).orElse(null);
    String configDescription = configOpt.flatMap(RateLimitConfig::getDescription).orElse(null);

    return new RateLimitBucketResponse(
        bucketId,
        configKey,
        clientIdentifier,
        null,
        tokenInfo.availableTokens(),
        tokenInfo.maxTokens(),
        tokenInfo.refillPeriodSeconds(),
        configEnabled,
        configDescription);
  }

  public void deleteBucket(String bucketId, String actorEmail) {
    if (!bucketRepository.delete(bucketId)) {
      throw new NotFoundException("Bucket not found: " + bucketId);
    }
    log.info("RATE_LIMIT_BUCKET_RESET bucketId={} by={}", bucketId, actorEmail);
  }

  public RateLimitBucketDeleteResponse deleteBucketsByConfigKey(
      String configKey, String actorEmail) {
    int deleted = bucketRepository.deleteByConfigKey(configKey);
    log.info(
        "RATE_LIMIT_BUCKET_BULK_RESET configKey={} deletedCount={} by={}",
        configKey,
        deleted,
        actorEmail);
    return new RateLimitBucketDeleteResponse(deleted);
  }

  public RateLimitBucketSummaryResponse getSummary() {
    List<Record> countsByKey = bucketRepository.countByConfigKey();
    List<RateLimitConfig> configs = configService.getAllConfigs();

    long totalActive = 0;
    List<RateLimitConfigSummary> configSummaries = new ArrayList<>();

    Map<String, Long> dbCounts = new LinkedHashMap<>();
    for (Record r : countsByKey) {
      String key = r.get("config_key", String.class);
      Long count = r.get("bucket_count", Long.class);
      if (key != null && count != null) {
        dbCounts.put(key, count);
        totalActive += count;
      }
    }

    for (RateLimitConfig config : configs) {
      configSummaries.add(
          new RateLimitConfigSummary(
              config.getKey(),
              config.getDisplayName(),
              config.isEnabled(),
              config.getMaxRequests(),
              config.getPeriodSeconds(),
              dbCounts.getOrDefault(config.getKey(), 0L)));
    }

    return new RateLimitBucketSummaryResponse(configSummaries, totalActive);
  }

  private RateLimitBucketResponse toBucketResponse(BucketRow row) {
    String[] parts = row.id().split(":", 2);
    String configKey = parts[0];
    String clientIdentifier = parts.length > 1 ? parts[1] : "";
    String expiresAt = Instant.ofEpochMilli(row.expiresAt()).toString();

    TokenInfo tokenInfo = resolveTokenInfo(row.id(), configKey);

    return new RateLimitBucketResponse(
        row.id(),
        configKey,
        clientIdentifier,
        expiresAt,
        tokenInfo.availableTokens(),
        tokenInfo.maxTokens(),
        tokenInfo.refillPeriodSeconds(),
        null,
        null);
  }

  private record TokenInfo(
      @Nullable Long availableTokens,
      @Nullable Long maxTokens,
      @Nullable Integer refillPeriodSeconds) {}

  private TokenInfo resolveTokenInfo(String bucketId, String configKey) {
    try {
      Optional<RateLimitConfig> configOpt = configService.getConfig(configKey);
      if (configOpt.isPresent()) {
        RateLimitConfig config = configOpt.get();
        var bucket =
            rateLimitProxyManager.getProxy(
                bucketId,
                () ->
                    BucketConfiguration.builder()
                        .addLimit(
                            Bandwidth.builder()
                                .capacity(config.getMaxRequests())
                                .refillIntervally(
                                    config.getMaxRequests(),
                                    Duration.ofSeconds(config.getPeriodSeconds()))
                                .build())
                        .build());
        return new TokenInfo(
            bucket.getAvailableTokens(), (long) config.getMaxRequests(), config.getPeriodSeconds());
      }
    } catch (Exception e) {
      log.debug("Could not retrieve token info for bucket {}: {}", bucketId, e.getMessage());
    }
    return new TokenInfo(null, null, null);
  }
}
