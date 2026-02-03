package com.buurman.controller;

import com.buurman.domain.Expense;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.DocumentService;
import com.buurman.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses", description = "Property expense tracking and management")
@SecurityRequirement(name = "bearer-jwt")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final DocumentService documentService;
    private final AuditService auditService;

    public ExpenseController(ExpenseService expenseService, DocumentService documentService, AuditService auditService) {
        this.expenseService = expenseService;
        this.documentService = documentService;
        this.auditService = auditService;
    }

    @Operation(summary = "Create expense", description = "Record a new property expense (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.createExpense(request, principal);
    }

    @Operation(summary = "List expenses", description = "Get all expenses with optional filters")
    @GetMapping
    public List<ExpenseResponse> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID propertyId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (propertyId != null) {
            return expenseService.getExpensesByProperty(propertyId, principal);
        }

        if (category != null) {
            try {
                Expense.ExpenseCategory expenseCategory = Expense.ExpenseCategory.valueOf(category.toUpperCase());
                return expenseService.getExpensesByCategory(expenseCategory, principal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid category: " + category);
            }
        }

        return expenseService.getAllExpenses(principal);
    }

    @Operation(summary = "Get expense details", description = "Get details of a specific expense")
    @GetMapping("/{id}")
    public ExpenseResponse getExpense(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.getExpense(id, principal);
    }

    @Operation(summary = "Update expense", description = "Update expense information (Admin/Editor)")
    @PutMapping("/{id}")
    public ExpenseResponse updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.updateExpense(id, request, principal);
    }

    @Operation(summary = "Delete expense", description = "Soft delete an expense (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        expenseService.deleteExpense(id, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for an expense (Admin/Editor)")
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "EXPENSE", id, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for an expense")
    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getDocuments("EXPENSE", id, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentId}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = documentService.getDownloadUrl(documentId, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal);
    }

    @Operation(summary = "Get audit log", description = "Get audit history for an expense")
    @GetMapping("/{id}/audit-log")
    public List<RecentActivityResponse> getExpenseAuditLog(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return auditService.getEntityAuditLog(principal.getTeamId(), "EXPENSE", id);
    }
}
