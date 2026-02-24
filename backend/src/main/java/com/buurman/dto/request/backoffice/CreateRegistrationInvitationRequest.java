package com.buurman.dto.request.backoffice;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record CreateRegistrationInvitationRequest(
    Optional<String> code,
    Optional<Integer> maxUsages,
    Optional<Instant> expiresAt,
    Optional<String> note) {
  public CreateRegistrationInvitationRequest {
    code = Objects.requireNonNullElse(code, Optional.empty());
    maxUsages = Objects.requireNonNullElse(maxUsages, Optional.empty());
    expiresAt = Objects.requireNonNullElse(expiresAt, Optional.empty());
    note = Objects.requireNonNullElse(note, Optional.empty());
  }
}
