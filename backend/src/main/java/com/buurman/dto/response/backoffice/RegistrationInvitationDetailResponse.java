package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record RegistrationInvitationDetailResponse(
    String identifier,
    String code,
    @Nullable Integer maxUsages,
    int usageCount,
    @Nullable Instant expiresAt,
    boolean revoked,
    @Nullable String revokedBy,
    @Nullable Instant revokedAt,
    String status,
    String createdBy,
    Instant createdAt,
    @Nullable Instant updatedAt,
    @Nullable String note,
    List<UsageRecord> usages) {
  public record UsageRecord(String userEmail, @Nullable String userName, Instant usedAt) {}
}
