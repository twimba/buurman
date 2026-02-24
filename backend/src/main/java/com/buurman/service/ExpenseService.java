package com.buurman.service;

import static com.buurman.util.UlidGenerator.newExpenseId;
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

import org.jooq.Record2;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Expense;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Property;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.dto.response.ExpenseStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.ExpenseMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.CurrencyUtils;
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
  private final DocumentRepository documentRepository;
  private final ExpenseMapper expenseMapper;
  private final PropertyMapper propertyMapper;
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
      final int index = i;
      CreateExpenseRequest request = requests.get(i);

      // Per-item validation
      Set<ConstraintViolation<CreateExpenseRequest>> violations = validator.validate(request);
      if (!violations.isEmpty()) {
        String errorMsg =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
        results.add(BulkCreateResult.error(index, errorMsg));
        continue;
      }

      try {
        ExpenseResponse response =
            txTemplate.execute(status -> performCreateExpense(request, principal));
        results.add(BulkCreateResult.success(index, response));
      } catch (Exception e) {
        log.warn("Bulk expense creation failed for item {}: {}", index, e.getMessage());
        results.add(BulkCreateResult.error(index, extractErrorMessage(e)));
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

  private ExpenseResponse performCreateExpense(
      CreateExpenseRequest request, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(
            request.propertyIdentifier(), principal.requireTeamId());

    Expense expense = expenseMapper.toEntity(request);
    expense.setPropertyId(property.getId());
    expense.setIdentifier(newExpenseId().value());
    expense.setTeamId(principal.requireTeamId());
    expense.setCreatedBy(principal.getUserId());
    expense.setUpdatedBy(principal.getUserId());
    expense.setCreatedAt(clock.instant());
    expense.setUpdatedAt(clock.instant());

    if (expense.getCurrency() == null || expense.getCurrency().isBlank()) {
      throw new BadRequestException("Currency is required for expenses");
    }

    Expense savedExpense = expenseRepository.save(expense);

    log.info(
        "Created expense {} for property {} by user {}",
        savedExpense.getIdentifier(),
        property.getIdentifier(),
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
  public ExpenseResponse getExpense(String identifier, UserPrincipal principal) {
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
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    PaginatedResult<Expense> result =
        expenseRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(), category, propertyId, dateFrom, dateTo, pageRequest);
    List<ExpenseResponse> responses =
        result.items().stream()
            .map(expense -> enrichExpenseResponse(expense, principal.requireTeamId()))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public UUID resolvePropertyId(String propertyIdentifier, UUID teamId) {
    return propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId).getId();
  }

  public ExpenseStatsResponse getExpenseStats(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Optional<Record2<Integer, BigDecimal>> totalStats = expenseRepository.getTotalStats(teamId);
    @Nullable String currency = expenseRepository.findCurrencyByTeamId(teamId).orElse(null);

    List<ExpenseStatsResponse.CategoryTotal> topCategories =
        expenseRepository.getCategoryBreakdown(teamId).stream()
            .map(
                r ->
                    new ExpenseStatsResponse.CategoryTotal(
                        r.value1(),
                        CurrencyUtils.sumToMajorUnits(r.value3(), currency),
                        r.value2()))
            .toList();

    List<ExpenseStatsResponse.MonthlyTrend> monthlyTrend =
        expenseRepository.getMonthlyExpenseTrend(teamId, 12).stream()
            .map(
                r ->
                    new ExpenseStatsResponse.MonthlyTrend(
                        r.value1(), CurrencyUtils.sumToMajorUnits(r.value2(), currency)))
            .toList();

    String effectiveCurrency = java.util.Objects.requireNonNullElse(currency, "EUR");
    return new ExpenseStatsResponse(
        totalStats
            .map(s -> CurrencyUtils.sumToMajorUnits(s.value2(), effectiveCurrency))
            .orElse(BigDecimal.ZERO),
        effectiveCurrency,
        topCategories,
        monthlyTrend);
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getExpensesByProperty(
      String propertyIdentifier, UserPrincipal principal) {
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
      String identifier, UpdateExpenseRequest request, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    ExpenseResponse oldState = enrichExpenseResponse(expense, principal.requireTeamId());

    expenseMapper.updateEntity(expense, request);
    expense.setUpdatedBy(principal.getUserId());
    expense.setUpdatedAt(clock.instant());

    Expense updatedExpense = expenseRepository.save(expense);
    ExpenseResponse newState = enrichExpenseResponse(updatedExpense, principal.requireTeamId());

    log.info(
        "Updated expense {} by user {}", updatedExpense.getIdentifier(), principal.getUserId());

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
  public void deleteExpense(String identifier, UserPrincipal principal) {
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

    log.info("Deleted expense {} by user {}", expense.getIdentifier(), principal.getUserId());

    auditService.logDelete(teamId, "EXPENSE", expenseId, principal.getUserId(), expense);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DocumentResponse uploadExpenseDocument(
      String identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return documentService.uploadDocument(
        file, "EXPENSE", expense.getId(), expense.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getExpenseDocuments(String identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return documentService.getDocuments("EXPENSE", expense.getId(), principal);
  }

  public List<RecentActivityResponse> getExpenseAuditLog(
      String identifier, UserPrincipal principal) {
    Expense expense =
        expenseRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return auditService.getEntityAuditLog(principal.requireTeamId(), "EXPENSE", expense.getId());
  }

  // Notification helpers

  private void sendExpenseCreatedNotification(ExpenseResponse response, UserPrincipal principal) {
    String propertyName = formatPropertyName(response.property());
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(principal.getTeamId().orElse(null))
            .notificationType(NotificationType.EXPENSE_CREATED)
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
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());
  }

  private void sendBulkExpenseNotification(
      List<ExpenseResponse> successes, UserPrincipal principal) {
    int count = successes.size();
    BigDecimal totalAmount =
        successes.stream().map(ExpenseResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
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
            .teamId(principal.getTeamId().orElse(null))
            .notificationType(NotificationType.EXPENSE_CREATED)
            .templateName("expenses-bulk-created")
            .templateVariables(vars)
            .createdBy(principal.getUserId())
            .build());
  }

  private static String formatPropertyName(Optional<PropertySummary> property) {
    return property
        .map(p -> p.street() != null ? p.street() + ", " + p.city() : p.identifier())
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

    // Get attached documents
    List<DocumentResponse> documents =
        documentRepository.findByEntityAndTeamId("EXPENSE", expense.getId(), teamId).stream()
            .map(documentMapper::toResponse)
            .toList();

    return new ExpenseResponse(
        response.identifier(),
        Optional.ofNullable(propertySummary),
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
