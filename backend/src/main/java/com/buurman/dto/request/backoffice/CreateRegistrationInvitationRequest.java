package com.buurman.dto.request.backoffice;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record CreateRegistrationInvitationRequest(
    @Nullable String code, @Nullable Integer maxUsages, @Nullable Instant expiresAt, @Nullable String note) {}
