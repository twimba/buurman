package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record BuurmyResponse(
    String id,
    String username,
    String email,
    @Nullable String firstName,
    @Nullable String lastName,
    boolean enabled,
    boolean emailVerified,
    Instant createdAt,
    @Nullable Instant lastLogin,
    List<String> requiredActions) {}
