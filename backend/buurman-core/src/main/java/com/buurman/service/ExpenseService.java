package com.buurman.service;

import static com.buurman.domain.NotificationType.EXPENSE_CREATED;
import static com.buurman.util.SidGenerator.newExpenseId;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.AmountStats;
import com.buurman.domain.Expense;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.ManualAllocationRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ExpenseAllocationResponse;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.dto.response.ExpenseStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.ExpenseMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.MoneyAmount;
import com.buurman.util.PaginationHelper.PaginatedResult;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseService {

  private final ExpenseRepository expenseRepository;
  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final ContactRepository contactRepository;
  private final DocumentRepository documentRepository;
  private final ExpenseAllocationService expenseAllocationService;
  private final ExpenseMapper expenseMapper;
  private final PropertyMapper propertyMapper;
  private final ContactMapper contactMapper;
  private final CurrencyEnforcementService currencyEnforcement;
  private final AuditService auditService;
  private final DocumentService documentService;
  private final com.buurman.mapper.DocumentMapper documentMapper;
  private final MetricsService metricsService;
  private final NotificationService notificationService;
  private final S3StorageService s3StorageService;
  private final com.buurman.repository.AuditLogRepository auditLogRepository;
  private final AppProperties appProperties;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;
  private final Validator validator;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ExpenseResponse createExpense(CreateExpenseRequest request, UserPrincipal principal) {
    ExpenseResponse response = performCreateExpense(request, principal);
    sendExpenseCreatedNotification(response, principal);
    return response;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkCreateResult<ExpenseResponse>> bulkCreateExpenses(
      List<CreateExpenseRequest> requests, UserPrincipal principal) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    List<BulkCreateResult<ExpenseResponse>> results = new ArrayList<>();

    for (int i = 0; i < requests.size(); i++) {
      CreateExpenseRequest request = requests.get(i);

      // Per-item validation
      Set<ConstraintViolation<CreateExpenseRequest>> violations = validator.validate(request);
      if (!violations.isEmpty()) {
        String errorMsg =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
        results.add(BulkCreateResult.error(i, errorMsg));
        continue;
      }

      try {
        ExpenseResponse response =
            txTemplate.execute(status -> performCreateExpense(request, principal));
        results.add(BulkCreateResult.success(i, response));
      } catch (Exception e) {
        log.warn("Bulk expense creation failed for item {}: {}", i, e.getMessage());
        results.add(BulkCreateResult.error(i, extractErrorMessage(e)));
      }
    }

    // Single summary notification for all successful items
    List<ExpenseResponse> successes =
        results.stream()
            .filter(BulkCreateResult::isSuccess)
            .flatMap(r -> r.result().stream())
            .toList();

    if (!successes.isEmpty()) {
      sendBulkExpenseNotification(successes, principal);
    }

    return results;
  }

  private Optional<UUID> resolveUnitId(
      Optional<UnitIdentifier> unitIdentifier, Property property, UserPrincipal principal) {
    return unitIdentifier.map(
        uid -> {
          Unit unit = unitRepository.getByIdentifierAndTeamId(uid, principal.requireTeamId());
          if (!unit.getPropertyId().equals(property.getId())) {
            throw new BadRequestException("Unit does not belong to the expense's property");
          }
          return unit.getId();
        });
  }

  private ExpenseResponse performCreateExpense(
      CreateExpenseRequest request, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(
            request.propertyIdentifier(), principal.requireTeamId());

    Optional<UUID> contactId =
        request
            .contactIdentifier()
            .map(
                cid ->
                    contactRepository
                        .getByIdentifierAndTeamId(cid, principal.requireTeamId())
                        .getId());

    Optional<UUID> unitId = resolveUnitId(request.unitIdentifier(), property, principal);

    Expense expense = expenseMapper.toEntity(request);
    expense.setPropertyId(property.getId());
    expense.setContactId(contactId);
    expense.setUnitId(unitId);
    expense.setIdentifier(Optional.of(newExpenseId()));
    expense.setTeamId(principal.requireTeamId());
    expense.setCreatedBy(principal.getUserId());
    expense.setUpdatedBy(principal.getUserId());
    expense.setCreatedAt(clock.instant());
    expense.setUpdatedAt(clock.instant());

    if (expense.getAmount() == null
        || expense.getAmount().currency() == null
        || expense.getAmount().currency().isBlank()) {
      throw new BadRequestException("Currency is required for expenses");
    }
    currencyEnforcement.validateCurrency(request.currency(), principal.requireTeamId());

    Expense savedExpense = expenseRepository.save(expense);

    if (savedExpense.getUnitId().isEmpty()) {
      expenseAllocationService.allocate(savedExpense, principal.getUserId());
    }

    log.info(
        "Created expense {} for property {} by user {}",
        savedExpense.getIdentifier().orElseThrow(),
        property.getIdentifier().orElseThrow(),
        principal.getUserId());

    auditService.logCreate(
        principal.requireTeamId(),
        "EXPENSE",
        savedExpense.getId(),
        principal.getUserId(),
        savedExpense);

    return enrichExpenseResponse(savedExpense, principal.requireTeamId());
  }

  @Transactional(readOnly = true)
  public ExpenseResponse getExpense(ExpenseIdentifier identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return enrichExpenseResponse(expense, principal.requireTeamId());
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getAllExpenses(UserPrincipal principal) {
    List<Expense> expenses = expenseRepository.findAllByTeamId(principal.requireTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.requireTeamId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<ExpenseResponse> getExpensesPaginated(
      UserPrincipal principal,
      @Nullable String category,
      @Nullable UUID propertyId,
      @Nullable UUID contactId,
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    PaginatedResult<Expense> result =
        expenseRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(),
            category,
            propertyId,
            contactId,
            dateFrom,
            dateTo,
            pageRequest);
    List<ExpenseResponse> responses =
        result.items().stream()
            .map(expense -> enrichExpenseResponse(expense, principal.requireTeamId()))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public UUID resolvePropertyId(PropertyIdentifier propertyIdentifier, UUID teamId) {
    return propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId).getId();
  }

  public UUID resolveContactId(ContactIdentifier contactIdentifier, UUID teamId) {
    return contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId).getId();
  }

  public ExpenseStatsResponse getExpenseStats(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Optional<AmountStats> totalStats = expenseRepository.getTotalStats(teamId);
    String currency = expenseRepository.findCurrencyByTeamId(teamId).orElse(null);

    List<ExpenseStatsResponse.CategoryTotal> topCategories =
        expenseRepository.getCategoryBreakdown(teamId).stream()
            .map(
                r ->
                    new ExpenseStatsResponse.CategoryTotal(
                        r.category(),
                        MoneyAmount.sumToMajorUnits(r.total().orElse(null), currency),
                        r.count()))
            .toList();

    List<ExpenseStatsResponse.MonthlyTrend> monthlyTrend =
        expenseRepository.getMonthlyExpenseTrend(teamId, 12).stream()
            .map(
                r ->
                    new ExpenseStatsResponse.MonthlyTrend(
                        r.month(), MoneyAmount.sumToMajorUnits(r.amount().orElse(null), currency)))
            .toList();

    String effectiveCurrency = java.util.Objects.requireNonNullElse(currency, "EUR");
    return new ExpenseStatsResponse(
        MoneyAmount.sumToMajorUnits(
            totalStats.flatMap(AmountStats::total).orElse(null), effectiveCurrency),
        effectiveCurrency,
        topCategories,
        monthlyTrend);
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getExpensesByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    // Resolve property identifier to UUID
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    List<Expense> expenses =
        expenseRepository.findByPropertyId(property.getId(), principal.requireTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.requireTeamId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getExpensesByCategory(
      Expense.ExpenseCategory category, UserPrincipal principal) {
    List<Expense> expenses = expenseRepository.findByCategory(category, principal.requireTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.requireTeamId()))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ExpenseResponse updateExpense(
      ExpenseIdentifier identifier, UpdateExpenseRequest request, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    ExpenseResponse oldState = enrichExpenseResponse(expense, principal.requireTeamId());

    // Captured before the mapper mutates `expense`, so re-allocation can be gated on whether an
    // allocation input actually changed. NL service-charge settlement statements built from these
    // rows are legal documents — re-running the split on every edit (e.g. a description typo fix)
    // silently launders the recorded basis and, worse, can erase a MANUAL override.
    UUID priorPropertyId = expense.getPropertyId();
    Optional<UUID> priorUnitId = expense.getUnitId();
    BigDecimal priorAmountValue = expense.getAmount().value();
    String priorAmountCurrency = expense.getAmount().currency();

    expenseMapper.updateEntity(expense, request);
    request
        .contactIdentifier()
        .ifPresent(
            cid -> {
              UUID resolvedContactId =
                  contactRepository
                      .getByIdentifierAndTeamId(cid, principal.requireTeamId())
                      .getId();
              expense.setContactId(Optional.of(resolvedContactId));
            });
    // Per the API contract, unitIdentifier is not a "leave untouched when absent" field like the
    // others above: absent makes (or keeps) the expense building-level, present assigns it to that
    // unit. So both branches apply, not just ifPresent — otherwise a unit-level expense could never
    // be moved back to building-level through this endpoint.
    if (request.unitIdentifier().isPresent()) {
      Unit unit =
          unitRepository.getByIdentifierAndTeamId(
              request.unitIdentifier().get(), principal.requireTeamId());
      if (!unit.getPropertyId().equals(expense.getPropertyId())) {
        throw new BadRequestException("Unit does not belong to the expense's property");
      }
      expense.setUnitId(Optional.of(unit.getId()));
    } else {
      expense.setUnitId(Optional.empty());
    }
    expense.setUpdatedBy(principal.getUserId());
    expense.setUpdatedAt(clock.instant());
    request
        .currency()
        .ifPresent(c -> currencyEnforcement.validateCurrency(c, principal.requireTeamId()));

    Expense updatedExpense = expenseRepository.save(expense);

    boolean allocationInputChanged =
        !updatedExpense.getPropertyId().equals(priorPropertyId)
            || !updatedExpense.getUnitId().equals(priorUnitId)
            || updatedExpense.getAmount().value().compareTo(priorAmountValue) != 0
            || !updatedExpense.getAmount().currency().equals(priorAmountCurrency);

    if (updatedExpense.getUnitId().isEmpty()) {
      // Only re-run the automatic split when something allocation-relevant actually changed — an
      // unrelated edit (description, notes, category, date, contact) must leave the persisted
      // rows untouched. allocate() itself additionally refuses to overwrite an active MANUAL set;
      // that override is only ever lifted through the explicit recompute endpoint.
      if (allocationInputChanged) {
        expenseAllocationService.allocate(updatedExpense, principal.getUserId());
      }
    } else {
      // The expense may have just gained a unitId (was building-level with per-unit allocation
      // rows); those rows must be retired so a settlement doesn't double-charge the unit — once by
      // allocation, once directly. A no-op when no rows existed.
      expenseAllocationService.retireAllocations(updatedExpense, principal.getUserId());
    }

    ExpenseResponse newState = enrichExpenseResponse(updatedExpense, principal.requireTeamId());

    log.info(
        "Updated expense {} by user {}",
        updatedExpense.getIdentifier().orElseThrow(),
        principal.getUserId());

    auditService.logUpdate(
        principal.requireTeamId(),
        "EXPENSE",
        updatedExpense.getId(),
        principal.getUserId(),
        oldState,
        newState,
        auditService.getChangedFields(oldState, newState));

    return newState;
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteExpense(ExpenseIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, teamId);
    UUID expenseId = expense.getId();

    // Cascade: delete documents and S3 files
    List<com.buurman.domain.Document> docs =
        documentRepository.findByEntityAndTeamId("EXPENSE", expenseId, teamId);
    for (com.buurman.domain.Document doc : docs) {
      s3StorageService.deleteFile(doc.getFileKey());
    }
    documentRepository.softDeleteByEntityAndTeamId("EXPENSE", expenseId, teamId);

    // Clean up audit logs
    auditLogRepository.deleteByEntityAndTeamId("EXPENSE", expenseId, teamId);

    // Soft-delete the expense itself
    expenseRepository.softDeleteByIdAndTeamId(expenseId, teamId);

    log.info(
        "Deleted expense {} by user {}",
        expense.getIdentifier().orElseThrow(),
        principal.getUserId());

    auditService.logDelete(teamId, "EXPENSE", expenseId, principal.getUserId(), expense);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DocumentResponse uploadExpenseDocument(
      ExpenseIdentifier identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return documentService.uploadDocument(
        file,
        "EXPENSE",
        expense.getId(),
        expense.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getExpenseDocuments(
      ExpenseIdentifier identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return documentService.getDocuments("EXPENSE", expense.getId(), principal);
  }

  public List<RecentActivityResponse> getExpenseAuditLog(
      ExpenseIdentifier identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return auditService.getEntityAuditLog(principal.requireTeamId(), "EXPENSE", expense.getId());
  }

  @Transactional(readOnly = true)
  public List<ExpenseAllocationResponse> getExpenseAllocations(
      ExpenseIdentifier identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return expenseAllocationService.getAllocations(expense);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> overrideExpenseAllocations(
      ExpenseIdentifier identifier, ManualAllocationRequest request, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return expenseAllocationService.overrideManual(expense, request, principal.getUserId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> recomputeExpenseAllocations(
      ExpenseIdentifier identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return expenseAllocationService.recompute(expense, principal.getUserId());
  }

  // Notification helpers

  private void sendExpenseCreatedNotification(ExpenseResponse response, UserPrincipal principal) {
    String propertyName = formatPropertyName(response.property());
    String base = appProperties.email().baseUrl();
    String secondaryUrl =
        response.property().map(p -> base + "/properties/" + p.identifier().value()).orElse("");
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(principal.requireTeamId()))
            .notificationType(EXPENSE_CREATED)
            .templateName("expense-created")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "category",
                    response.category() != null ? response.category().name() : "N/A",
                    "amount",
                    response.currency() + " " + response.amount(),
                    "description",
                    response.description().orElse(""),
                    "baseUrl",
                    base,
                    "primaryUrl",
                    base + "/expenses/" + response.identifier().value(),
                    "secondaryUrl",
                    secondaryUrl))
            .createdBy(principal.getUserId())
            .build());
  }

  private void sendBulkExpenseNotification(
      List<ExpenseResponse> successes, UserPrincipal principal) {
    int count = successes.size();
    BigDecimal totalAmount =
        successes.stream().map(ExpenseResponse::amount).reduce(ZERO, BigDecimal::add);
    String currency = successes.stream().map(ExpenseResponse::currency).findFirst().orElse("");
    String propertyNames =
        successes.stream()
            .map(e -> formatPropertyName(e.property()))
            .distinct()
            .collect(joining(", "));

    Map<String, Object> vars = new HashMap<>();
    vars.put("count", count);
    vars.put("totalAmount", currency + " " + totalAmount);
    vars.put("propertyNames", propertyNames);
    vars.put("baseUrl", appProperties.email().baseUrl());

    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(principal.requireTeamId()))
            .notificationType(EXPENSE_CREATED)
            .templateName("expenses-bulk-created")
            .templateVariables(vars)
            .createdBy(principal.getUserId())
            .build());
  }

  private static String formatPropertyName(Optional<PropertySummary> property) {
    return property
        .map(p -> p.street() != null ? p.street() + ", " + p.city() : p.identifier().value())
        .orElse("N/A");
  }

  private String extractErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return "An unexpected error occurred";
    }
    return message;
  }

  // Helper methods

  private ExpenseResponse enrichExpenseResponse(Expense expense, UUID teamId) {
    ExpenseResponse response = expenseMapper.toResponse(expense);

    // Enrich with property summary
    PropertySummary propertySummary =
        propertyRepository
            .findByIdAndTeamId(expense.getPropertyId(), teamId)
            .map(propertyMapper::toSummary)
            .orElse(null);

    // Enrich with contact summary
    Optional<ContactSummary> contactSummary =
        expense
            .getContactId()
            .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
            .map(contactMapper::toSummary);

    // Enrich with unit identifier (empty for a building-level expense)
    Optional<Sid> unitIdentifier =
        expense
            .getUnitId()
            .flatMap(uid -> unitRepository.findByIdAndTeamId(uid, teamId))
            .flatMap(Unit::getIdentifier);

    // Get attached documents
    List<DocumentResponse> documents =
        documentRepository.findByEntityAndTeamId("EXPENSE", expense.getId(), teamId).stream()
            .map(documentMapper::toResponse)
            .toList();

    return new ExpenseResponse(
        response.identifier(),
        Optional.ofNullable(propertySummary),
        contactSummary,
        unitIdentifier,
        response.category(),
        response.amount(),
        response.currency(),
        response.expenseDate(),
        response.description(),
        response.notes(),
        documents,
        response.createdAt(),
        response.updatedAt());
  }
}
