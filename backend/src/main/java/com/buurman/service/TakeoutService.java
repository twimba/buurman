package com.buurman.service;

import static com.buurman.jooq.generated.Tables.AMENITIES;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.CONTRACT_RENT_PERIODS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.jooq.generated.Tables.PHOTOS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OCCUPANCY_PERIODS;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.DataTakeout;
import com.buurman.domain.DataTakeout.TakeoutStatus;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.TakeoutResponse;
import com.buurman.repository.DataTakeoutRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.util.UlidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TakeoutService {

  private final DataTakeoutRepository takeoutRepository;
  private final TeamPreferencesRepository preferencesRepository;
  private final S3StorageService s3StorageService;
  private final MetricsService metricsService;
  private final DSLContext dsl;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TakeoutResponse requestTakeout(UUID teamId, String teamIdentifier, UUID userId) {
    DataTakeout takeout =
        DataTakeout.builder()
            .identifier(UlidGenerator.newTakeoutId().value())
            .teamId(teamId)
            .status(TakeoutStatus.PENDING)
            .createdBy(userId)
            .updatedBy(userId)
            .build();

    takeout = takeoutRepository.save(takeout);
    log.info("Data takeout requested: {} for team {}", takeout.getIdentifier(), teamIdentifier);
    metricsService.incrementCounter("takeout.requested.total");

    // Trigger async processing
    processTakeout(takeout.getId(), teamId, teamIdentifier);

    return toResponse(takeout);
  }

  @Async
  public void processTakeout(UUID takeoutId, UUID teamId, String teamIdentifier) {
    log.info("Processing data takeout {} for team {}", takeoutId, teamIdentifier);
    Instant start = clock.instant();

    try {
      // Mark as processing
      takeoutRepository
          .findById(takeoutId)
          .ifPresent(
              t -> {
                t.setStatus(TakeoutStatus.PROCESSING);
                takeoutRepository.save(t);
              });

      String dateSuffix = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE);
      String folderName = "takeout-" + teamIdentifier + "-" + dateSuffix;

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {

        // Define export categories with progress tracking
        ExportCategory[] categories = {
          new ExportCategory("properties.csv", () -> exportTable(PROPERTIES, teamId)),
          new ExportCategory("tenants.csv", () -> exportTable(TENANTS, teamId)),
          new ExportCategory("tenant-addresses.csv", () -> exportTable(TENANT_ADDRESSES, teamId)),
          new ExportCategory("contracts.csv", () -> exportTable(CONTRACTS, teamId)),
          new ExportCategory("contract-parties.csv", () -> exportTable(CONTRACT_PARTIES, teamId)),
          new ExportCategory(
              "contract-rent-periods.csv", () -> exportTable(CONTRACT_RENT_PERIODS, teamId)),
          new ExportCategory(
              "contract-payment-instructions.csv",
              () -> exportTable(CONTRACT_PAYMENT_INSTRUCTIONS, teamId)),
          new ExportCategory("payments.csv", () -> exportTable(PAYMENTS, teamId)),
          new ExportCategory("payment-receivals.csv", () -> exportTable(PAYMENT_RECEIVALS, teamId)),
          new ExportCategory("expenses.csv", () -> exportTable(EXPENSES, teamId)),
          new ExportCategory("documents-metadata.csv", () -> exportTable(DOCUMENTS, teamId)),
          new ExportCategory("photos-metadata.csv", () -> exportTable(PHOTOS, teamId)),
          new ExportCategory(
              "property-acquisitions.csv", () -> exportTable(PROPERTY_ACQUISITIONS, teamId)),
          new ExportCategory(
              "property-valuations.csv", () -> exportTable(PROPERTY_VALUATIONS, teamId)),
          new ExportCategory("property-fees.csv", () -> exportTable(PROPERTY_FEES, teamId)),
          new ExportCategory("property-taxes.csv", () -> exportTable(PROPERTY_TAXES, teamId)),
          new ExportCategory(
              "property-insurances.csv", () -> exportTable(PROPERTY_INSURANCES, teamId)),
          new ExportCategory(
              "property-financings.csv", () -> exportTable(PROPERTY_FINANCINGS, teamId)),
          new ExportCategory(
              "property-outdoor-areas.csv", () -> exportTable(PROPERTY_OUTDOOR_AREAS, teamId)),
          new ExportCategory("amenities.csv", () -> exportTable(AMENITIES, teamId)),
          new ExportCategory(
              "property-occupancy-periods.csv",
              () -> exportTable(PROPERTY_OCCUPANCY_PERIODS, teamId)),
          new ExportCategory("team-members.csv", () -> exportTeamMembers(teamId)),
        };

        int totalCategories = categories.length + 2; // +2 for team.json and manifest.json
        int completed = 0;

        // Export CSV categories
        for (ExportCategory category : categories) {
          byte[] csvData = category.exporter.export();
          addZipEntry(zos, folderName + "/" + category.filename, csvData);
          completed++;
          int progress = (int) ((completed * 90.0) / totalCategories);
          takeoutRepository.updateProgress(takeoutId, progress);
        }

        // Export team.json
        byte[] teamJson = exportTeamJson(teamId);
        addZipEntry(zos, folderName + "/team.json", teamJson);
        completed++;
        takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalCategories));

        // Export manifest.json
        byte[] manifest = buildManifest(teamIdentifier, dateSuffix, categories);
        addZipEntry(zos, folderName + "/manifest.json", manifest);
        takeoutRepository.updateProgress(takeoutId, 95);
      }

      // Upload to S3
      byte[] zipBytes = baos.toByteArray();
      String fileKey =
          s3StorageService.uploadFile(
              zipBytes,
              "application/zip",
              teamIdentifier,
              "takeout",
              teamIdentifier,
              "takeout-" + dateSuffix + ".zip");

      // Calculate expiry
      int retentionDays = preferencesRepository.getByTeamId(teamId).getTakeoutRetentionDays();
      Instant expiresAt = clock.instant().plusSeconds((long) retentionDays * 86400);

      takeoutRepository.markCompleted(takeoutId, fileKey, zipBytes.length, expiresAt);

      long durationMs = clock.instant().toEpochMilli() - start.toEpochMilli();
      log.info(
          "Data takeout {} completed: {} bytes, {} ms", takeoutId, zipBytes.length, durationMs);
      metricsService.incrementCounter("takeout.completed.total", "result", "success");

    } catch (Exception e) {
      log.error("Data takeout {} failed", takeoutId, e);
      String errorMessage =
          e.getMessage() != null ? e.getMessage() : "Unknown error during data export";
      takeoutRepository.markFailed(takeoutId, errorMessage);
      metricsService.incrementCounter("takeout.completed.total", "result", "failure");
    }
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TakeoutResponse getTakeout(String identifier, UUID teamId) {
    DataTakeout takeout = takeoutRepository.getByIdentifierAndTeamId(identifier, teamId);
    return toResponse(takeout);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public PageResponse<TakeoutResponse> listTakeouts(UUID teamId, PageRequest pageRequest) {
    var result = takeoutRepository.findAllByTeamId(teamId, pageRequest);
    List<TakeoutResponse> responses = result.items().stream().map(this::toResponse).toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteTakeout(String identifier, UUID teamId) {
    DataTakeout takeout = takeoutRepository.getByIdentifierAndTeamId(identifier, teamId);
    takeout
        .getFileKey()
        .ifPresent(
            key -> {
              try {
                s3StorageService.deleteFile(key);
              } catch (Exception e) {
                log.warn("Failed to delete S3 file for takeout {}: {}", identifier, key, e);
              }
            });
    takeoutRepository.softDelete(takeout.getId());
    log.info("Data takeout {} soft-deleted for team {}", identifier, teamId);
  }

  // ── Private helpers ─────────────────────────────────────────────────────

  private <R extends Record> byte[] exportTable(Table<R> table, UUID teamId) {
    Field<?>[] fields = table.fields();
    Field<UUID> teamIdField = table.field("team_id", UUID.class);
    Field<?> deletedAtField = table.field("deleted_at");

    // Build WHERE condition: always filter by team_id, exclude soft-deleted if column exists
    Result<R> records;
    if (teamIdField != null && deletedAtField != null) {
      records =
          dsl.selectFrom(table).where(teamIdField.eq(teamId)).and(deletedAtField.isNull()).fetch();
    } else if (teamIdField != null) {
      records = dsl.selectFrom(table).where(teamIdField.eq(teamId)).fetch();
    } else {
      records = dsl.selectFrom(table).fetch();
    }

    return toCsv(fields, records);
  }

  private byte[] exportTeamMembers(UUID teamId) {
    var records = dsl.selectFrom(TEAM_MEMBERS).where(TEAM_MEMBERS.TEAM_ID.eq(teamId)).fetch();
    return toCsv(TEAM_MEMBERS.fields(), records);
  }

  private byte[] exportTeamJson(UUID teamId) {
    try {
      var record = dsl.selectFrom(TEAMS).where(TEAMS.ID.eq(teamId)).fetchOne();
      if (record == null) {
        return "{}".getBytes(StandardCharsets.UTF_8);
      }

      ObjectNode node = objectMapper.createObjectNode();
      for (Field<?> field : TEAMS.fields()) {
        Object value = record.get(field);
        if (value != null) {
          node.put(field.getName(), value.toString());
        } else {
          node.putNull(field.getName());
        }
      }
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(node);
    } catch (Exception e) {
      log.warn("Failed to export team JSON for team {}", teamId, e);
      return "{}".getBytes(StandardCharsets.UTF_8);
    }
  }

  private byte[] buildManifest(
      String teamIdentifier, String dateSuffix, ExportCategory[] categories) {
    try {
      ObjectNode manifest = objectMapper.createObjectNode();
      manifest.put("version", "1.0");
      manifest.put("teamIdentifier", teamIdentifier);
      manifest.put("exportDate", dateSuffix);
      manifest.put("generatedAt", clock.instant().toString());

      ArrayNode files = manifest.putArray("files");
      for (ExportCategory category : categories) {
        files.add(category.filename);
      }
      files.add("team.json");
      files.add("manifest.json");

      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
    } catch (Exception e) {
      log.warn("Failed to build manifest", e);
      return "{}".getBytes(StandardCharsets.UTF_8);
    }
  }

  private <R extends Record> byte[] toCsv(Field<?>[] fields, Result<R> records) {
    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      // Header row
      String[] headers = Arrays.stream(fields).map(Field::getName).toArray(String[]::new);
      writer.writeNext(headers);

      // Data rows
      for (R record : records) {
        String[] row = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
          Object value = record.get(fields[i]);
          row[i] = (value != null) ? value.toString() : "";
        }
        writer.writeNext(row);
      }

      return sw.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate CSV", e);
    }
  }

  private void addZipEntry(ZipOutputStream zos, String entryName, byte[] data)
      throws java.io.IOException {
    ZipEntry entry = new ZipEntry(entryName);
    zos.putNextEntry(entry);
    zos.write(data);
    zos.closeEntry();
  }

  private TakeoutResponse toResponse(DataTakeout takeout) {
    String downloadUrl = null;
    if (takeout.getStatus() == TakeoutStatus.COMPLETED) {
      downloadUrl =
          takeout
              .getFileKey()
              .map(
                  key -> {
                    try {
                      URL url = s3StorageService.generatePresignedUrl(key);
                      return url.toString();
                    } catch (Exception e) {
                      log.warn(
                          "Failed to generate presigned URL for takeout {}",
                          takeout.getIdentifier(),
                          e);
                      return null;
                    }
                  })
              .orElse(null);
    }

    return new TakeoutResponse(
        takeout.getIdentifier(),
        takeout.getStatus().name(),
        takeout.getProgress(),
        takeout.getFileSize().orElse(null),
        takeout.getCreatedAt(),
        takeout.getCompletedAt().orElse(null),
        takeout.getExpiresAt().orElse(null),
        downloadUrl);
  }

  @FunctionalInterface
  private interface CsvExporter {
    byte[] export();
  }

  private record ExportCategory(String filename, CsvExporter exporter) {}
}
