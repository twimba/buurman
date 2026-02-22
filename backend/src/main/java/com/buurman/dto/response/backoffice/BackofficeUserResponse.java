package com.buurman.dto.response.backoffice;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record BackofficeUserResponse(
    String identifier,
    String email,
    @Nullable String firstName,
    @Nullable String lastName,
    @Nullable String phone,
    boolean emailVerified,
    boolean disabled,
    boolean online,
    long teamCount,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
