package com.buurman.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.FeatureFlagState;
import com.buurman.generated.api.FeatureFlagsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FeatureFlagController implements FeatureFlagsApi {

  private final FeatureFlagService featureFlagService;

  @Override
  public Map<String, FeatureFlagState> getFeatureFlags() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return featureFlagService.getAllFlags(principal);
  }
}
