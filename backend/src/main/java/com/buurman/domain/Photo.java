package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String entityType;
  private UUID entityId;
  private String fileKey;
  @Builder.Default private Optional<String> thumbnailFileKey = Optional.empty();
  private String fileName;
  private Long fileSize;
  private String mimeType;
  @Builder.Default private Optional<String> title = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private Boolean isMainPhoto = false;
  private UUID uploadedBy;
  private Instant uploadedAt;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();

  @Schema(description = "Type of entity the photo is attached to")
  public enum EntityType {
    PROPERTY,
    TENANT,
    CONTRACT,
    PAYMENT,
    EXPENSE
  }
}
