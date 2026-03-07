package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

public record RateLimitConfigResponse(
    String key,
    String displayName,
    Optional<String> description,
    int maxRequests,
    int periodSeconds,
    boolean enabled,
    Optional<Instant> updatedAt,
    Optional<String> updatedBy) {}
