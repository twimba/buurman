package com.buurman.service;

import static com.buurman.util.UlidGenerator.newExpenseId;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jooq.Record2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Expense;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Property;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
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

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ExpenseResponse createExpense(CreateExpenseRequest request, UserPrincipal principal) {
    // Resolve property by identifier
    Property property =
        propertyRepository.getByIdentifierAndTeamId(
            request.propertyIdentifier(), principal.getTeamId());

    Expense expense = expenseMapper.toEntity(request);
    expense.setPropertyId(property.getId());
    expense.setIdentifier(newExpenseId().value());
    expense.setTeamId(principal.getTeamId());
    expense.setCreatedBy(principal.getUserId());
    expense.setUpdatedBy(principal.getUserId());
    expense.setCreatedAt(clock.instant());
    expense.setUpdatedAt(clock.instant());

    // Validate currency is provided
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
        principal.getTeamId(),
        "EXPENSE",
        savedExpense.getId(),
        principal.getUserId(),
        savedExpense);

    String propertyName =
        property.getStreet() != null
            ? property.getStreet() + ", " + property.getCity()
            : property.getIdentifier();
    String currency = savedExpense.getCurrency();
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(principal.getTeamId())
            .notificationType(NotificationType.EXPENSE_CREATED)
            .templateName("expense-created")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "category",
                    savedExpense.getCategory() != null ? savedExpense.getCategory().name() : "N/A",
                    "amount",
                    currency + " " + savedExpense.getAmount(),
                    "description",
                    savedExpense.getDescription() != null ? savedExpense.getDescription() : "",
                    "baseUrl",
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return enrichExpenseResponse(savedExpense, principal.getTeamId());
  }

  @Transactional(readOnly = true)
  public ExpenseResponse getExpense(String identifier, UserPrincipal principal) {
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return enrichExpenseResponse(expense, principal.getTeamId());
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getAllExpenses(UserPrincipal principal) {
    List<Expense> expenses = expenseRepository.findAllByTeamId(principal.getTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<ExpenseResponse> getExpensesPaginated(
      UserPrincipal principal, String category, UUID propertyId, PageRequest pageRequest) {
    PaginatedResult<Expense> result =
        expenseRepository.findAllByTeamIdPaginated(
            principal.getTeamId(), category, propertyId, pageRequest);
    List<ExpenseResponse> responses =
        result.items().stream()
            .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public ExpenseStatsResponse getExpenseStats(UserPrincipal principal) {
    UUID teamId = principal.getTeamId();

    Record2<Integer, BigDecimal> totalStats = expenseRepository.getTotalStats(teamId);
    String currency = expenseRepository.findCurrencyByTeamId(teamId);

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

    return new ExpenseStatsResponse(
        CurrencyUtils.sumToMajorUnits(totalStats.value2(), currency),
        currency,
        topCategories,
        monthlyTrend);
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getExpensesByProperty(
      String propertyIdentifier, UserPrincipal principal) {
    // Resolve property identifier to UUID
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId());

    List<Expense> expenses =
        expenseRepository.findByPropertyId(property.getId(), principal.getTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getExpensesByCategory(
      Expense.ExpenseCategory category, UserPrincipal principal) {
    List<Expense> expenses = expenseRepository.findByCategory(category, principal.getTeamId());

    return expenses.stream()
        .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ExpenseResponse updateExpense(
      String identifier, UpdateExpenseRequest request, UserPrincipal principal) {
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    ExpenseResponse oldState = enrichExpenseResponse(expense, principal.getTeamId());

    expenseMapper.updateEntity(expense, request);
    expense.setUpdatedBy(principal.getUserId());
    expense.setUpdatedAt(clock.instant());

    Expense updatedExpense = expenseRepository.save(expense);
    ExpenseResponse newState = enrichExpenseResponse(updatedExpense, principal.getTeamId());

    log.info(
        "Updated expense {} by user {}", updatedExpense.getIdentifier(), principal.getUserId());

    auditService.logUpdate(
        principal.getTeamId(),
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
    UUID teamId = principal.getTeamId();
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
      String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return documentService.uploadDocument(
        file, "EXPENSE", expense.getId(), expense.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getExpenseDocuments(String identifier, UserPrincipal principal) {
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return documentService.getDocuments("EXPENSE", expense.getId(), principal);
  }

  public List<RecentActivityResponse> getExpenseAuditLog(
      String identifier, UserPrincipal principal) {
    Expense expense = expenseRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return auditService.getEntityAuditLog(principal.getTeamId(), "EXPENSE", expense.getId());
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
        propertySummary,
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
