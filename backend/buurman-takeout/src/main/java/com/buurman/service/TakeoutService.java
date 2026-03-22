package com.buurman.service;

import static com.buurman.jooq.generated.Tables.AMENITIES;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTACT_ADDRESSES;
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
import static com.buurman.util.FeatureFlags.TAKEOUT_MAX_EXPORTS;

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
import java.util.Optional;
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
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.TakeoutResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.DataTakeoutRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TakeoutService {

  @Setter(
      onMethod_ = {
        @org.springframework.beans.factory.annotation.Autowired,
        @org.springframework.context.annotation.Lazy
      })
  private TakeoutService self;

  private final DataTakeoutRepository takeoutRepository;
  private final TeamPreferencesRepository preferencesRepository;
  private final S3StorageService s3StorageService;
  private final MetricsService metricsService;
  private final FeatureFlagService featureFlagService;
  private final ExportService exportService;
  private final DSLContext dsl;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TakeoutResponse requestTakeout(UUID teamId, String teamIdentifier, UUID userId) {
    if (takeoutRepository.hasInProgressByTeamId(teamId)) {
      throw new BusinessRuleException(
          "An export is already in progress. Please wait for it to complete before requesting"
              + " another.");
    }

    UserPrincipal userPrincipal = SecurityUtils.getCurrentPrincipal();
    featureFlagService
        .getValue(TAKEOUT_MAX_EXPORTS, userPrincipal)
        .ifPresent(
            value -> {
              int maxExports = Integer.parseInt(value.toString());
              long currentCount = takeoutRepository.countActiveByTeamId(teamId);
              if (currentCount >= maxExports) {
                throw new BusinessRuleException(
                    "Export limit reached. Maximum %d exports allowed. Delete existing exports to create new ones."
                        .formatted(maxExports));
              }
            });

    DataTakeout takeout =
        DataTakeout.builder()
            .identifier(Optional.of(SidGenerator.newTakeoutId()))
            .teamId(teamId)
            .status(TakeoutStatus.PENDING)
            .createdBy(userId)
            .updatedBy(userId)
            .build();

    takeout = takeoutRepository.save(takeout);
    log.info(
        "Data takeout requested: {} for team {}",
        takeout.getIdentifier().orElseThrow(),
        teamIdentifier);
    metricsService.incrementCounter("takeout.requested.total");

    // Trigger async processing (via proxy so @Async is honoured)
    self.processTakeout(takeout.getId(), teamId, teamIdentifier);

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
          new ExportCategory("contacts.csv", () -> exportTable(CONTACTS, teamId)),
          new ExportCategory("contact-addresses.csv", () -> exportTable(CONTACT_ADDRESSES, teamId)),
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

        // Collect identifiers for booklet generation
        List<Sid> propertyIdentifiers = fetchIdentifiers(PROPERTIES, teamId);
        List<Sid> contactIdentifiers = fetchIdentifiers(CONTACTS, teamId);
        List<Sid> contractIdentifiers = fetchIdentifiers(CONTRACTS, teamId);
        int totalBooklets =
            propertyIdentifiers.size() + contactIdentifiers.size() + contractIdentifiers.size();
        int totalSteps = categories.length + totalBooklets + 2; // +2 for team.json and manifest
        int completed = 0;

        // Export CSV categories
        for (ExportCategory category : categories) {
          byte[] csvData = category.exporter.export();
          addZipEntry(zos, folderName + "/" + category.filename, csvData);
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 85.0) / totalSteps));
        }

        // Generate booklet PDFs
        String bookletFolder = folderName + "/booklets";
        for (Sid identifier : propertyIdentifiers) {
          try {
            byte[] pdf =
                exportService.generatePropertyBrochurePDF(
                    PropertyIdentifier.of(identifier.value()), teamId);
            addZipEntry(zos, bookletFolder + "/properties/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate property booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 85.0) / totalSteps));
        }
        for (Sid identifier : contactIdentifiers) {
          try {
            byte[] pdf =
                exportService.generateContactReportPDF(
                    ContactIdentifier.of(identifier.value()), teamId);
            addZipEntry(zos, bookletFolder + "/contacts/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate contact booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 85.0) / totalSteps));
        }
        for (Sid identifier : contractIdentifiers) {
          try {
            byte[] pdf =
                exportService.generateContractReportPDF(
                    ContractIdentifier.of(identifier.value()), teamId);
            addZipEntry(zos, bookletFolder + "/contracts/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate contract booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 85.0) / totalSteps));
        }

        // Export team.json
        byte[] teamJson = exportTeamJson(teamId);
        addZipEntry(zos, folderName + "/team.json", teamJson);
        takeoutRepository.updateProgress(takeoutId, 90);

        // Export manifest.json
        byte[] manifest =
            buildManifest(
                teamIdentifier,
                dateSuffix,
                categories,
                propertyIdentifiers.size(),
                contactIdentifiers.size(),
                contractIdentifiers.size());
        addZipEntry(zos, folderName + "/manifest.json", manifest);
        takeoutRepository.updateProgress(takeoutId, 95);
      }

      // Upload to S3
      byte[] zipBytes = baos.toByteArray();
      Sid teamSid = Sid.of(teamIdentifier);
      String fileKey =
          s3StorageService.uploadFile(
              zipBytes,
              "application/zip",
              teamSid,
              "takeout",
              teamSid,
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
  public TakeoutResponse getTakeout(DataTakeoutIdentifier identifier, UUID teamId) {
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
  public void deleteTakeout(DataTakeoutIdentifier identifier, UUID teamId) {
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

  private <R extends Record> List<Sid> fetchIdentifiers(Table<R> table, UUID teamId) {
    Field<UUID> teamIdField = table.field("team_id", UUID.class);
    Field<Sid> identifierField = table.field("identifier", Sid.class);
    Field<?> deletedAtField = table.field("deleted_at");

    if (teamIdField == null || identifierField == null) {
      return List.of();
    }

    var query = dsl.select(identifierField).from(table).where(teamIdField.eq(teamId));
    if (deletedAtField != null) {
      query = query.and(deletedAtField.isNull());
    }
    return query.fetch(identifierField);
  }

  private byte[] buildManifest(
      String teamIdentifier,
      String dateSuffix,
      ExportCategory[] categories,
      int propertyBooklets,
      int contactBooklets,
      int contractBooklets) {
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

      ObjectNode booklets = manifest.putObject("booklets");
      booklets.put("properties", propertyBooklets);
      booklets.put("contacts", contactBooklets);
      booklets.put("contracts", contractBooklets);

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
                          takeout.getIdentifier().orElseThrow(),
                          e);
                      return null;
                    }
                  })
              .orElse(null);
    }

    return new TakeoutResponse(
        takeout.getIdentifier().orElseThrow(),
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
