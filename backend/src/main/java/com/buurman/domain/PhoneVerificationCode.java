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
public class PhoneVerificationCode {

  private UUID id;
  private UUID userId;
  private String phone;
  private String code;
  private Instant expiresAt;
  @Builder.Default private Optional<Instant> usedAt = Optional.empty();
  private Instant createdAt;
}
