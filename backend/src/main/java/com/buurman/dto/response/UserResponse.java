package com.buurman.dto.response;

import java.time.Instant;

public record UserResponse(
    String identifier,
    String teamIdentifier,
    String email,
    String firstName,
    String lastName,
    String role,
    boolean emailVerified,
    Instant createdAt
) {}
