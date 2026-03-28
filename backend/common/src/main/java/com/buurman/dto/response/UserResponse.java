package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UserResponse(
    Sid identifier,
    Sid teamIdentifier,
    String email,
    String firstName,
    String lastName,
    String role,
    boolean emailVerified,
    Instant createdAt) {}
