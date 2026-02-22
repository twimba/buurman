package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

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

  public enum EntityType {
    PROPERTY,
    TENANT,
    CONTRACT,
    PAYMENT,
    EXPENSE
  }
}
