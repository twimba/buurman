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
public class SignatureSigner {

  private UUID id;
  private UUID signatureRequestId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  private String email;
  private SignatureSignerRole role;
  private String providerSignerId;
  private SignatureSignerStatus status;
  @Builder.Default private Optional<Instant> signedAt = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
}
