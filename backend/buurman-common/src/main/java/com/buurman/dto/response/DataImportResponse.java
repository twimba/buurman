package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.DataImportEntityType;
import com.buurman.domain.DataImportStatus;
import com.buurman.domain.Sid;

public record DataImportResponse(
    Sid identifier,
    String fileName,
    String fileFormat,
    DataImportEntityType entityType,
    DataImportStatus status,
    int totalRows,
    int importedRows,
    int skippedRows,
    int errorRows,
    Optional<String> createdByName,
    Instant createdAt,
    Optional<Instant> revertedAt) {}
