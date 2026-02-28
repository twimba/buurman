package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.buurman.dto.request.BulkCreateFinancingPaymentsRequest;
import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.CreatePropertyFinancingRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.request.UpdatePropertyFinancingRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.FinancingPaymentResponse;
import com.buurman.dto.response.PropertyFinancingResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
import com.buurman.service.FinancingPaymentService;
import com.buurman.service.PropertyFinancingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/financings")
@Tag(
    name = "Property Financings",
    description = "Financing instruments and payments (mortgages, loans, etc.)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyFinancingController {

  private final PropertyFinancingService financingService;
  private final FinancingPaymentService paymentService;
  private final DocumentService documentService;

  // ===== Financings =====

  @Operation(summary = "List financings", description = "Get all financings for a property")
  @GetMapping
  public List<PropertyFinancingResponse> listFinancings(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(summary = "Get financing", description = "Get details of a specific financing")
  @GetMapping("/{financingIdentifier}")
  public PropertyFinancingResponse getFinancing(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.getFinancing(financingIdentifier, principal);
  }

  @Operation(
      summary = "Create financing",
      description = "Add a new financing instrument (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyFinancingResponse createFinancing(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyFinancingRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.create(propertyIdentifier, request, principal);
  }

  @Operation(
      summary = "Update financing",
      description = "Update a financing instrument (Admin/Editor)")
  @PutMapping("/{financingIdentifier}")
  public PropertyFinancingResponse updateFinancing(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Valid @RequestBody UpdatePropertyFinancingRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.update(financingIdentifier, request, principal);
  }

  @Operation(
      summary = "Delete financing",
      description = "Soft delete a financing instrument (Admin/Editor)")
  @DeleteMapping("/{financingIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteFinancing(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    financingService.delete(financingIdentifier, principal);
  }

  // ===== Financing Payments =====

  @Operation(summary = "List payments", description = "Get all payments for a financing instrument")
  @GetMapping("/{financingIdentifier}/payments")
  public List<FinancingPaymentResponse> listPayments(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.listByFinancing(financingIdentifier, principal);
  }

  @Operation(
      summary = "Create payment",
      description = "Record a payment for a financing instrument (Admin/Editor)")
  @PostMapping("/{financingIdentifier}/payments")
  @ResponseStatus(CREATED)
  public FinancingPaymentResponse createPayment(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Valid @RequestBody CreateFinancingPaymentRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.create(financingIdentifier, request, principal);
  }

  @Operation(
      summary = "Bulk create payments",
      description = "Bulk create financing payments (1-1000 items, Admin/Editor)")
  @PostMapping("/{financingIdentifier}/payments/bulk")
  @ResponseStatus(CREATED)
  public List<BulkCreateResult<FinancingPaymentResponse>> bulkCreatePayments(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Valid @RequestBody BulkCreateFinancingPaymentsRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.bulkCreate(financingIdentifier, request.items(), principal);
  }

  @Operation(summary = "Update payment", description = "Update a payment record (Admin/Editor)")
  @PutMapping("/{financingIdentifier}/payments/{paymentIdentifier}")
  public FinancingPaymentResponse updatePayment(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Payment ULID identifier") @PathVariable String paymentIdentifier,
      @Valid @RequestBody UpdateFinancingPaymentRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.update(paymentIdentifier, request, principal);
  }

  @Operation(summary = "Delete payment", description = "Soft delete a payment (Admin/Editor)")
  @DeleteMapping("/{financingIdentifier}/payments/{paymentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deletePayment(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Payment ULID identifier") @PathVariable String paymentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    paymentService.delete(paymentIdentifier, principal);
  }

  // ===== Payment Documents =====

  @Operation(
      summary = "Upload payment document",
      description = "Upload a document for a financing payment")
  @PostMapping("/{financingIdentifier}/payments/{paymentIdentifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadPaymentDocument(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Payment ULID identifier") @PathVariable String paymentIdentifier,
      @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
      @Parameter(description = "Document title") @RequestParam Optional<String> title,
      @Parameter(description = "Additional notes") @RequestParam Optional<String> notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.uploadPaymentDocument(
        paymentIdentifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Operation(
      summary = "List payment documents",
      description = "Get all documents for a financing payment")
  @GetMapping("/{financingIdentifier}/payments/{paymentIdentifier}/documents")
  public List<DocumentResponse> getPaymentDocuments(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Payment ULID identifier") @PathVariable String paymentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.getPaymentDocuments(paymentIdentifier, principal);
  }

  @Operation(
      summary = "Get payment document download URL",
      description = "Get presigned download URL for a payment document")
  @GetMapping("/{financingIdentifier}/payments/documents/{documentIdentifier}/download")
  public Map<String, String> getPaymentDocumentDownloadUrl(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Operation(
      summary = "Delete payment document",
      description = "Delete a payment document (Admin/Editor)")
  @DeleteMapping("/{financingIdentifier}/payments/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deletePaymentDocument(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Financing ULID identifier") @PathVariable
          String financingIdentifier,
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }
}
