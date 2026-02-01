package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID userId,
    String email,
    String firstName,
    String lastName,
    UUID teamId,
    String role,
    Instant createdAt
) {}
