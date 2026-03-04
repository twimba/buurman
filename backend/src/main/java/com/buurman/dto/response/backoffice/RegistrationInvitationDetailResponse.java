package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;

public record RegistrationInvitationDetailResponse(
    Sid identifier,
    String code,
    Optional<Integer> maxUsages,
    int usageCount,
    Optional<Instant> expiresAt,
    boolean revoked,
    Optional<String> revokedBy,
    Optional<Instant> revokedAt,
    String status,
    String createdBy,
    Instant createdAt,
    Optional<Instant> updatedAt,
    Optional<String> note,
    List<UsageRecord> usages) {
  public record UsageRecord(String userEmail, Optional<String> userName, Instant usedAt) {}
}
