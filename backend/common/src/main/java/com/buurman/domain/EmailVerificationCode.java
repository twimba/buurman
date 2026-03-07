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
public class EmailVerificationCode {

  private UUID id;
  private UUID userId;
  private String code;
  @Builder.Default private Optional<String> token = Optional.empty();
  private Instant expiresAt;
  @Builder.Default private Optional<Instant> usedAt = Optional.empty();
  private Instant createdAt;
}
