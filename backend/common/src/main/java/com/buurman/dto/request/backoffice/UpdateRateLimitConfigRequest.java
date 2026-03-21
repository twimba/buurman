package com.buurman.dto.request.backoffice;

import com.buurman.util.Generated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Generated
public record UpdateRateLimitConfigRequest(
    @Min(1) @Max(10000) int maxRequests, @Min(10) @Max(86400) int periodSeconds, boolean enabled) {}
