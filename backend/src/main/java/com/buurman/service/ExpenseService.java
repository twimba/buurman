package com.buurman.service;

import com.buurman.domain.Expense;
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
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Record2;
import org.jooq.Record3;
import com.buurman.mapper.ExpenseMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final DocumentRepository documentRepository;
    private final ExpenseMapper expenseMapper;
    private final PropertyMapper propertyMapper;
    private final AuditService auditService;
    private final DocumentService documentService;
    private final com.buurman.mapper.DocumentMapper documentMapper;
    private final MetricsService metricsService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            PropertyRepository propertyRepository,
            DocumentRepository documentRepository,
            ExpenseMapper expenseMapper,
            PropertyMapper propertyMapper,
            AuditService auditService,
            DocumentService documentService,
            com.buurman.mapper.DocumentMapper documentMapper,
            MetricsService metricsService) {
        this.expenseRepository = expenseRepository;
        this.propertyRepository = propertyRepository;
        this.documentRepository = documentRepository;
        this.expenseMapper = expenseMapper;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
        this.documentService = documentService;
        this.documentMapper = documentMapper;
        this.metricsService = metricsService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ExpenseResponse createExpense(CreateExpenseRequest request, UserPrincipal principal) {
        // Resolve property by identifier
        Property property = propertyRepository.findByIdentifierAndTeamId(request.propertyIdentifier(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        Expense expense = expenseMapper.toEntity(request);
        expense.setPropertyId(property.getId());
        expense.setIdentifier(UlidGenerator.generate(EntityPrefix.EXP));
        expense.setTeamId(principal.getTeamId());
        expense.setCreatedBy(principal.getUserId());
        expense.setUpdatedBy(principal.getUserId());
        expense.setCreatedAt(Instant.now());
        expense.setUpdatedAt(Instant.now());

        // Set defaults
        if (expense.getCurrency() == null || expense.getCurrency().isEmpty()) {
            expense.setCurrency("EUR");
        }

        Expense savedExpense = expenseRepository.save(expense);

        log.info("Created expense {} for property {} by user {}",
                savedExpense.getIdentifier(), property.getIdentifier(), principal.getUserId());

        auditService.logCreate(principal.getTeamId(), "EXPENSE", savedExpense.getId(), principal.getUserId(), savedExpense);

        return enrichExpenseResponse(savedExpense, principal.getTeamId());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(String identifier, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

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
    public PageResponse<ExpenseResponse> getExpensesPaginated(UserPrincipal principal, String category, UUID propertyId, PageRequest pageRequest) {
        PaginatedResult<Expense> result = expenseRepository.findAllByTeamIdPaginated(
                principal.getTeamId(), category, propertyId, pageRequest);
        List<ExpenseResponse> responses = result.items().stream()
                .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    public ExpenseStatsResponse getExpenseStats(UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Record2<Integer, BigDecimal> totalStats = expenseRepository.getTotalStats(teamId);
        String currency = expenseRepository.findCurrencyByTeamId(teamId);

        List<ExpenseStatsResponse.CategoryTotal> topCategories = expenseRepository.getCategoryBreakdown(teamId)
                .stream()
                .map(r -> new ExpenseStatsResponse.CategoryTotal(
                        r.value1(),
                        r.value3() != null ? r.value3() : BigDecimal.ZERO,
                        r.value2()))
                .toList();

        List<ExpenseStatsResponse.MonthlyTrend> monthlyTrend = expenseRepository.getMonthlyExpenseTrend(teamId, 12)
                .stream()
                .map(r -> new ExpenseStatsResponse.MonthlyTrend(
                        r.value1(),
                        r.value2() != null ? r.value2() : BigDecimal.ZERO))
                .toList();

        return new ExpenseStatsResponse(
                totalStats.value2() != null ? totalStats.value2() : BigDecimal.ZERO,
                currency,
                topCategories,
                monthlyTrend
        );
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByProperty(String propertyIdentifier, UserPrincipal principal) {
        // Resolve property identifier to UUID
        Property property = propertyRepository.findByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        List<Expense> expenses = expenseRepository.findByPropertyId(property.getId(), principal.getTeamId());

        return expenses.stream()
                .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(Expense.ExpenseCategory category, UserPrincipal principal) {
        List<Expense> expenses = expenseRepository.findByCategory(category, principal.getTeamId());

        return expenses.stream()
                .map(expense -> enrichExpenseResponse(expense, principal.getTeamId()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ExpenseResponse updateExpense(String identifier, UpdateExpenseRequest request, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        ExpenseResponse oldState = enrichExpenseResponse(expense, principal.getTeamId());

        expenseMapper.updateEntity(expense, request);
        expense.setUpdatedBy(principal.getUserId());
        expense.setUpdatedAt(Instant.now());

        Expense updatedExpense = expenseRepository.save(expense);
        ExpenseResponse newState = enrichExpenseResponse(updatedExpense, principal.getTeamId());

        log.info("Updated expense {} by user {}", updatedExpense.getIdentifier(), principal.getUserId());

        auditService.logUpdate(principal.getTeamId(), "EXPENSE", updatedExpense.getId(), principal.getUserId(),
                oldState, newState, auditService.getChangedFields(oldState, newState));

        return newState;
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteExpense(String identifier, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        expenseRepository.softDeleteByIdAndTeamId(expense.getId(), principal.getTeamId());

        log.info("Deleted expense {} by user {}", expense.getIdentifier(), principal.getUserId());

        auditService.logDelete(principal.getTeamId(), "EXPENSE", expense.getId(), principal.getUserId(), expense);
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public DocumentResponse uploadExpenseDocument(String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        return documentService.uploadDocument(file, "EXPENSE", expense.getId(), title, notes, principal);
    }

    public List<DocumentResponse> getExpenseDocuments(String identifier, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        return documentService.getDocuments("EXPENSE", expense.getId(), principal);
    }

    public List<RecentActivityResponse> getExpenseAuditLog(String identifier, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        return auditService.getEntityAuditLog(principal.getTeamId(), "EXPENSE", expense.getId());
    }

    // Helper methods

    private ExpenseResponse enrichExpenseResponse(Expense expense, UUID teamId) {
        ExpenseResponse response = expenseMapper.toResponse(expense);

        // Enrich with property summary
        PropertySummary propertySummary = propertyRepository.findByIdAndTeamId(expense.getPropertyId(), teamId)
                .map(propertyMapper::toSummary)
                .orElse(null);

        // Get attached documents
        List<DocumentResponse> documents = documentRepository.findByEntityAndTeamId("EXPENSE", expense.getId(), teamId)
                .stream()
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
                response.updatedAt()
        );
    }
}
