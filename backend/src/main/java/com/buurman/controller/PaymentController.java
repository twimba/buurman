package com.buurman.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.domain.identifier.PaymentReceivalIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
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
      Optional<String> status,
      Optional<String> contractIdentifier,
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
    return paymentService.getPaymentsPaginated(
        principal,
        status.orElse(null),
        contractIdentifier.map(ContractIdentifier::of).orElse(null),
        propertyIdentifier.map(PropertyIdentifier::of).orElse(null),
        dateFrom.orElse(null),
        dateTo.orElse(null),
        pageRequest);
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
  public PaymentResponse getPayment(PaymentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getPayment(identifier, principal);
  }

  @Override
  public PaymentResponse updatePayment(
      PaymentIdentifier identifier, UpdatePaymentRequest updatePaymentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.updatePayment(identifier, updatePaymentRequest, principal);
  }

  @Override
  public PaymentResponse markPaymentAsPaid(
      PaymentIdentifier identifier, MarkPaidRequest markPaidRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.markPaymentAsPaid(identifier, markPaidRequest, principal);
  }

  @Override
  public PaymentResponse registerReceival(
      PaymentIdentifier identifier, CreatePaymentReceivalRequest createPaymentReceivalRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.registerReceival(identifier, createPaymentReceivalRequest, principal);
  }

  @Override
  public List<PaymentReceivalResponse> getReceivals(PaymentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getReceivalsForPayment(identifier, principal);
  }

  @Override
  public PaymentResponse updateReceival(
      PaymentIdentifier identifier,
      PaymentReceivalIdentifier receivalIdentifier,
      UpdatePaymentReceivalRequest updatePaymentReceivalRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.updateReceival(
        identifier, receivalIdentifier, updatePaymentReceivalRequest, principal);
  }

  @Override
  public PaymentResponse deleteReceival(
      PaymentIdentifier identifier, PaymentReceivalIdentifier receivalIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.deleteReceival(identifier, receivalIdentifier, principal);
  }

  @Override
  public void deletePayment(PaymentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.deletePayment(identifier, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadPaymentDocument(
      PaymentIdentifier identifier,
      Optional<String> title,
      Optional<String> notes,
      Optional<UploadPhotoRequest> uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return paymentService.uploadDocument(
        identifier, null, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getPaymentDocuments(PaymentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getPaymentDocumentDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = paymentService.getDocumentDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deletePaymentDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getPaymentAuditLog(PaymentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentService.getAuditLog(identifier, principal);
  }
}
