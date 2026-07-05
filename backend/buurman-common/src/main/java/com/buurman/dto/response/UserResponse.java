package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UserResponse(
    Sid identifier,
    @Nullable Sid teamIdentifier,
    String email,
    String firstName,
    String lastName,
    @Nullable String role,
    boolean emailVerified,
    Instant createdAt) {}
