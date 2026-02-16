package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.net.URL;
import java.util.List;
import java.util.Map;

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

import com.buurman.domain.Expense;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
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
      summary = "List expenses",
      description = "Get all expenses with optional filters and pagination")
  @GetMapping
  public PageResponse<ExpenseResponse> getExpenses(
      @RequestParam(required = false) Expense.ExpenseCategory category,
      @RequestParam(required = false) String propertyIdentifier,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {

    // When filtering by propertyIdentifier, use existing non-paginated method wrapped in
    // PageResponse
    if (propertyIdentifier != null) {
      List<ExpenseResponse> results =
          expenseService.getExpensesByProperty(propertyIdentifier, principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return expenseService.getExpensesPaginated(
        principal, category != null ? category.name() : null, null, pageRequest);
  }

  @Operation(summary = "Get expense stats", description = "Get expense statistics for the team")
  @GetMapping("/stats")
  public ExpenseStatsResponse getExpenseStats(@AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseStats(principal);
  }

  @Operation(summary = "Get expense details", description = "Get details of a specific expense")
  @GetMapping("/{identifier}")
  public ExpenseResponse getExpense(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpense(identifier, principal);
  }

  @Operation(summary = "Update expense", description = "Update expense information (Admin/Editor)")
  @PutMapping("/{identifier}")
  public ExpenseResponse updateExpense(
      @PathVariable String identifier,
      @Valid @RequestBody UpdateExpenseRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.updateExpense(identifier, request, principal);
  }

  @Operation(summary = "Delete expense", description = "Soft delete an expense (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteExpense(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    expenseService.deleteExpense(identifier, principal);
  }

  @Operation(
      summary = "Upload document",
      description = "Upload a document for an expense (Admin/Editor)")
  @PostMapping("/{identifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadDocument(
      @PathVariable String identifier,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.uploadExpenseDocument(identifier, file, title, notes, principal);
  }

  @Operation(summary = "List documents", description = "Get all documents for an expense")
  @GetMapping("/{identifier}/documents")
  public List<DocumentResponse> getDocuments(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseDocuments(identifier, principal);
  }

  @Operation(
      summary = "Get download URL",
      description = "Get presigned download URL for a document")
  @GetMapping("/documents/{documentIdentifier}/download")
  public Map<String, String> getDownloadUrl(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
  @DeleteMapping("/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDocument(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  @Operation(summary = "Get audit log", description = "Get audit history for an expense")
  @GetMapping("/{identifier}/audit-log")
  public List<RecentActivityResponse> getExpenseAuditLog(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return expenseService.getExpenseAuditLog(identifier, principal);
  }
}
