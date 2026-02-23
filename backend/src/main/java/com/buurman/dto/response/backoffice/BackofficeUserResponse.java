package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

public record BackofficeUserResponse(
    String identifier,
    String email,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> phone,
    boolean emailVerified,
    boolean disabled,
    boolean online,
    long teamCount,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
