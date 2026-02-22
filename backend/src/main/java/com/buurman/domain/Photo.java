package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class Photo {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String entityType;
  private UUID entityId;
  private String fileKey;
  private @Nullable String thumbnailFileKey;
  private String fileName;
  private Long fileSize;
  private String mimeType;
  private @Nullable String title;
  private @Nullable String notes;
  private @Nullable Boolean isMainPhoto;
  private UUID uploadedBy;
  private Instant uploadedAt;
  private @Nullable Instant deletedAt;

  public Photo(
      UUID id,
      UUID teamId,
      String entityType,
      UUID entityId,
      String fileKey,
      String fileName,
      Long fileSize,
      String mimeType,
      String title,
      String notes,
      Boolean isMainPhoto,
      UUID uploadedBy,
      Instant uploadedAt,
      Instant deletedAt) {
    this.id = id;
    this.teamId = teamId;
    this.entityType = entityType;
    this.entityId = entityId;
    this.fileKey = fileKey;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.mimeType = mimeType;
    this.title = title;
    this.notes = notes;
    this.isMainPhoto = isMainPhoto;
    this.uploadedBy = uploadedBy;
    this.uploadedAt = uploadedAt;
    this.deletedAt = deletedAt;
  }

  public enum EntityType {
    PROPERTY,
    TENANT,
    CONTRACT,
    PAYMENT,
    EXPENSE
  }
}
