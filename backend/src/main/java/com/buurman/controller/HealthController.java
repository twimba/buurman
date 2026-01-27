package com.buurman.controller;

import com.buurman.dto.response.HealthResponse;
import com.buurman.dto.response.InfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Health and info endpoints")
public class HealthController {

    @Value("${app.version}")
    private String version;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Operation(summary = "Health check", description = "Returns the health status of the application")
    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up();
    }

    @Operation(summary = "Application info", description = "Returns application version and environment info")
    @GetMapping("/info")
    public InfoResponse info() {
        return new InfoResponse(version, environment, Instant.now());
    }
}
