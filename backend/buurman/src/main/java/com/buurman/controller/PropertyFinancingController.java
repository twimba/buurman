package com.buurman.controller;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.FinancingPaymentIdentifier;
import com.buurman.domain.identifier.PropertyFinancingIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.BulkCreateFinancingPaymentsRequest;
import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.CreatePropertyFinancingRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.request.UpdatePropertyFinancingRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.FinancingPaymentResponse;
import com.buurman.dto.response.PropertyFinancingResponse;
import com.buurman.generated.api.PropertyFinancingsApi;
import org.springframework.web.multipart.MultipartFile;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
import com.buurman.service.FinancingPaymentService;
import com.buurman.service.PropertyFinancingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyFinancingController implements PropertyFinancingsApi {

  private final PropertyFinancingService financingService;
  private final FinancingPaymentService paymentService;
  private final DocumentService documentService;

  // ===== Financings =====

  @Override
  public List<PropertyFinancingResponse> listFinancings(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public PropertyFinancingResponse getFinancing(
      PropertyIdentifier propertyIdentifier, PropertyFinancingIdentifier financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.getFinancing(financingIdentifier, principal);
  }

  @Override
  public PropertyFinancingResponse createFinancing(
      PropertyIdentifier propertyIdentifier,
      CreatePropertyFinancingRequest createPropertyFinancingRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.create(propertyIdentifier, createPropertyFinancingRequest, principal);
  }

  @Override
  public PropertyFinancingResponse updateFinancing(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      UpdatePropertyFinancingRequest updatePropertyFinancingRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.update(financingIdentifier, updatePropertyFinancingRequest, principal);
  }

  @Override
  public void deleteFinancing(
      PropertyIdentifier propertyIdentifier, PropertyFinancingIdentifier financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    financingService.delete(financingIdentifier, principal);
  }

  // ===== Financing Payments =====

  @Override
  public List<FinancingPaymentResponse> listPayments(
      PropertyIdentifier propertyIdentifier, PropertyFinancingIdentifier financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.listByFinancing(financingIdentifier, principal);
  }

  @Override
  public FinancingPaymentResponse createFinancingPayment(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      CreateFinancingPaymentRequest createFinancingPaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.create(financingIdentifier, createFinancingPaymentRequest, principal);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<BulkCreateResult> bulkCreateFinancingPayments(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      BulkCreateFinancingPaymentsRequest bulkCreateFinancingPaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return (List)
        paymentService.bulkCreate(
            financingIdentifier, bulkCreateFinancingPaymentsRequest.items(), principal);
  }

  @Override
  public FinancingPaymentResponse updateFinancingPayment(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      FinancingPaymentIdentifier paymentIdentifier,
      UpdateFinancingPaymentRequest updateFinancingPaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.update(paymentIdentifier, updateFinancingPaymentRequest, principal);
  }

  @Override
  public void deleteFinancingPayment(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      FinancingPaymentIdentifier paymentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.delete(paymentIdentifier, principal);
  }

  // ===== Payment Documents =====

  @Override
  public DocumentResponse uploadFinancingPaymentDocument(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      FinancingPaymentIdentifier paymentIdentifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.uploadPaymentDocument(
        paymentIdentifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getFinancingPaymentDocuments(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      FinancingPaymentIdentifier paymentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getPaymentDocuments(paymentIdentifier, principal);
  }

  @Override
  public Map<String, String> getFinancingPaymentDocumentDownloadUrl(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteFinancingPaymentDocument(
      PropertyIdentifier propertyIdentifier,
      PropertyFinancingIdentifier financingIdentifier,
      DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    documentService.deleteDocument(documentIdentifier, principal);
  }
}
