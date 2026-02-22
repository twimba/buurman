package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class RegistrationInvitation {
  private UUID id;
  private String identifier;
  private String code;
  private @Nullable Integer maxUsages;
  private int usageCount;
  private @Nullable Instant expiresAt;
  private @Nullable Instant revokedAt;
  private @Nullable String revokedBy;
  private Instant createdAt;
  private Instant updatedAt;
  private String createdBy;
  private String note;

  public boolean isValid() {
    if (revokedAt != null) {
      return false;
    }
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      return false;
    }
    if (maxUsages != null && usageCount >= maxUsages) {
      return false;
    }
    return true;
  }

  public String getStatus() {
    if (revokedAt != null) {
      return "REVOKED";
    }
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      return "EXPIRED";
    }
    if (maxUsages != null && usageCount >= maxUsages) {
      return "EXHAUSTED";
    }
    return "ACTIVE";
  }
}
