package com.buurman.controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FeatureFlagController {

  private final FeatureFlagService featureFlagService;

  @GetMapping("/feature-flags")
  public ResponseEntity<Map<String, Object>> getFeatureFlags(
      @AuthenticationPrincipal UserPrincipal principal) {
    Map<String, Object> flags = featureFlagService.getAllFlags(principal);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
        .body(flags);
  }
}
