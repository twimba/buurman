package com.buurman.service.backoffice;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooq.Record;
import org.springframework.stereotype.Service;

import com.buurman.domain.RateLimitConfig;
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

  public Map<String, Object> listBuckets(
      Optional<String> configKey, Optional<String> clientIp, int page, int size) {
    List<BucketRow> rows = bucketRepository.findActive(configKey, clientIp, page, size);
    long total = bucketRepository.countActive(configKey, clientIp);

    List<Map<String, Object>> content = rows.stream().map(this::toBucketResponse).toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("content", content);
    result.put("page", page);
    result.put("size", size);
    result.put("totalElements", total);
    return result;
  }

  public Map<String, Object> getBucket(String bucketId) {
    if (!bucketRepository.exists(bucketId)) {
      throw new NotFoundException("Bucket not found: " + bucketId);
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("bucketId", bucketId);
    String[] parts = bucketId.split(":", 2);
    response.put("configKey", parts[0]);
    response.put("clientIdentifier", parts.length > 1 ? parts[1] : "");

    enrichWithTokenInfo(response, bucketId, parts[0]);

    configService
        .getConfig(parts[0])
        .ifPresent(
            config -> {
              response.put("configEnabled", config.isEnabled());
              response.put("configDescription", config.getDescription().orElse(null));
            });

    return response;
  }

  public void deleteBucket(String bucketId, String actorEmail) {
    if (!bucketRepository.delete(bucketId)) {
      throw new NotFoundException("Bucket not found: " + bucketId);
    }
    log.info("RATE_LIMIT_BUCKET_RESET bucketId={} by={}", bucketId, actorEmail);
  }

  public Map<String, Object> deleteBucketsByConfigKey(String configKey, String actorEmail) {
    int deleted = bucketRepository.deleteByConfigKey(configKey);
    log.info(
        "RATE_LIMIT_BUCKET_BULK_RESET configKey={} deletedCount={} by={}",
        configKey,
        deleted,
        actorEmail);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deletedCount", deleted);
    return result;
  }

  public Map<String, Object> getSummary() {
    List<Record> countsByKey = bucketRepository.countByConfigKey();
    List<RateLimitConfig> configs = configService.getAllConfigs();

    long totalActive = 0;
    List<Map<String, Object>> configSummaries = new ArrayList<>();

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
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("configKey", config.getKey());
      entry.put("displayName", config.getDisplayName());
      entry.put("enabled", config.isEnabled());
      entry.put("maxRequests", config.getMaxRequests());
      entry.put("periodSeconds", config.getPeriodSeconds());
      entry.put("activeBuckets", dbCounts.getOrDefault(config.getKey(), 0L));
      configSummaries.add(entry);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("configs", configSummaries);
    result.put("totalActiveBuckets", totalActive);
    return result;
  }

  private Map<String, Object> toBucketResponse(BucketRow row) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("bucketId", row.id());

    String[] parts = row.id().split(":", 2);
    response.put("configKey", parts[0]);
    response.put("clientIdentifier", parts.length > 1 ? parts[1] : "");
    response.put("expiresAt", Instant.ofEpochMilli(row.expiresAt()).toString());

    enrichWithTokenInfo(response, row.id(), parts[0]);

    return response;
  }

  private void enrichWithTokenInfo(
      Map<String, Object> response, String bucketId, String configKey) {
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
        long available = bucket.getAvailableTokens();
        response.put("availableTokens", available);
        response.put("maxTokens", config.getMaxRequests());
        response.put("refillPeriodSeconds", config.getPeriodSeconds());
      }
    } catch (Exception e) {
      log.debug("Could not retrieve token info for bucket {}: {}", bucketId, e.getMessage());
      response.put("availableTokens", null);
      response.put("maxTokens", null);
      response.put("refillPeriodSeconds", null);
    }
  }
}
