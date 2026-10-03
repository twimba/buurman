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
public class Document {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private String entityType;
  private UUID entityId;
  private String fileKey;
  private String fileName;
  private Long fileSize;
  private String mimeType;
  @Builder.Default private Optional<String> title = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  // Points at the document this one was generated from (e.g. a signed copy or signing
  // certificate produced from the original PDF sent for signature). Empty for everything else.
  @Builder.Default private Optional<UUID> sourceDocumentId = Optional.empty();
  private UUID uploadedBy;
  private Instant uploadedAt;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();

  public enum EntityType {
    PROPERTY,
    TENANT,
    CONTRACT,
    PAYMENT,
    EXPENSE,
    FINANCING_PAYMENT
  }
}
