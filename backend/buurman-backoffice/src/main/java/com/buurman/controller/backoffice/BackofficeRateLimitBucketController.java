package com.buurman.controller.backoffice;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.RateLimitBucketDeleteResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketPageResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketResponse;
import com.buurman.dto.response.backoffice.RateLimitBucketSummaryResponse;
import com.buurman.generated.backoffice.api.BackofficeRateLimitsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeRateLimitBucketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeRateLimitBucketController implements BackofficeRateLimitsApi {

  private static final int MAX_PAGE_SIZE = 200;

  private final BackofficeRateLimitBucketService bucketService;

  @Override
  public RateLimitBucketPageResponse listRateLimitBuckets(
      Optional<String> configKey,
      Optional<String> clientIp,
      Optional<Integer> page,
      Optional<Integer> size) {
    return bucketService.listBuckets(
        configKey, clientIp, page.orElse(0), Math.min(size.orElse(50), MAX_PAGE_SIZE));
  }

  @Override
  public RateLimitBucketSummaryResponse getRateLimitSummary() {
    return bucketService.getSummary();
  }

  @Override
  public RateLimitBucketResponse getRateLimitBucket(String bucketId) {
    return bucketService.getBucket(bucketId);
  }

  @Override
  public void deleteRateLimitBucket(String bucketId) {
    bucketService.deleteBucket(bucketId, actor());
  }

  @Override
  public RateLimitBucketDeleteResponse deleteRateLimitBucketsByConfigKey(String configKey) {
    return bucketService.deleteBucketsByConfigKey(configKey, actor());
  }

  private String actor() {
    return SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
  }
}
