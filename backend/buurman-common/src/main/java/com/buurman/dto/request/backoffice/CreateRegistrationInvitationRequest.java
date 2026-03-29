package com.buurman.dto.request.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CreateRegistrationInvitationRequest(
    Optional<String> code,
    Optional<Integer> maxUsages,
    Optional<Instant> expiresAt,
    Optional<String> note) {}
