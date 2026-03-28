package com.buurman.controller.backoffice;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeRateLimitBucketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/backoffice/rate-limits/buckets")
public class BackofficeRateLimitBucketController {

  private final BackofficeRateLimitBucketService bucketService;

  @GetMapping
  public Map<String, Object> listBuckets(
      @RequestParam Optional<String> configKey,
      @RequestParam Optional<String> clientIp,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return bucketService.listBuckets(configKey, clientIp, page, Math.min(size, 200));
  }

  @GetMapping("/summary")
  public Map<String, Object> getSummary() {
    return bucketService.getSummary();
  }

  @GetMapping("/{bucketId}")
  public Map<String, Object> getBucket(@PathVariable String bucketId) {
    return bucketService.getBucket(bucketId);
  }

  @DeleteMapping("/{bucketId}")
  public ResponseEntity<Void> deleteBucket(@PathVariable String bucketId) {
    String actor = SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
    bucketService.deleteBucket(bucketId, actor);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public Map<String, Object> deleteBucketsByConfigKey(@RequestParam String configKey) {
    String actor = SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
    return bucketService.deleteBucketsByConfigKey(configKey, actor);
  }
}
