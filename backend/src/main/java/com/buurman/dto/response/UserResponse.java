package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Ulid;

public record UserResponse(
    Ulid identifier,
    Ulid teamIdentifier,
    String email,
    String firstName,
    String lastName,
    String role,
    boolean emailVerified,
    Instant createdAt) {}
