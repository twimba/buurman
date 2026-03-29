package com.buurman.domain;

import java.time.Instant;
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
public class DataImportItem {

  private UUID id;
  private UUID importId;
  private DataImportEntityType entityType;
  private UUID entityId;
  private int rowNumber;
  private Instant createdAt;
}
