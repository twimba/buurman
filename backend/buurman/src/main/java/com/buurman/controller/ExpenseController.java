package com.buurman.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
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
import org.springframework.web.multipart.MultipartFile;
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
      Optional<String> category,
      Optional<String> propertyIdentifier,
      Optional<LocalDate> dateFrom,
      Optional<LocalDate> dateTo,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    UUID propertyId = null;
    if (propertyIdentifier.isPresent()) {
      propertyId =
          expenseService.resolvePropertyId(
              PropertyIdentifier.of(propertyIdentifier.get()), principal.requireTeamId());
    }
    return expenseService.getExpensesPaginated(
        principal,
        category.orElse(null),
        propertyId,
        dateFrom.orElse(null),
        dateTo.orElse(null),
        pageRequest);
  }

  @Override
  public ExpenseStatsResponse getExpenseStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseStats(principal);
  }

  @Override
  public ExpenseResponse getExpense(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpense(identifier, principal);
  }

  @Override
  public ExpenseResponse updateExpense(
      ExpenseIdentifier identifier, UpdateExpenseRequest updateExpenseRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.updateExpense(identifier, updateExpenseRequest, principal);
  }

  @Override
  public void deleteExpense(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    expenseService.deleteExpense(identifier, principal);
  }

  @Override
  public DocumentResponse uploadExpenseDocument(
      ExpenseIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.uploadExpenseDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getExpenseDocuments(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getExpenseDocumentDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteExpenseDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    documentService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getExpenseAuditLog(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseAuditLog(identifier, principal);
  }
}
