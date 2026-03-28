package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
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
public class DataImport {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private String fileName;
  private String fileFormat;
  @Builder.Default private DataImportEntityType entityType = DataImportEntityType.CONTACT;
  @Builder.Default private DataImportStatus status = DataImportStatus.PROCESSING;
  private int totalRows;
  private int importedRows;
  private int skippedRows;
  private int errorRows;
  @Builder.Default private Map<String, String> columnMapping = Map.of();
  @Builder.Default private Optional<String> errorReport = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> revertedAt = Optional.empty();
  @Builder.Default private Optional<UUID> revertedBy = Optional.empty();
}
