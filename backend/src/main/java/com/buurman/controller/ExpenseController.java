package com.buurman.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

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
import com.buurman.generated.api.ExpensesApi;
import com.buurman.generated.model.UploadPhotoRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
import com.buurman.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ExpenseController implements ExpensesApi {

  private final ExpenseService expenseService;
  private final DocumentService documentService;

  @Override
  public ExpenseResponse createExpense(CreateExpenseRequest createExpenseRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.createExpense(createExpenseRequest, principal);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<BulkCreateResult> bulkCreateExpenses(
      BulkCreateExpensesRequest bulkCreateExpensesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return (List) expenseService.bulkCreateExpenses(bulkCreateExpensesRequest.items(), principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getExpenses(
      String category,
      String propertyIdentifier,
      LocalDate dateFrom,
      LocalDate dateTo,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    UUID propertyId = null;
    if (propertyIdentifier != null) {
      propertyId = expenseService.resolvePropertyId(propertyIdentifier, principal.requireTeamId());
    }
    return expenseService.getExpensesPaginated(
        principal, category, propertyId, dateFrom, dateTo, pageRequest);
  }

  @Override
  public ExpenseStatsResponse getExpenseStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseStats(principal);
  }

  @Override
  public ExpenseResponse getExpense(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpense(identifier, principal);
  }

  @Override
  public ExpenseResponse updateExpense(
      String identifier, UpdateExpenseRequest updateExpenseRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.updateExpense(identifier, updateExpenseRequest, principal);
  }

  @Override
  public void deleteExpense(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    expenseService.deleteExpense(identifier, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadExpenseDocument(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return expenseService.uploadExpenseDocument(identifier, null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getExpenseDocuments(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getExpenseDocumentDownloadUrl(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteExpenseDocument(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    documentService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getExpenseAuditLog(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseAuditLog(identifier, principal);
  }
}
