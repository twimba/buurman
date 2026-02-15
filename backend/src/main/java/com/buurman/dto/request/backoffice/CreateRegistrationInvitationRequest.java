package com.buurman.dto.request.backoffice;

import java.time.Instant;

public record CreateRegistrationInvitationRequest(
    String code,
    Integer maxUsages,
    Instant expiresAt
) {}
