package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.DATA_IMPORTS;
import static com.buurman.jooq.generated.Tables.DATA_IMPORT_ITEMS;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import com.buurman.domain.DataImport;
import com.buurman.domain.DataImportEntityType;
import com.buurman.domain.DataImportItem;
import com.buurman.domain.DataImportStatus;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.util.PaginationHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DataImportRepository {

  private final DSLContext dsl;
  private final ObjectMapper objectMapper;

  public void save(DataImport dataImport) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    dsl.insertInto(DATA_IMPORTS)
        .set(DATA_IMPORTS.ID, dataImport.getId())
        .set(DATA_IMPORTS.IDENTIFIER, dataImport.getIdentifier().orElseThrow())
        .set(DATA_IMPORTS.TEAM_ID, dataImport.getTeamId())
        .set(DATA_IMPORTS.FILE_NAME, dataImport.getFileName())
        .set(DATA_IMPORTS.FILE_FORMAT, dataImport.getFileFormat())
        .set(DATA_IMPORTS.ENTITY_TYPE, toJooqEntityType(dataImport.getEntityType()))
        .set(DATA_IMPORTS.STATUS, toJooqStatus(dataImport.getStatus()))
        .set(DATA_IMPORTS.TOTAL_ROWS, dataImport.getTotalRows())
        .set(DATA_IMPORTS.IMPORTED_ROWS, dataImport.getImportedRows())
        .set(DATA_IMPORTS.SKIPPED_ROWS, dataImport.getSkippedRows())
        .set(DATA_IMPORTS.ERROR_ROWS, dataImport.getErrorRows())
        .set(DATA_IMPORTS.COLUMN_MAPPING, toJsonb(dataImport.getColumnMapping()))
        .set(DATA_IMPORTS.ERROR_REPORT, dataImport.getErrorReport().map(JSONB::jsonb).orElse(null))
        .set(DATA_IMPORTS.CREATED_AT, now)
        .set(DATA_IMPORTS.UPDATED_AT, now)
        .set(DATA_IMPORTS.CREATED_BY, dataImport.getCreatedBy())
        .set(DATA_IMPORTS.UPDATED_BY, dataImport.getUpdatedBy())
        .execute();
  }

  public void updateStatus(
      UUID id,
      UUID teamId,
      DataImportStatus status,
      int importedRows,
      int skippedRows,
      int errorRows,
      Optional<String> errorReport,
      UUID updatedBy) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    var update =
        dsl.update(DATA_IMPORTS)
            .set(DATA_IMPORTS.STATUS, toJooqStatus(status))
            .set(DATA_IMPORTS.IMPORTED_ROWS, importedRows)
            .set(DATA_IMPORTS.SKIPPED_ROWS, skippedRows)
            .set(DATA_IMPORTS.ERROR_ROWS, errorRows)
            .set(DATA_IMPORTS.ERROR_REPORT, errorReport.map(JSONB::jsonb).orElse(null))
            .set(DATA_IMPORTS.UPDATED_AT, now)
            .set(DATA_IMPORTS.UPDATED_BY, updatedBy);

    update.where(DATA_IMPORTS.ID.eq(id).and(DATA_IMPORTS.TEAM_ID.eq(teamId))).execute();
  }

  public void markReverted(UUID id, UUID teamId, UUID revertedBy) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    dsl.update(DATA_IMPORTS)
        .set(DATA_IMPORTS.STATUS, com.buurman.jooq.generated.enums.DataImportStatus.REVERTED)
        .set(DATA_IMPORTS.REVERTED_AT, now)
        .set(DATA_IMPORTS.REVERTED_BY, revertedBy)
        .set(DATA_IMPORTS.UPDATED_AT, now)
        .set(DATA_IMPORTS.UPDATED_BY, revertedBy)
        .where(DATA_IMPORTS.ID.eq(id).and(DATA_IMPORTS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public Optional<DataImport> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(DATA_IMPORTS)
        .where(DATA_IMPORTS.IDENTIFIER.eq(identifier).and(DATA_IMPORTS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public DataImport getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Data import not found"));
  }

  public PaginationHelper.PaginatedResult<DataImport> findAllByTeamIdPaginated(
      UUID teamId, PageRequest pageRequest) {
    var condition = DATA_IMPORTS.TEAM_ID.eq(teamId);
    Map<String, org.jooq.Field<?>> sortableFields = Map.of("createdAt", DATA_IMPORTS.CREATED_AT);

    return PaginationHelper.paginate(
        dsl,
        DATA_IMPORTS,
        condition,
        sortableFields,
        DATA_IMPORTS.CREATED_AT,
        pageRequest,
        this::toDomain);
  }

  public void saveItems(List<DataImportItem> items) {
    if (items.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(
            DATA_IMPORT_ITEMS,
            DATA_IMPORT_ITEMS.ID,
            DATA_IMPORT_ITEMS.IMPORT_ID,
            DATA_IMPORT_ITEMS.ENTITY_TYPE,
            DATA_IMPORT_ITEMS.ENTITY_ID,
            DATA_IMPORT_ITEMS.ROW_NUMBER,
            DATA_IMPORT_ITEMS.CREATED_AT);

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    for (DataImportItem item : items) {
      insert =
          insert.values(
              item.getId(),
              item.getImportId(),
              toJooqEntityType(item.getEntityType()),
              item.getEntityId(),
              item.getRowNumber(),
              now);
    }
    insert.execute();
  }

  public List<DataImportItem> findItemsByImportId(UUID importId) {
    return dsl.selectFrom(DATA_IMPORT_ITEMS)
        .where(DATA_IMPORT_ITEMS.IMPORT_ID.eq(importId))
        .orderBy(DATA_IMPORT_ITEMS.ROW_NUMBER.asc())
        .fetch()
        .map(
            record ->
                DataImportItem.builder()
                    .id(record.getId())
                    .importId(record.getImportId())
                    .entityType(fromJooqEntityType(record.getEntityType()))
                    .entityId(record.getEntityId())
                    .rowNumber(record.getRowNumber())
                    .createdAt(record.getCreatedAt().toInstant(ZoneOffset.UTC))
                    .build());
  }

  public List<UUID> findContactIdsByImportId(UUID importId) {
    return dsl.select(DATA_IMPORT_ITEMS.ENTITY_ID)
        .from(DATA_IMPORT_ITEMS)
        .where(
            DATA_IMPORT_ITEMS
                .IMPORT_ID
                .eq(importId)
                .and(
                    DATA_IMPORT_ITEMS.ENTITY_TYPE.eq(
                        com.buurman.jooq.generated.enums.DataImportEntityType.CONTACT)))
        .fetch(DATA_IMPORT_ITEMS.ENTITY_ID);
  }

  public void deleteItemsByImportId(UUID importId) {
    dsl.deleteFrom(DATA_IMPORT_ITEMS).where(DATA_IMPORT_ITEMS.IMPORT_ID.eq(importId)).execute();
  }

  // --- Mapping helpers ---

  private DataImport toDomain(com.buurman.jooq.generated.tables.records.DataImportsRecord record) {
    return DataImport.builder()
        .id(record.getId())
        .identifier(Optional.of(record.getIdentifier()))
        .teamId(record.getTeamId())
        .fileName(record.getFileName())
        .fileFormat(record.getFileFormat())
        .entityType(fromJooqEntityType(record.getEntityType()))
        .status(fromJooqStatus(record.getStatus()))
        .totalRows(record.getTotalRows())
        .importedRows(record.getImportedRows())
        .skippedRows(record.getSkippedRows())
        .errorRows(record.getErrorRows())
        .columnMapping(fromJsonb(record.getColumnMapping()))
        .errorReport(Optional.ofNullable(record.getErrorReport()).map(JSONB::data))
        .createdAt(record.getCreatedAt().toInstant(ZoneOffset.UTC))
        .updatedAt(record.getUpdatedAt().toInstant(ZoneOffset.UTC))
        .createdBy(record.getCreatedBy())
        .updatedBy(record.getUpdatedBy())
        .revertedAt(
            Optional.ofNullable(record.getRevertedAt()).map(ldt -> ldt.toInstant(ZoneOffset.UTC)))
        .revertedBy(Optional.ofNullable(record.getRevertedBy()))
        .build();
  }

  private JSONB toJsonb(Map<String, String> map) {
    try {
      return JSONB.jsonb(objectMapper.writeValueAsString(map));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize column mapping", e);
    }
  }

  private Map<String, String> fromJsonb(JSONB jsonb) {
    if (jsonb == null || jsonb.data() == null) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(jsonb.data(), new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  private com.buurman.jooq.generated.enums.DataImportStatus toJooqStatus(DataImportStatus status) {
    return com.buurman.jooq.generated.enums.DataImportStatus.valueOf(status.name());
  }

  private DataImportStatus fromJooqStatus(
      com.buurman.jooq.generated.enums.DataImportStatus status) {
    return DataImportStatus.valueOf(status.name());
  }

  private com.buurman.jooq.generated.enums.DataImportEntityType toJooqEntityType(
      DataImportEntityType type) {
    return com.buurman.jooq.generated.enums.DataImportEntityType.valueOf(type.name());
  }

  private DataImportEntityType fromJooqEntityType(
      com.buurman.jooq.generated.enums.DataImportEntityType type) {
    return DataImportEntityType.valueOf(type.name());
  }
}
