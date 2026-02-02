package com.buurman.service;

import com.buurman.domain.Expense;
import com.buurman.domain.Property;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.mapper.ExpenseMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final com.buurman.mapper.DocumentMapper documentMapper;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            PropertyRepository propertyRepository,
            DocumentRepository documentRepository,
            ExpenseMapper expenseMapper,
            PropertyMapper propertyMapper,
            AuditService auditService,
            com.buurman.mapper.DocumentMapper documentMapper) {
        this.expenseRepository = expenseRepository;
        this.propertyRepository = propertyRepository;
        this.documentRepository = documentRepository;
        this.expenseMapper = expenseMapper;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, UserPrincipal principal) {
        // Validate property exists and belongs to team
        Property property = propertyRepository.findByIdAndTeamId(request.propertyId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        Expense expense = expenseMapper.toEntity(request);
        expense.setIdentifier(UlidGenerator.generate());
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
    public ExpenseResponse getExpense(UUID id, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdAndTeamId(id, principal.getTeamId())
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
    public List<ExpenseResponse> getExpensesByProperty(UUID propertyId, UserPrincipal principal) {
        // Verify property belongs to team
        propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        List<Expense> expenses = expenseRepository.findByPropertyId(propertyId, principal.getTeamId());

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
    public ExpenseResponse updateExpense(UUID id, UpdateExpenseRequest request, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        expenseMapper.updateEntity(expense, request);
        expense.setUpdatedBy(principal.getUserId());
        expense.setUpdatedAt(Instant.now());

        Expense updatedExpense = expenseRepository.save(expense);

        log.info("Updated expense {} by user {}", updatedExpense.getIdentifier(), principal.getUserId());

        auditService.logUpdate(principal.getTeamId(), "EXPENSE", updatedExpense.getId(), principal.getUserId(),
                expense, updatedExpense, new java.util.HashMap<>());

        return enrichExpenseResponse(updatedExpense, principal.getTeamId());
    }

    @Transactional
    public void deleteExpense(UUID id, UserPrincipal principal) {
        Expense expense = expenseRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        expenseRepository.softDeleteByIdAndTeamId(id, principal.getTeamId());

        log.info("Deleted expense {} by user {}", expense.getIdentifier(), principal.getUserId());

        auditService.logDelete(principal.getTeamId(), "EXPENSE", id, principal.getUserId(), expense);
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
                response.id(),
                response.identifier(),
                response.teamId(),
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
