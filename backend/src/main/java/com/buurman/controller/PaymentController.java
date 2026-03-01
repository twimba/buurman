package com.buurman.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkCreatePaymentsRequest;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.PaymentsApi;
import com.buurman.generated.model.UploadPhotoRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {

  private final PaymentService paymentService;

  @Override
  public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.createPayment(createPaymentRequest, principal);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<BulkCreateResult> bulkCreatePayments(
      BulkCreatePaymentsRequest bulkCreatePaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return (List) paymentService.bulkCreatePayments(bulkCreatePaymentsRequest.items(), principal);
  }

  @Override
  public List<PaymentResponse> bulkGeneratePayments(
      BulkGeneratePaymentsRequest bulkGeneratePaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.bulkGeneratePayments(bulkGeneratePaymentsRequest, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getPayments(
      String status,
      String contractIdentifier,
      String propertyIdentifier,
      LocalDate dateFrom,
      LocalDate dateTo,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    SortDirection sortDirection = SortDirection.valueOf(direction);
    PageRequest pageRequest = PageRequest.of(page, size, sort, sortDirection);
    return paymentService.getPaymentsPaginated(
        principal, status, contractIdentifier, propertyIdentifier, dateFrom, dateTo, pageRequest);
  }

  @Override
  public List<PaymentResponse> getOverduePayments() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getOverduePayments(principal);
  }

  @Override
  public PaymentStatsResponse getPaymentStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getPaymentStats(principal);
  }

  @Override
  public PaymentResponse getPayment(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getPayment(identifier, principal);
  }

  @Override
  public PaymentResponse updatePayment(
      String identifier, UpdatePaymentRequest updatePaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.updatePayment(identifier, updatePaymentRequest, principal);
  }

  @Override
  public PaymentResponse markPaymentAsPaid(String identifier, MarkPaidRequest markPaidRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.markPaymentAsPaid(identifier, markPaidRequest, principal);
  }

  @Override
  public PaymentResponse registerReceival(
      String identifier, CreatePaymentReceivalRequest createPaymentReceivalRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.registerReceival(identifier, createPaymentReceivalRequest, principal);
  }

  @Override
  public List<PaymentReceivalResponse> getReceivals(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getReceivalsForPayment(identifier, principal);
  }

  @Override
  public PaymentResponse updateReceival(
      String identifier,
      String receivalIdentifier,
      UpdatePaymentReceivalRequest updatePaymentReceivalRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.updateReceival(
        identifier, receivalIdentifier, updatePaymentReceivalRequest, principal);
  }

  @Override
  public PaymentResponse deleteReceival(String identifier, String receivalIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.deleteReceival(identifier, receivalIdentifier, principal);
  }

  @Override
  public void deletePayment(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.deletePayment(identifier, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadPaymentDocument(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return paymentService.uploadDocument(identifier, null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getPaymentDocuments(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getPaymentDocumentDownloadUrl(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = paymentService.getDocumentDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deletePaymentDocument(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getPaymentAuditLog(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getAuditLog(identifier, principal);
  }
}
