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
public class RateLimitConfig {

  private UUID id;
  private String key;
  private String displayName;
  @Builder.Default private Optional<String> description = Optional.empty();
  @Builder.Default private int maxRequests = 10;
  @Builder.Default private int periodSeconds = 60;
  @Builder.Default private boolean enabled = true;
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
