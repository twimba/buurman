package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Expense.ExpenseCategory;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkCreateExpensesRequest;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.dto.response.ExpenseStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.DocumentService;
import com.buurman.service.ExpenseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Property expense tracking and management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ExpenseController {

  private final ExpenseService expenseService;
  private final DocumentService documentService;
  private final AuditService auditService;

  @Operation(
      summary = "Create expense",
      description = "Record a new property expense (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public ExpenseResponse createExpense(
      @Valid @RequestBody CreateExpenseRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.createExpense(request, principal);
  }

  @Operation(
      summary = "Bulk create expenses",
      description =
          "Create multiple expenses in a single request with per-item error handling"
              + " (Admin/Editor)")
  @PostMapping("/bulk")
  @ResponseStatus(CREATED)
  public List<BulkCreateResult<ExpenseResponse>> bulkCreateExpenses(
      @Valid @RequestBody BulkCreateExpensesRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.bulkCreateExpenses(request.items(), principal);
  }

  @Operation(
      summary = "List expenses",
      description = "Get all expenses with optional filters and pagination")
  @GetMapping
  public PageResponse<ExpenseResponse> getExpenses(
      @Parameter(description = "Filter by category") @RequestParam
          Optional<ExpenseCategory> category,
      @Parameter(description = "Property ULID identifier") @RequestParam
          Optional<String> propertyIdentifier,
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          Optional<LocalDate> dateFrom,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31") @RequestParam
          Optional<LocalDate> dateTo,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          Integer size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {

    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    UUID propertyId =
        propertyIdentifier
            .map(id -> expenseService.resolvePropertyId(id, principal.requireTeamId()))
            .orElse(null);
    return expenseService.getExpensesPaginated(
        principal,
        category.map(ExpenseCategory::name).orElse(null),
        propertyId,
        dateFrom.orElse(null),
        dateTo.orElse(null),
        pageRequest);
  }

  @Operation(summary = "Get expense stats", description = "Get expense statistics for the team")
  @GetMapping("/stats")
  public ExpenseStatsResponse getExpenseStats(@AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseStats(principal);
  }

  @Operation(summary = "Get expense details", description = "Get details of a specific expense")
  @GetMapping("/{identifier}")
  public ExpenseResponse getExpense(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpense(identifier, principal);
  }

  @Operation(summary = "Update expense", description = "Update expense information (Admin/Editor)")
  @PutMapping("/{identifier}")
  public ExpenseResponse updateExpense(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody UpdateExpenseRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.updateExpense(identifier, request, principal);
  }

  @Operation(summary = "Delete expense", description = "Soft delete an expense (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteExpense(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    expenseService.deleteExpense(identifier, principal);
  }

  @Operation(
      summary = "Upload document",
      description = "Upload a document for an expense (Admin/Editor)")
  @PostMapping("/{identifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadDocument(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
      @Parameter(description = "Document title") @RequestParam Optional<String> title,
      @Parameter(description = "Additional notes") @RequestParam Optional<String> notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.uploadExpenseDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Operation(summary = "List documents", description = "Get all documents for an expense")
  @GetMapping("/{identifier}/documents")
  public List<DocumentResponse> getDocuments(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseDocuments(identifier, principal);
  }

  @Operation(
      summary = "Get download URL",
      description = "Get presigned download URL for a document")
  @GetMapping("/documents/{documentIdentifier}/download")
  public Map<String, String> getDownloadUrl(
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
  @DeleteMapping("/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDocument(
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  @Operation(summary = "Get audit log", description = "Get audit history for an expense")
  @GetMapping("/{identifier}/audit-log")
  public List<RecentActivityResponse> getExpenseAuditLog(
      @Parameter(description = "Expense ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseAuditLog(identifier, principal);
  }
}
