package com.buurman.controller;

import java.time.Clock;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.config.models.AppProperties;
import com.buurman.dto.response.HealthResponse;
import com.buurman.dto.response.InfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("")
@Tag(name = "Health", description = "Health and info endpoints")
@RequiredArgsConstructor
public class HealthController {

  private final AppProperties appProperties;
  private final Environment environment;
  private final Clock clock;

  @Operation(summary = "Health check", description = "Returns the health status of the application")
  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("UP", clock.instant());
  }

  @Operation(
      summary = "Application info",
      description = "Returns application version and environment")
  @GetMapping("/info")
  public InfoResponse info() {
    String activeProfiles = String.join(",", environment.getActiveProfiles());
    return new InfoResponse(
        appProperties.version(), activeProfiles.isEmpty() ? "default" : activeProfiles);
  }
}
