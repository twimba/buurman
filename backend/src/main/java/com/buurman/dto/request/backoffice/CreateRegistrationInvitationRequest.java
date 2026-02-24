package com.buurman.dto.request.backoffice;

import java.time.Instant;
import java.util.Optional;

public record CreateRegistrationInvitationRequest(
    Optional<String> code,
    Optional<Integer> maxUsages,
    Optional<Instant> expiresAt,
    Optional<String> note) {}
