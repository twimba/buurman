package com.buurman.dto.response.backoffice;

import java.time.Instant;

public record BuurmyResponse(
    String id,
    String username,
    String email,
    String firstName,
    String lastName,
    boolean enabled,
    boolean emailVerified,
    Instant createdAt,
    Instant lastLogin
) {}
