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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        ExportCategory[] categories = {
          new ExportCategory(
              "properties.csv",
              "All properties (units, buildings, etc.) registered in your portfolio.",
              () -> exportTable(PROPERTIES, teamId)),
          new ExportCategory(
              "contacts.csv",
              "Contacts in your address book (tenants, landlords, agents, vendors).",
              () -> exportTable(CONTACTS, teamId)),
          new ExportCategory(
              "contact-addresses.csv",
              "Postal/residential addresses linked to your contacts.",
              () -> exportTable(CONTACT_ADDRESSES, teamId)),
          new ExportCategory(
              "contracts.csv",
              "Rental, lease and management contracts.",
              () -> exportTable(CONTRACTS, teamId)),
          new ExportCategory(
              "contract-parties.csv",
              "Parties (tenants/landlords) attached to each contract.",
              () -> exportTable(CONTRACT_PARTIES, teamId)),
          new ExportCategory(
              "contract-rent-periods.csv",
              "Rent amount changes over time for each contract.",
              () -> exportTable(CONTRACT_RENT_PERIODS, teamId)),
          new ExportCategory(
              "contract-payment-instructions.csv",
              "Payment instructions (bank accounts, schedules) for contracts.",
              () -> exportTable(CONTRACT_PAYMENT_INSTRUCTIONS, teamId)),
          new ExportCategory(
              "payments.csv",
              "Scheduled and incurred payments due under contracts.",
              () -> exportTable(PAYMENTS, teamId)),
          new ExportCategory(
              "payment-receivals.csv",
              "Money actually received against each payment.",
              () -> exportTable(PAYMENT_RECEIVALS, teamId)),
          new ExportCategory(
              "expenses.csv",
              "Expenses incurred against properties (repairs, utilities, etc.).",
              () -> exportTable(EXPENSES, teamId)),
          new ExportCategory(
              "documents-metadata.csv",
              "Metadata for every document attached to any entity (the files themselves are in"
                  + " /documents).",
              () -> exportTable(DOCUMENTS, teamId)),
          new ExportCategory(
              "photos-metadata.csv",
              "Metadata for every photo attached to any entity (the files themselves are in"
                  + " /photos).",
              () -> exportTable(PHOTOS, teamId)),
          new ExportCategory(
              "property-acquisitions.csv",
              "Purchase/acquisition details for each property.",
              () -> exportTable(PROPERTY_ACQUISITIONS, teamId)),
          new ExportCategory(
              "property-valuations.csv",
              "Historical valuations recorded for properties.",
              () -> exportTable(PROPERTY_VALUATIONS, teamId)),
          new ExportCategory(
              "property-fees.csv",
              "Recurring fees associated with properties (HOA, management, etc.).",
              () -> exportTable(PROPERTY_FEES, teamId)),
          new ExportCategory(
              "property-taxes.csv",
              "Property tax records.",
              () -> exportTable(PROPERTY_TAXES, teamId)),
          new ExportCategory(
              "property-insurances.csv",
              "Insurance policies linked to properties.",
              () -> exportTable(PROPERTY_INSURANCES, teamId)),
          new ExportCategory(
              "property-financings.csv",
              "Mortgages and other financing arrangements per property.",
              () -> exportTable(PROPERTY_FINANCINGS, teamId)),
          new ExportCategory(
              "property-outdoor-areas.csv",
              "Outdoor area details (garden, balcony, parking) per property.",
              () -> exportTable(PROPERTY_OUTDOOR_AREAS, teamId)),
          new ExportCategory(
              "amenities.csv",
              "Amenities (appliances, features) attached to properties.",
              () -> exportTable(AMENITIES, teamId)),
          new ExportCategory(
              "property-occupancy-periods.csv",
              "Occupancy intervals (occupied/vacant) for each property.",
              () -> exportTable(PROPERTY_OCCUPANCY_PERIODS, teamId)),
          new ExportCategory(
              "team-members.csv",
              "Members of your team and their roles.",
              () -> exportTeamMembers(teamId)),
        };

        List<Sid> propertyIdentifiers = fetchIdentifiers(PROPERTIES, teamId);
        List<Sid> contactIdentifiers = fetchIdentifiers(CONTACTS, teamId);
        List<Sid> contractIdentifiers = fetchIdentifiers(CONTRACTS, teamId);

        List<MediaItem> documents = fetchMedia(teamId, false);
        List<MediaItem> photos = fetchMedia(teamId, true);

        int totalBooklets =
            propertyIdentifiers.size() + contactIdentifiers.size() + contractIdentifiers.size();
        int totalSteps =
            categories.length
                + totalBooklets
                + documents.size()
                + photos.size()
                + 3; // team.json, manifest, index.html
        int completed = 0;

        for (ExportCategory category : categories) {
          byte[] csvData = category.exporter.export();
          addZipEntry(zos, folderName + "/" + category.filename, csvData);
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }

        String bookletFolder = folderName + "/booklets";
        for (Sid identifier : propertyIdentifiers) {
          try {
            byte[] pdf =
                exportService.generatePropertyBrochurePDF(
                    PropertyIdentifier.of(identifier.value()), teamId, Locale.getDefault());
            addZipEntry(zos, bookletFolder + "/properties/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate property booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }
        for (Sid identifier : contactIdentifiers) {
          try {
            byte[] pdf =
                exportService.generateContactReportPDF(
                    ContactIdentifier.of(identifier.value()), teamId, Locale.getDefault());
            addZipEntry(zos, bookletFolder + "/contacts/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate contact booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }
        for (Sid identifier : contractIdentifiers) {
          try {
            byte[] pdf =
                exportService.generateContractReportPDF(
                    ContractIdentifier.of(identifier.value()), teamId, Locale.getDefault());
            addZipEntry(zos, bookletFolder + "/contracts/" + identifier.value() + ".pdf", pdf);
          } catch (Exception e) {
            log.warn("Failed to generate contract booklet for {}: {}", identifier, e.getMessage());
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }

        int documentsIncluded = 0;
        for (MediaItem item : documents) {
          if (streamMediaToZip(zos, folderName + "/documents", item)) {
            documentsIncluded++;
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }

        int photosIncluded = 0;
        for (MediaItem item : photos) {
          if (streamMediaToZip(zos, folderName + "/photos", item)) {
            photosIncluded++;
          }
          completed++;
          takeoutRepository.updateProgress(takeoutId, (int) ((completed * 90.0) / totalSteps));
        }

        byte[] teamJson = exportTeamJson(teamId);
        addZipEntry(zos, folderName + "/team.json", teamJson);
        completed++;

        byte[] manifest =
            buildManifest(
                teamIdentifier,
                dateSuffix,
                categories,
                propertyIdentifiers.size(),
                contactIdentifiers.size(),
                contractIdentifiers.size(),
                documentsIncluded,
                photosIncluded);
        addZipEntry(zos, folderName + "/manifest.json", manifest);
        completed++;

        byte[] indexHtml =
            buildIndexHtml(
                teamIdentifier,
                dateSuffix,
                categories,
                propertyIdentifiers.size(),
                contactIdentifiers.size(),
                contractIdentifiers.size(),
                documentsIncluded,
                photosIncluded);
        addZipEntry(zos, folderName + "/index.html", indexHtml);
        takeoutRepository.updateProgress(takeoutId, 95);
      }

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

  private List<MediaItem> fetchMedia(UUID teamId, boolean isPhoto) {
    if (isPhoto) {
      var records =
          dsl.select(
                  PHOTOS.IDENTIFIER,
                  PHOTOS.ENTITY_TYPE,
                  PHOTOS.ENTITY_ID,
                  PHOTOS.FILE_KEY,
                  PHOTOS.FILE_NAME)
              .from(PHOTOS)
              .where(PHOTOS.TEAM_ID.eq(teamId))
              .and(PHOTOS.DELETED_AT.isNull())
              .fetch();
      return buildMediaItems(
          records,
          PHOTOS.IDENTIFIER,
          PHOTOS.ENTITY_TYPE,
          PHOTOS.ENTITY_ID,
          PHOTOS.FILE_KEY,
          PHOTOS.FILE_NAME,
          teamId);
    }
    var records =
        dsl.select(
                DOCUMENTS.IDENTIFIER,
                DOCUMENTS.ENTITY_TYPE,
                DOCUMENTS.ENTITY_ID,
                DOCUMENTS.FILE_KEY,
                DOCUMENTS.FILE_NAME)
            .from(DOCUMENTS)
            .where(DOCUMENTS.TEAM_ID.eq(teamId))
            .and(DOCUMENTS.DELETED_AT.isNull())
            .fetch();
    return buildMediaItems(
        records,
        DOCUMENTS.IDENTIFIER,
        DOCUMENTS.ENTITY_TYPE,
        DOCUMENTS.ENTITY_ID,
        DOCUMENTS.FILE_KEY,
        DOCUMENTS.FILE_NAME,
        teamId);
  }

  private List<MediaItem> buildMediaItems(
      org.jooq.Result<? extends Record> records,
      Field<Sid> identifierField,
      Field<String> entityTypeField,
      Field<UUID> entityIdField,
      Field<String> fileKeyField,
      Field<String> fileNameField,
      UUID teamId) {
    Map<String, Set<UUID>> idsByType = new HashMap<>();
    for (Record r : records) {
      idsByType
          .computeIfAbsent(r.get(entityTypeField), k -> new HashSet<>())
          .add(r.get(entityIdField));
    }

    Map<String, Map<UUID, String>> identifiersByType = new HashMap<>();
    idsByType.forEach(
        (type, ids) -> identifiersByType.put(type, resolveEntityIdentifiers(type, ids, teamId)));

    List<MediaItem> items = new ArrayList<>(records.size());
    for (Record r : records) {
      String type = r.get(entityTypeField);
      UUID entityId = r.get(entityIdField);
      String resolved =
          identifiersByType
              .getOrDefault(type, Map.of())
              .getOrDefault(entityId, entityId.toString());
      items.add(
          new MediaItem(
              type,
              resolved,
              r.get(fileKeyField),
              r.get(fileNameField),
              r.get(identifierField).value()));
    }
    return items;
  }

  private Map<UUID, String> resolveEntityIdentifiers(
      String entityType, Set<UUID> ids, UUID teamId) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Table<?> table =
        switch (entityType == null ? "" : entityType.toLowerCase(Locale.ROOT)) {
          case "property" -> PROPERTIES;
          case "contact" -> CONTACTS;
          case "contract" -> CONTRACTS;
          case "payment" -> PAYMENTS;
          case "expense" -> EXPENSES;
          case "tax" -> PROPERTY_TAXES;
          default -> null;
        };
    if (table == null) {
      return Map.of();
    }
    Field<UUID> idField = table.field("id", UUID.class);
    Field<Sid> identifierField = table.field("identifier", Sid.class);
    Field<UUID> teamIdField = table.field("team_id", UUID.class);
    if (idField == null || identifierField == null || teamIdField == null) {
      return Map.of();
    }
    Map<UUID, String> result = new HashMap<>();
    dsl.select(idField, identifierField)
        .from(table)
        .where(idField.in(ids))
        .and(teamIdField.eq(teamId))
        .fetch()
        .forEach(r -> result.put(r.get(idField), r.get(identifierField).value()));
    return result;
  }

  private boolean streamMediaToZip(ZipOutputStream zos, String basePath, MediaItem item) {
    String entryName =
        basePath
            + "/"
            + sanitize(item.entityType())
            + "/"
            + sanitize(item.entityIdentifier())
            + "/"
            + item.identifier()
            + "-"
            + sanitize(item.fileName());
    try {
      ZipEntry entry = new ZipEntry(entryName);
      zos.putNextEntry(entry);
      try (InputStream in = s3StorageService.downloadFile(item.fileKey())) {
        in.transferTo(zos);
      }
      zos.closeEntry();
      return true;
    } catch (Exception e) {
      log.warn(
          "Failed to include media file {} (key={}) in takeout: {}",
          item.identifier(),
          item.fileKey(),
          e.getMessage());
      try {
        zos.closeEntry();
      } catch (IOException ignored) {
        // entry may not be open
      }
      return false;
    }
  }

  private static String sanitize(String segment) {
    if (segment == null || segment.isEmpty()) {
      return "_";
    }
    return segment.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
  }

  private byte[] buildManifest(
      String teamIdentifier,
      String dateSuffix,
      ExportCategory[] categories,
      int propertyBooklets,
      int contactBooklets,
      int contractBooklets,
      int documentsIncluded,
      int photosIncluded) {
    try {
      ObjectNode manifest = objectMapper.createObjectNode();
      manifest.put("version", "1.1");
      manifest.put("teamIdentifier", teamIdentifier);
      manifest.put("exportDate", dateSuffix);
      manifest.put("generatedAt", clock.instant().toString());

      ArrayNode files = manifest.putArray("files");
      for (ExportCategory category : categories) {
        ObjectNode entry = files.addObject();
        entry.put("path", category.filename);
        entry.put("description", category.description);
      }
      files.addObject().put("path", "team.json").put("description", "Team profile.");
      files.addObject().put("path", "manifest.json").put("description", "This file.");
      files.addObject().put("path", "index.html").put("description", "Browsable index.");

      ObjectNode booklets = manifest.putObject("booklets");
      booklets.put("properties", propertyBooklets);
      booklets.put("contacts", contactBooklets);
      booklets.put("contracts", contractBooklets);

      ObjectNode media = manifest.putObject("media");
      media.put("documents", documentsIncluded);
      media.put("photos", photosIncluded);

      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
    } catch (Exception e) {
      log.warn("Failed to build manifest", e);
      return "{}".getBytes(StandardCharsets.UTF_8);
    }
  }

  private byte[] buildIndexHtml(
      String teamIdentifier,
      String dateSuffix,
      ExportCategory[] categories,
      int propertyBooklets,
      int contactBooklets,
      int contractBooklets,
      int documentsIncluded,
      int photosIncluded) {
    StringBuilder html = new StringBuilder(8 * 1024);
    html.append("<!DOCTYPE html>\n")
        .append("<html lang=\"en\">\n<head>\n")
        .append("<meta charset=\"UTF-8\">\n")
        .append("<title>Buurman data export — ")
        .append(escapeHtml(teamIdentifier))
        .append("</title>\n")
        .append("<style>\n")
        .append(
            "  body { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto,"
                + " sans-serif; max-width: 880px; margin: 2rem auto; padding: 0 1.5rem; color:"
                + " #1f2937; line-height: 1.55; }\n")
        .append("  h1 { font-size: 1.75rem; margin-bottom: .25rem; }\n")
        .append(
            "  h2 { margin-top: 2rem; border-bottom: 1px solid #e5e7eb; padding-bottom: .35rem;"
                + " }\n")
        .append("  p.lead { color: #6b7280; }\n")
        .append("  table { width: 100%; border-collapse: collapse; margin: .75rem 0; }\n")
        .append(
            "  td, th { padding: .5rem .65rem; border-bottom: 1px solid #f1f5f9; vertical-align:"
                + " top; text-align: left; }\n")
        .append("  th { background: #f9fafb; font-weight: 600; }\n")
        .append("  a { color: #2563eb; text-decoration: none; }\n")
        .append("  a:hover { text-decoration: underline; }\n")
        .append(
            "  code { background: #f3f4f6; padding: 1px 5px; border-radius: 3px; font-size: .9em;"
                + " }\n")
        .append("  .counts { display: flex; gap: 1rem; flex-wrap: wrap; margin: 1rem 0; }\n")
        .append("  .counts div { background: #f9fafb; padding: .5rem 1rem; border-radius: 6px; }\n")
        .append("</style>\n</head>\n<body>\n");

    html.append("<h1>Buurman data export</h1>\n")
        .append("<p class=\"lead\">Team <code>")
        .append(escapeHtml(teamIdentifier))
        .append("</code> &middot; generated ")
        .append(escapeHtml(dateSuffix))
        .append("</p>\n");

    html.append(
        "<p>This archive contains a full snapshot of your team's data. CSV files hold the"
            + " structured records, PDF booklets are human-readable summaries, and the"
            + " <code>documents</code> and <code>photos</code> folders contain every original file"
            + " uploaded against any entity.</p>\n");

    html.append("<div class=\"counts\">")
        .append("<div><strong>")
        .append(propertyBooklets)
        .append("</strong> property booklets</div>")
        .append("<div><strong>")
        .append(contactBooklets)
        .append("</strong> contact reports</div>")
        .append("<div><strong>")
        .append(contractBooklets)
        .append("</strong> contract reports</div>")
        .append("<div><strong>")
        .append(documentsIncluded)
        .append("</strong> documents</div>")
        .append("<div><strong>")
        .append(photosIncluded)
        .append("</strong> photos</div>")
        .append("</div>\n");

    html.append("<h2>Structured data (CSV)</h2>\n")
        .append(
            "<p>Open these in any spreadsheet (Excel, Numbers, Google Sheets) or load them into a"
                + " database. UTF-8 encoded, comma-delimited, with a header row.</p>\n")
        .append("<table><thead><tr><th>File</th><th>Contents</th></tr></thead><tbody>\n");
    for (ExportCategory cat : categories) {
      html.append("<tr><td><a href=\"")
          .append(cat.filename)
          .append("\">")
          .append(cat.filename)
          .append("</a></td><td>")
          .append(escapeHtml(cat.description))
          .append("</td></tr>\n");
    }
    html.append("</tbody></table>\n");

    html.append("<h2>Reference data</h2>\n")
        .append("<table><tbody>\n")
        .append(
            "<tr><td><a href=\"team.json\">team.json</a></td><td>Your team profile (identifier,"
                + " name, settings).</td></tr>\n")
        .append(
            "<tr><td><a href=\"manifest.json\">manifest.json</a></td><td>Machine-readable manifest"
                + " listing every file in this archive.</td></tr>\n")
        .append("</tbody></table>\n");

    html.append("<h2>Booklets (PDF)</h2>\n")
        .append(
            "<p>Human-readable reports, ready to print or share. Files are named after the entity"
                + " identifier so they line up with the CSV rows.</p>\n")
        .append("<table><tbody>\n")
        .append(
            "<tr><td><a href=\"booklets/properties/\">booklets/properties/</a></td><td>One PDF"
                + " brochure per property.</td></tr>\n")
        .append(
            "<tr><td><a href=\"booklets/contacts/\">booklets/contacts/</a></td><td>One PDF report"
                + " per contact.</td></tr>\n")
        .append(
            "<tr><td><a href=\"booklets/contracts/\">booklets/contracts/</a></td><td>One PDF report"
                + " per contract.</td></tr>\n")
        .append("</tbody></table>\n");

    html.append("<h2>Documents</h2>\n")
        .append(
            "<p>Original files attached to your entities (PDFs, scans, invoices, etc.). Organised"
                + " as <code>documents/&lt;entity-type&gt;/&lt;entity-identifier&gt;/&lt;document-identifier&gt;-&lt;original-filename&gt;</code>."
                + " Metadata (titles, notes, upload date) is in <a"
                + " href=\"documents-metadata.csv\">documents-metadata.csv</a>.</p>\n")
        .append("<p><a href=\"documents/\">Browse documents/</a></p>\n");

    html.append("<h2>Photos</h2>\n")
        .append(
            "<p>Original photos attached to your entities. Organised as"
                + " <code>photos/&lt;entity-type&gt;/&lt;entity-identifier&gt;/&lt;photo-identifier&gt;-&lt;original-filename&gt;</code>."
                + " Metadata (titles, main-photo flag, upload date) is in <a"
                + " href=\"photos-metadata.csv\">photos-metadata.csv</a>.</p>\n")
        .append("<p><a href=\"photos/\">Browse photos/</a></p>\n");

    html.append("<h2>How identifiers work</h2>\n")
        .append(
            "<p>Every record has an <code>identifier</code> (26&ndash;29 characters, prefixed by"
                + " entity type — e.g. <code>prp_…</code> for properties). Cross-file relationships"
                + " use these identifiers, so you can join records together in your tool of"
                + " choice.</p>\n");

    html.append("</body>\n</html>\n");
    return html.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private <R extends Record> byte[] toCsv(Field<?>[] fields, Result<R> records) {
    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      String[] headers = Arrays.stream(fields).map(Field::getName).toArray(String[]::new);
      writer.writeNext(headers);

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

  private void addZipEntry(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
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

  private record ExportCategory(String filename, String description, CsvExporter exporter) {}

  private record MediaItem(
      String entityType,
      String entityIdentifier,
      String fileKey,
      String fileName,
      String identifier) {}
}
