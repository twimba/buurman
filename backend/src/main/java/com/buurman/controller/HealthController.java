package com.buurman.controller;

import java.time.Clock;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.config.models.AppProperties;
import com.buurman.dto.response.HealthResponse;
import com.buurman.dto.response.InfoResponse;
import com.buurman.generated.api.HealthApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HealthController implements HealthApi {

  private final AppProperties appProperties;
  private final Environment environment;
  private final Clock clock;

  @Override
  public HealthResponse health() {
    return new HealthResponse("UP", clock.instant());
  }

  @Override
  public InfoResponse info() {
    String activeProfiles = String.join(",", environment.getActiveProfiles());
    return new InfoResponse(
        appProperties.version(), activeProfiles.isEmpty() ? "default" : activeProfiles);
  }
}
