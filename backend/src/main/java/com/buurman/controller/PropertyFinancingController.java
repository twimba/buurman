package com.buurman.controller;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

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
import com.buurman.generated.model.UploadPhotoRequest;
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
  public List<PropertyFinancingResponse> listFinancings(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public PropertyFinancingResponse getFinancing(
      String propertyIdentifier, String financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.getFinancing(financingIdentifier, principal);
  }

  @Override
  public PropertyFinancingResponse createFinancing(
      String propertyIdentifier, CreatePropertyFinancingRequest createPropertyFinancingRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.create(propertyIdentifier, createPropertyFinancingRequest, principal);
  }

  @Override
  public PropertyFinancingResponse updateFinancing(
      String propertyIdentifier,
      String financingIdentifier,
      UpdatePropertyFinancingRequest updatePropertyFinancingRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financingService.update(financingIdentifier, updatePropertyFinancingRequest, principal);
  }

  @Override
  public void deleteFinancing(String propertyIdentifier, String financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    financingService.delete(financingIdentifier, principal);
  }

  // ===== Financing Payments =====

  @Override
  public List<FinancingPaymentResponse> listPayments(
      String propertyIdentifier, String financingIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.listByFinancing(financingIdentifier, principal);
  }

  @Override
  public FinancingPaymentResponse createFinancingPayment(
      String propertyIdentifier,
      String financingIdentifier,
      CreateFinancingPaymentRequest createFinancingPaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.create(financingIdentifier, createFinancingPaymentRequest, principal);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<BulkCreateResult> bulkCreateFinancingPayments(
      String propertyIdentifier,
      String financingIdentifier,
      BulkCreateFinancingPaymentsRequest bulkCreateFinancingPaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return (List)
        paymentService.bulkCreate(
            financingIdentifier, bulkCreateFinancingPaymentsRequest.items(), principal);
  }

  @Override
  public FinancingPaymentResponse updateFinancingPayment(
      String propertyIdentifier,
      String financingIdentifier,
      String paymentIdentifier,
      UpdateFinancingPaymentRequest updateFinancingPaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.update(paymentIdentifier, updateFinancingPaymentRequest, principal);
  }

  @Override
  public void deleteFinancingPayment(
      String propertyIdentifier, String financingIdentifier, String paymentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.delete(paymentIdentifier, principal);
  }

  // ===== Payment Documents =====

  @Override
  @SuppressWarnings(
      "NullAway") // Generated interface uses UploadPhotoRequest instead of MultipartFile
  public DocumentResponse uploadFinancingPaymentDocument(
      String propertyIdentifier,
      String financingIdentifier,
      String paymentIdentifier,
      String title,
      String notes,
      UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.uploadPaymentDocument(paymentIdentifier, null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getFinancingPaymentDocuments(
      String propertyIdentifier, String financingIdentifier, String paymentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getPaymentDocuments(paymentIdentifier, principal);
  }

  @Override
  public Map<String, String> getFinancingPaymentDocumentDownloadUrl(
      String propertyIdentifier, String financingIdentifier, String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteFinancingPaymentDocument(
      String propertyIdentifier, String financingIdentifier, String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    documentService.deleteDocument(documentIdentifier, principal);
  }
}
