package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationInvitation {
  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String code;
  @Builder.Default private Optional<Integer> maxUsages = Optional.empty();
  private int usageCount;
  @Builder.Default private Optional<Instant> expiresAt = Optional.empty();
  @Builder.Default private Optional<Instant> revokedAt = Optional.empty();
  @Builder.Default private Optional<String> revokedBy = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private String createdBy;
  @Builder.Default private Optional<String> note = Optional.empty();

  public boolean isValid() {
    if (revokedAt.isPresent()) {
      return false;
    }
    if (expiresAt.isPresent() && expiresAt.get().isBefore(Instant.now())) {
      return false;
    }

    return maxUsages.isEmpty() || usageCount < maxUsages.get();
  }

  public String getStatus() {
    if (revokedAt.isPresent()) {
      return "REVOKED";
    }
    if (expiresAt.isPresent() && expiresAt.get().isBefore(Instant.now())) {
      return "EXPIRED";
    }
    if (maxUsages.isPresent() && usageCount >= maxUsages.get()) {
      return "EXHAUSTED";
    }
    return "ACTIVE";
  }
}
