package com.buurman.dto.response.backoffice;

import java.time.Instant;

public record BackofficeUserResponse(
    String identifier,
    String email,
    String firstName,
    String lastName,
    String phone,
    boolean emailVerified,
    boolean disabled,
    long teamCount,
    Instant createdAt,
    Instant updatedAt) {}
