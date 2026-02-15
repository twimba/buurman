package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;

public record RegistrationInvitationDetailResponse(
    String identifier,
    String code,
    Integer maxUsages,
    int usageCount,
    Instant expiresAt,
    boolean revoked,
    String revokedBy,
    Instant revokedAt,
    String status,
    String createdBy,
    Instant createdAt,
    Instant updatedAt,
    List<UsageRecord> usages
) {
    public record UsageRecord(
        String userEmail,
        String userName,
        Instant usedAt
    ) {}
}
