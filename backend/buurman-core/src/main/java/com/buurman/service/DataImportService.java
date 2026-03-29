package com.buurman.service;

import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTACT_ADDRESSES;
import static com.buurman.jooq.generated.Tables.CONTACT_NOTES;
import static com.buurman.jooq.generated.Tables.CONTACT_RELATIONSHIPS;
import static com.buurman.jooq.generated.Tables.CONTACT_TAGS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTY_CONTACT_HISTORY;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.DataImport;
import com.buurman.domain.DataImportEntityType;
import com.buurman.domain.DataImportItem;
import com.buurman.domain.DataImportStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.DataImportIdentifier;
import com.buurman.dto.request.ImportExecuteRequest;
import com.buurman.dto.request.ImportPreviewRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.DataImportDetailResponse;
import com.buurman.dto.response.DataImportItemResponse;
import com.buurman.dto.response.DataImportResponse;
import com.buurman.dto.response.ImportExecuteResponse;
import com.buurman.dto.response.ImportPreviewItemResponse;
import com.buurman.dto.response.ImportPreviewResponse;
import com.buurman.dto.response.ImportRevertResponse;
import com.buurman.dto.response.ImportUploadResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.ForbiddenException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactTagRepository;
import com.buurman.repository.DataImportRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.imports.ImportFileParser;
import com.buurman.service.imports.ImportFileStore;
import com.buurman.util.FeatureFlags;
import com.buurman.util.PaginationHelper;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataImportService {

  private static final Set<String> VALID_CONTACT_FIELDS =
      Set.of(
          "firstName",
          "lastName",
          "companyName",
          "tradeName",
          "industry",
          "email",
          "invoiceEmail",
          "phone",
          "website",
          "taxNumber",
          "idNumber",
          "dateOfBirth",
          "idExpiryDate",
          "notes");

  private static final int REVERT_WINDOW_DAYS = 30;
  private static final int PREVIEW_MAX_ROWS = 5;

  private final DataImportRepository dataImportRepository;
  private final ContactRepository contactRepository;
  private final ContactTagRepository contactTagRepository;
  private final DSLContext dsl;
  private final ImportFileParser fileParser;
  private final ImportFileStore fileStore;
  private final FeatureFlagService featureFlagService;
  private final ObjectMapper objectMapper;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ImportUploadResponse uploadFile(
      MultipartFile file, boolean headerRow, UserPrincipal principal) {
    String format = fileParser.detectFormat(file);
    if ("XLSX".equals(format)
        && !featureFlagService.isEnabled(FeatureFlags.EXCEL_EXPORT, principal)) {
      throw new ForbiddenException("XLSX import requires the Excel feature to be enabled");
    }

    ImportFileParser.ParsedFile parsed = fileParser.parse(file, headerRow);

    String storeKey =
        fileStore.store(
            new ImportFileStore.ParsedImportData(
                parsed.columns(),
                parsed.rows(),
                file.getOriginalFilename(),
                parsed.fileFormat(),
                Instant.now()));

    List<Map<String, String>> previewRows =
        parsed.rows().size() > PREVIEW_MAX_ROWS
            ? parsed.rows().subList(0, PREVIEW_MAX_ROWS)
            : parsed.rows();

    return new ImportUploadResponse(
        parsed.columns(), previewRows, storeKey, parsed.rows().size(), parsed.fileFormat());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ImportPreviewResponse previewImport(
      ImportPreviewRequest request, UserPrincipal principal) {
    ImportFileStore.ParsedImportData data =
        fileStore
            .get(request.fileName())
            .orElseThrow(
                () -> new BadRequestException("File not found or expired. Please upload again."));

    validateMappings(request.mappings());

    UUID teamId = principal.requireTeamId();

    // Batch lookup: collect all emails from the file, then query once
    Set<String> allEmails =
        data.rows().stream()
            .map(row -> applyMapping(row, request.mappings()))
            .map(mapped -> mapped.get("email"))
            .filter(email -> email != null && !email.isBlank())
            .collect(Collectors.toSet());
    Map<String, Contact> existingByEmail =
        contactRepository.findByEmailsAndTeamId(allEmails, teamId);

    List<ImportPreviewItemResponse> items = new ArrayList<>();
    int toCreate = 0;
    int toSkip = 0;
    int errors = 0;

    for (int i = 0; i < data.rows().size(); i++) {
      Map<String, String> row = data.rows().get(i);
      int rowNumber = i + 1;
      Map<String, String> mapped = applyMapping(row, request.mappings());
      String displayName = computeDisplayName(mapped, request.contactType());

      Optional<String> error = validateRow(mapped, request.contactType());
      if (error.isPresent()) {
        errors++;
        items.add(
            new ImportPreviewItemResponse(
                rowNumber,
                "ERROR",
                displayName,
                Optional.ofNullable(mapped.get("email")),
                Optional.ofNullable(mapped.get("phone")),
                error,
                Optional.empty(),
                Optional.empty()));
        continue;
      }

      String email = mapped.get("email");
      if (email != null && !email.isBlank()) {
        Contact duplicate = existingByEmail.get(email);
        if (duplicate != null) {
          toSkip++;
          items.add(
              new ImportPreviewItemResponse(
                  rowNumber,
                  "SKIP",
                  displayName,
                  Optional.of(email),
                  Optional.ofNullable(mapped.get("phone")),
                  Optional.of("Duplicate email: " + email),
                  duplicate.getIdentifier().map(Sid::value),
                  Optional.of(duplicate.getDisplayName())));
          continue;
        }
      }

      toCreate++;
      items.add(
          new ImportPreviewItemResponse(
              rowNumber,
              "CREATE",
              displayName,
              Optional.ofNullable(mapped.get("email")),
              Optional.ofNullable(mapped.get("phone")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty()));
    }

    return new ImportPreviewResponse(toCreate, toSkip, errors, data.rows().size(), items);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public ImportExecuteResponse executeImport(
      ImportExecuteRequest request, UserPrincipal principal) {
    ImportFileStore.ParsedImportData data =
        fileStore
            .get(request.fileName())
            .orElseThrow(
                () -> new BadRequestException("File not found or expired. Please upload again."));

    validateMappings(request.mappings());

    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    DataImportIdentifier importIdentifier = SidGenerator.newDataImportId();
    UUID importId = UUID.randomUUID();

    DataImport dataImport =
        DataImport.builder()
            .id(importId)
            .identifier(Optional.of(importIdentifier))
            .teamId(teamId)
            .fileName(data.originalFileName())
            .fileFormat(data.fileFormat())
            .entityType(DataImportEntityType.CONTACT)
            .status(DataImportStatus.PROCESSING)
            .totalRows(data.rows().size())
            .columnMapping(request.mappings())
            .createdBy(userId)
            .updatedBy(userId)
            .build();
    dataImportRepository.save(dataImport);

    // Batch lookup: collect all emails, query once for duplicates
    Set<String> allEmails =
        data.rows().stream()
            .map(row -> applyMapping(row, request.mappings()))
            .map(mapped -> mapped.get("email"))
            .filter(email -> email != null && !email.isBlank())
            .collect(Collectors.toSet());
    Set<String> existingEmails =
        contactRepository.findByEmailsAndTeamId(allEmails, teamId).keySet();
    // Track emails created within this import to detect intra-file duplicates
    Set<String> createdEmails = new java.util.HashSet<>();

    int imported = 0;
    int skipped = 0;
    int errored = 0;
    List<DataImportItem> importItems = new ArrayList<>();
    List<Map<String, String>> errorRows = new ArrayList<>();

    for (int i = 0; i < data.rows().size(); i++) {
      Map<String, String> row = data.rows().get(i);
      int rowNumber = i + 1;
      Map<String, String> mapped = applyMapping(row, request.mappings());

      Optional<String> error = validateRow(mapped, request.contactType());
      if (error.isPresent()) {
        errored++;
        Map<String, String> errorRow = new LinkedHashMap<>(row);
        errorRow.put("_error", error.get());
        errorRow.put("_row", String.valueOf(rowNumber));
        errorRows.add(errorRow);
        continue;
      }

      String email = mapped.get("email");
      if (email != null && !email.isBlank()) {
        if (existingEmails.contains(email) || createdEmails.contains(email)) {
          skipped++;
          continue;
        }
      }

      try {
        String displayName = computeDisplayName(mapped, request.contactType());

        Contact contact =
            Contact.builder()
                .identifier(Optional.of(SidGenerator.newContactId()))
                .teamId(teamId)
                .contactType(request.contactType())
                .displayName(displayName)
                .firstName(optionalNonBlank(mapped.getOrDefault("firstName", "")))
                .lastName(optionalNonBlank(mapped.getOrDefault("lastName", "")))
                .companyName(optionalNonBlank(mapped.getOrDefault("companyName", "")))
                .tradeName(optionalNonBlank(mapped.getOrDefault("tradeName", "")))
                .industry(optionalNonBlank(mapped.getOrDefault("industry", "")))
                .email(optionalNonBlank(mapped.getOrDefault("email", "")))
                .invoiceEmail(optionalNonBlank(mapped.getOrDefault("invoiceEmail", "")))
                .phone(optionalNonBlank(mapped.getOrDefault("phone", "")))
                .website(optionalNonBlank(mapped.getOrDefault("website", "")))
                .taxNumber(optionalNonBlank(mapped.getOrDefault("taxNumber", "")))
                .idNumber(optionalNonBlank(mapped.getOrDefault("idNumber", "")))
                .dateOfBirth(parseOptionalDate(mapped.getOrDefault("dateOfBirth", "")))
                .idExpiryDate(parseOptionalDate(mapped.getOrDefault("idExpiryDate", "")))
                .notes(optionalNonBlank(mapped.getOrDefault("notes", "")))
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        Contact saved = contactRepository.save(contact);

        // Set import_id on the contact
        dsl.update(CONTACTS)
            .set(CONTACTS.IMPORT_ID, importId)
            .where(CONTACTS.ID.eq(saved.getId()).and(CONTACTS.TEAM_ID.eq(teamId)))
            .execute();

        // Add tags if specified
        request
            .tags()
            .ifPresent(
                tags -> {
                  for (ContactTag tag : tags) {
                    contactTagRepository.addTag(saved.getId(), teamId, tag, userId);
                  }
                });

        // Track this email to prevent intra-file duplicates
        if (email != null && !email.isBlank()) {
          createdEmails.add(email);
        }

        importItems.add(
            DataImportItem.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .entityType(DataImportEntityType.CONTACT)
                .entityId(saved.getId())
                .rowNumber(rowNumber)
                .build());

        imported++;
      } catch (Exception e) {
        log.warn("Failed to import row {}: {}", rowNumber, e.getMessage());
        errored++;
        Map<String, String> errorRow = new LinkedHashMap<>(row);
        errorRow.put("_error", e.getMessage());
        errorRow.put("_row", String.valueOf(rowNumber));
        errorRows.add(errorRow);
      }
    }

    dataImportRepository.saveItems(importItems);

    Optional<String> errorReport = Optional.empty();
    if (!errorRows.isEmpty()) {
      errorReport = Optional.of(generateErrorReportJson(errorRows));
    }

    DataImportStatus finalStatus;
    if (errored > 0 && imported == 0) {
      finalStatus = DataImportStatus.FAILED;
    } else if (errored > 0) {
      finalStatus = DataImportStatus.PARTIALLY_COMPLETED;
    } else {
      finalStatus = DataImportStatus.COMPLETED;
    }

    dataImportRepository.updateStatus(
        importId, teamId, finalStatus, imported, skipped, errored, errorReport, userId);

    fileStore.remove(request.fileName());

    return new ImportExecuteResponse(
        importIdentifier, imported, skipped, errored, data.rows().size());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PageResponse<DataImportResponse> listImports(
      UserPrincipal principal, PageRequest pageRequest) {
    UUID teamId = principal.requireTeamId();
    PaginationHelper.PaginatedResult<DataImport> result =
        dataImportRepository.findAllByTeamIdPaginated(teamId, pageRequest);

    List<DataImportResponse> items = result.items().stream().map(this::toResponse).toList();

    return PageResponse.of(items, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public DataImportDetailResponse getImportDetail(
      DataImportIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    DataImport dataImport = dataImportRepository.getByIdentifierAndTeamId(identifier, teamId);
    List<DataImportItem> items = dataImportRepository.findItemsByImportId(dataImport.getId());

    List<UUID> contactIds =
        items.stream()
            .filter(item -> item.getEntityType() == DataImportEntityType.CONTACT)
            .map(DataImportItem::getEntityId)
            .toList();

    Map<UUID, Contact> contactMap = new HashMap<>();
    if (!contactIds.isEmpty()) {
      contactRepository
          .findByIdsAndTeamId(contactIds, teamId)
          .forEach(c -> contactMap.put(c.getId(), c));
    }

    List<DataImportItemResponse> itemResponses =
        items.stream()
            .map(
                item -> {
                  Contact contact = contactMap.get(item.getEntityId());
                  String displayName = contact != null ? contact.getDisplayName() : "(deleted)";
                  Sid entityIdentifier =
                      contact != null
                          ? contact.getIdentifier().orElse(Sid.of("unknown"))
                          : Sid.of("deleted");
                  return new DataImportItemResponse(
                      entityIdentifier,
                      item.getEntityType().getDisplayName(),
                      displayName,
                      item.getRowNumber());
                })
            .toList();

    return new DataImportDetailResponse(
        dataImport.getIdentifier().orElseThrow(),
        dataImport.getFileName(),
        dataImport.getFileFormat(),
        dataImport.getEntityType(),
        dataImport.getStatus(),
        dataImport.getTotalRows(),
        dataImport.getImportedRows(),
        dataImport.getSkippedRows(),
        dataImport.getErrorRows(),
        dataImport.getColumnMapping(),
        itemResponses,
        Optional.empty(),
        dataImport.getCreatedAt(),
        dataImport.getRevertedAt());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN')")
  @Transactional
  public ImportRevertResponse revertImport(
      DataImportIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    DataImport dataImport = dataImportRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (dataImport.getStatus() == DataImportStatus.REVERTED) {
      throw new BadRequestException("Import has already been reverted");
    }

    Instant revertDeadline =
        dataImport.getCreatedAt().plusSeconds((long) REVERT_WINDOW_DAYS * 24 * 60 * 60);
    if (Instant.now().isAfter(revertDeadline)) {
      throw new BadRequestException(
          "Import can only be reverted within " + REVERT_WINDOW_DAYS + " days of creation");
    }

    List<UUID> contactIds = dataImportRepository.findContactIdsByImportId(dataImport.getId());

    Map<String, Integer> deletedRelated = new HashMap<>();
    int deletedContacts = 0;
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    if (!contactIds.isEmpty()) {
      // Soft-delete contact-owned entities (have deleted_at column)
      int softDeletedAddresses =
          dsl.update(CONTACT_ADDRESSES)
              .set(CONTACT_ADDRESSES.DELETED_AT, now)
              .where(
                  CONTACT_ADDRESSES
                      .CONTACT_ID
                      .in(contactIds)
                      .and(CONTACT_ADDRESSES.DELETED_AT.isNull()))
              .execute();
      if (softDeletedAddresses > 0) {
        deletedRelated.put("addresses", softDeletedAddresses);
      }

      int softDeletedNotes =
          dsl.update(CONTACT_NOTES)
              .set(CONTACT_NOTES.DELETED_AT, now)
              .where(CONTACT_NOTES.CONTACT_ID.in(contactIds).and(CONTACT_NOTES.DELETED_AT.isNull()))
              .execute();
      if (softDeletedNotes > 0) {
        deletedRelated.put("notes", softDeletedNotes);
      }

      int softDeletedRelationships =
          dsl.update(CONTACT_RELATIONSHIPS)
              .set(CONTACT_RELATIONSHIPS.DELETED_AT, now)
              .where(
                  CONTACT_RELATIONSHIPS
                      .SOURCE_CONTACT_ID
                      .in(contactIds)
                      .or(CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID.in(contactIds))
                      .and(CONTACT_RELATIONSHIPS.DELETED_AT.isNull()))
              .execute();
      if (softDeletedRelationships > 0) {
        deletedRelated.put("relationships", softDeletedRelationships);
      }

      int softDeletedParties =
          dsl.update(CONTRACT_PARTIES)
              .set(CONTRACT_PARTIES.DELETED_AT, now)
              .where(
                  CONTRACT_PARTIES
                      .CONTACT_ID
                      .in(contactIds)
                      .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
              .execute();
      if (softDeletedParties > 0) {
        deletedRelated.put("contractParties", softDeletedParties);
      }

      int softDeletedHistory =
          dsl.deleteFrom(PROPERTY_CONTACT_HISTORY)
              .where(PROPERTY_CONTACT_HISTORY.CONTACT_ID.in(contactIds))
              .execute();
      if (softDeletedHistory > 0) {
        deletedRelated.put("contactHistory", softDeletedHistory);
      }

      int softDeletedCalendarFeeds =
          dsl.update(CALENDAR_FEEDS)
              .set(CALENDAR_FEEDS.DELETED_AT, now)
              .where(
                  CALENDAR_FEEDS.CONTACT_ID.in(contactIds).and(CALENDAR_FEEDS.DELETED_AT.isNull()))
              .execute();
      if (softDeletedCalendarFeeds > 0) {
        deletedRelated.put("calendarFeeds", softDeletedCalendarFeeds);
      }

      // Hard-delete junction table (no deleted_at column)
      int deletedTags =
          dsl.deleteFrom(CONTACT_TAGS).where(CONTACT_TAGS.CONTACT_ID.in(contactIds)).execute();
      if (deletedTags > 0) {
        deletedRelated.put("tags", deletedTags);
      }

      // NULL out nullable FK references (don't delete financial/notification data)
      int unlinkedExpenses =
          dsl.update(EXPENSES)
              .setNull(EXPENSES.CONTACT_ID)
              .where(EXPENSES.CONTACT_ID.in(contactIds))
              .execute();
      if (unlinkedExpenses > 0) {
        deletedRelated.put("unlinkedExpenses", unlinkedExpenses);
      }

      int unlinkedPayments =
          dsl.update(PAYMENTS)
              .setNull(PAYMENTS.CONTACT_ID)
              .where(PAYMENTS.CONTACT_ID.in(contactIds))
              .execute();
      if (unlinkedPayments > 0) {
        deletedRelated.put("unlinkedPayments", unlinkedPayments);
      }

      int unlinkedNotifications =
          dsl.update(NOTIFICATIONS)
              .setNull(NOTIFICATIONS.RECIPIENT_CONTACT_ID)
              .where(NOTIFICATIONS.RECIPIENT_CONTACT_ID.in(contactIds))
              .execute();
      if (unlinkedNotifications > 0) {
        deletedRelated.put("unlinkedNotifications", unlinkedNotifications);
      }

      // Soft-delete the contacts themselves
      deletedContacts =
          dsl.update(CONTACTS)
              .set(CONTACTS.DELETED_AT, now)
              .set(CONTACTS.UPDATED_AT, now)
              .set(CONTACTS.UPDATED_BY, userId)
              .where(
                  CONTACTS
                      .ID
                      .in(contactIds)
                      .and(CONTACTS.TEAM_ID.eq(teamId))
                      .and(CONTACTS.DELETED_AT.isNull()))
              .execute();
    }

    dataImportRepository.deleteItemsByImportId(dataImport.getId());
    dataImportRepository.markReverted(dataImport.getId(), teamId, userId);

    return new ImportRevertResponse(deletedContacts, deletedRelated);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public Resource downloadErrorReport(DataImportIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    DataImport dataImport = dataImportRepository.getByIdentifierAndTeamId(identifier, teamId);

    String errorReport =
        dataImport
            .getErrorReport()
            .orElseThrow(() -> new NotFoundException("No error report available for this import"));

    String csv = convertErrorReportToCsv(errorReport);
    return new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));
  }

  // --- Private helpers ---

  private void validateMappings(Map<String, String> mappings) {
    if (mappings.isEmpty()) {
      throw new BadRequestException("At least one column mapping is required");
    }
    for (String targetField : mappings.values()) {
      if (!VALID_CONTACT_FIELDS.contains(targetField)) {
        throw new BadRequestException("Invalid target field: " + targetField);
      }
    }
  }

  private Map<String, String> applyMapping(Map<String, String> row, Map<String, String> mappings) {
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
      String sourceColumn = mapping.getKey();
      String targetField = mapping.getValue();
      String value = row.getOrDefault(sourceColumn, "");
      if (!value.isBlank()) {
        result.put(targetField, value.trim());
      }
    }
    return result;
  }

  private String computeDisplayName(Map<String, String> mapped, ContactType contactType) {
    if (contactType == ContactType.COMPANY || contactType == ContactType.SERVICE_PROVIDER) {
      String companyName = mapped.getOrDefault("companyName", "");
      if (!companyName.isBlank()) {
        return companyName;
      }
    }
    String firstName = mapped.getOrDefault("firstName", "");
    String lastName = mapped.getOrDefault("lastName", "");
    String name = (firstName + " " + lastName).trim();
    return name.isEmpty() ? "Unknown" : name;
  }

  private Optional<String> validateRow(Map<String, String> mapped, ContactType contactType) {
    if (contactType == ContactType.INDIVIDUAL) {
      String firstName = mapped.getOrDefault("firstName", "");
      String lastName = mapped.getOrDefault("lastName", "");
      if (firstName.isBlank() && lastName.isBlank()) {
        return Optional.of("Individual contact requires at least first name or last name");
      }
    } else {
      String companyName = mapped.getOrDefault("companyName", "");
      if (companyName.isBlank()) {
        return Optional.of(contactType.getDisplayName() + " contact requires company name");
      }
    }

    String email = mapped.get("email");
    if (email != null && !email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      return Optional.of("Invalid email format: " + email);
    }

    String phone = mapped.get("phone");
    if (phone != null && !phone.isBlank() && !phone.matches("^\\+[1-9]\\d{1,14}$")) {
      return Optional.of("Invalid phone format (expected E.164): " + phone);
    }

    return Optional.empty();
  }

  private Optional<String> optionalNonBlank(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private Optional<LocalDate> parseOptionalDate(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(value.trim()));
    } catch (DateTimeParseException e) {
      log.debug("Could not parse date '{}': {}", value, e.getMessage());
      return Optional.empty();
    }
  }

  private DataImportResponse toResponse(DataImport dataImport) {
    return new DataImportResponse(
        dataImport.getIdentifier().orElseThrow(),
        dataImport.getFileName(),
        dataImport.getFileFormat(),
        dataImport.getEntityType(),
        dataImport.getStatus(),
        dataImport.getTotalRows(),
        dataImport.getImportedRows(),
        dataImport.getSkippedRows(),
        dataImport.getErrorRows(),
        Optional.empty(),
        dataImport.getCreatedAt(),
        dataImport.getRevertedAt());
  }

  private String generateErrorReportJson(List<Map<String, String>> errorRows) {
    try {
      return objectMapper.writeValueAsString(errorRows);
    } catch (Exception e) {
      return "[]";
    }
  }

  private String convertErrorReportToCsv(String jsonReport) {
    try {
      List<Map<String, String>> rows = objectMapper.readValue(jsonReport, new TypeReference<>() {});

      if (rows.isEmpty()) {
        return "No errors";
      }

      StringBuilder sb = new StringBuilder();
      List<String> headers = new ArrayList<>(rows.get(0).keySet());
      sb.append(String.join(",", headers)).append("\n");

      for (Map<String, String> row : rows) {
        List<String> values = new ArrayList<>();
        for (String header : headers) {
          String val = row.getOrDefault(header, "");
          val = sanitizeCsvValue(val);
          if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            val = "\"" + val.replace("\"", "\"\"") + "\"";
          }
          values.add(val);
        }
        sb.append(String.join(",", values)).append("\n");
      }
      return sb.toString();
    } catch (Exception e) {
      return "Error generating report";
    }
  }

  /** Prevent CSV injection by prefixing formula-triggering characters with a single quote. */
  private String sanitizeCsvValue(String value) {
    if (value.isEmpty()) {
      return value;
    }
    char first = value.charAt(0);
    if (first == '='
        || first == '+'
        || first == '-'
        || first == '@'
        || first == '\t'
        || first == '\r') {
      return "'" + value;
    }
    return value;
  }
}
