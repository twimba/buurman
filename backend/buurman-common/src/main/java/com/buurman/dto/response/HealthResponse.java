package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record HealthResponse(String status, Instant timestamp) {}
