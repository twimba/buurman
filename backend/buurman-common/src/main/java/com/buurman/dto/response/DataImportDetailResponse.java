package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buurman.domain.DataImportEntityType;
import com.buurman.domain.DataImportStatus;
import com.buurman.domain.Sid;

public record DataImportDetailResponse(
    Sid identifier,
    String fileName,
    String fileFormat,
    DataImportEntityType entityType,
    DataImportStatus status,
    int totalRows,
    int importedRows,
    int skippedRows,
    int errorRows,
    Map<String, String> columnMapping,
    List<DataImportItemResponse> items,
    Optional<String> createdByName,
    Instant createdAt,
    Optional<Instant> revertedAt) {}
