package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.UlidGenerator.newPaymentId;
import static com.buurman.util.UlidGenerator.newPaymentReceivalId;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.Record2;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.ContractSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentStatsResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantSummary;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PaymentMapper;
import com.buurman.mapper.PaymentReceivalMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.PaginationHelper.PaginatedResult;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository receivalRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final TenantRepository tenantRepository;
  private final ContractPartyService contractPartyService;
  private final DocumentRepository documentRepository;
  private final PaymentMapper paymentMapper;
  private final PaymentReceivalMapper receivalMapper;
  private final ContractMapper contractMapper;
  private final PropertyMapper propertyMapper;
  private final TenantMapper tenantMapper;
  private final AuditService auditService;
  private final DocumentService documentService;
  private final com.buurman.mapper.DocumentMapper documentMapper;
  private final MetricsService metricsService;
  private final NotificationService notificationService;
  private final S3StorageService s3StorageService;
  private final com.buurman.repository.AuditLogRepository auditLogRepository;
  private final AppProperties appProperties;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;
  private final Validator validator;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse createPayment(CreatePaymentRequest request, UserPrincipal principal) {
    return performCreatePayment(request, principal);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkCreateResult<PaymentResponse>> bulkCreatePayments(
      List<CreatePaymentRequest> requests, UserPrincipal principal) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    List<BulkCreateResult<PaymentResponse>> results = new ArrayList<>();

    for (int i = 0; i < requests.size(); i++) {
      final int index = i;
      CreatePaymentRequest request = requests.get(i);

      Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);
      if (!violations.isEmpty()) {
        String errorMsg =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
        results.add(BulkCreateResult.error(index, errorMsg));
        continue;
      }

      try {
        PaymentResponse response =
            txTemplate.execute(status -> performCreatePayment(request, principal));
        results.add(BulkCreateResult.success(index, response));
      } catch (Exception e) {
        log.warn("Bulk payment creation failed for item {}: {}", index, e.getMessage());
        results.add(BulkCreateResult.error(index, extractPaymentErrorMessage(e)));
      }
    }

    return results;
  }

  private PaymentResponse performCreatePayment(
      CreatePaymentRequest request, UserPrincipal principal) {
    UUID teamId = principal.getTeamId();

    Contract contract =
        contractRepository.getByIdentifierAndTeamId(request.contractIdentifier(), teamId);

    if (contract.getStatus() != ACTIVE) {
      throw new BusinessRuleException(
          "Payments can only be created for active contracts. Current status: "
              + contract.getStatus());
    }

    validateCurrencyDecimals(request.amount(), request.currency());

    boolean markAsPaid = Boolean.TRUE.equals(request.markAsPaid());
    @Nullable LocalDate paymentDate =
        markAsPaid
            ? (request.paymentDate() != null ? request.paymentDate() : LocalDate.now(clock))
            : null;

    Payment payment = paymentMapper.toEntity(request);
    payment.setContractId(contract.getId());
    payment.setIdentifier(newPaymentId().value());
    payment.setTeamId(teamId);
    payment.setStatus(markAsPaid ? PAID : PENDING);
    payment.setCreatedBy(principal.getUserId());
    payment.setUpdatedBy(principal.getUserId());
    payment.setCreatedAt(clock.instant());
    payment.setUpdatedAt(clock.instant());

    if (markAsPaid) {
      payment.setPaymentDate(paymentDate);
    }

    if (payment.getCurrency() == null || payment.getCurrency().isEmpty()) {
      payment.setCurrency(contract.getRentAmountCurrency());
    }

    Payment savedPayment = paymentRepository.save(payment);

    if (markAsPaid) {
      PaymentReceival receival = new PaymentReceival();
      receival.setIdentifier(newPaymentReceivalId().value());
      receival.setTeamId(teamId);
      receival.setPaymentId(savedPayment.getId());
      receival.setAmount(savedPayment.getAmount());
      receival.setCurrency(savedPayment.getCurrency());
      receival.setReceivalDate(paymentDate != null ? paymentDate : LocalDate.now(clock));
      receival.setCreatedBy(principal.getUserId());
      receival.setUpdatedBy(principal.getUserId());
      receival.setCreatedAt(clock.instant());
      receival.setUpdatedAt(clock.instant());
      receivalRepository.save(receival);
    }

    metricsService.incrementCounter("payment.total");
    metricsService.recordHistogram(
        "payment.amount",
        savedPayment.getAmount().doubleValue(),
        "currency",
        savedPayment.getCurrency(),
        "status",
        savedPayment.getStatus().name());

    log.info(
        "Created payment {} for contract {} by user {}",
        savedPayment.getIdentifier(),
        contract.getIdentifier(),
        principal.getUserId());

    auditService.logCreate(
        teamId, "PAYMENT", savedPayment.getId(), principal.getUserId(), savedPayment);

    return enrichPaymentResponse(savedPayment, teamId);
  }

  private String extractPaymentErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return "An unexpected error occurred";
    }
    return message;
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPayment(String identifier, UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return enrichPaymentResponse(payment, principal.requireTeamId());
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getAllPayments(UserPrincipal principal) {
    List<Payment> payments = paymentRepository.findAllByTeamId(principal.requireTeamId());

    LocalDate today = LocalDate.now(clock);
    payments.forEach(payment -> updatePaymentStatus(payment, today));

    return enrichPaymentResponses(payments, principal.requireTeamId());
  }

  @Transactional(readOnly = true)
  public PageResponse<PaymentResponse> getPaymentsPaginated(
      UserPrincipal principal,
      @Nullable String status,
      @Nullable String contractIdentifier,
      @Nullable String propertyIdentifier,
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    @Nullable UUID contractId = null;
    if (contractIdentifier != null) {
      Contract contract =
          contractRepository.getByIdentifierAndTeamId(
              contractIdentifier, principal.requireTeamId());
      contractId = contract.getId();
    }
    UUID propertyId = null;
    if (propertyIdentifier != null) {
      var property =
          propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId());
      propertyId = property.getId();
    }
    PaginatedResult<Payment> result =
        paymentRepository.findAllByTeamIdPaginated(
            principal.getTeamId(), status, contractId, propertyId, dateFrom, dateTo, pageRequest);

    LocalDate today = LocalDate.now(clock);
    result.items().forEach(payment -> updatePaymentStatus(payment, today));

    List<PaymentResponse> responses =
        enrichPaymentResponses(result.items(), principal.requireTeamId());
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public PaymentStatsResponse getPaymentStats(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Optional<Record2<Integer, BigDecimal>> pending = paymentRepository.getPendingStats(teamId);
    Optional<Record2<Integer, BigDecimal>> overdue = paymentRepository.getOverdueStats(teamId);
    @Nullable String currency = paymentRepository.findCurrencyByTeamId(teamId).orElse(null);

    List<PaymentStatsResponse.MonthlyTrend> monthlyTrend =
        paymentRepository.getMonthlyPaidTrend(teamId, 12).stream()
            .map(
                r ->
                    new PaymentStatsResponse.MonthlyTrend(
                        r.value1(), CurrencyUtils.sumToMajorUnits(r.value2(), currency)))
            .toList();

    return new PaymentStatsResponse(
        pending.map(Record2::value1).orElse(0),
        CurrencyUtils.sumToMajorUnits(pending.map(Record2::value2).orElse(null), currency),
        overdue.map(Record2::value1).orElse(0),
        CurrencyUtils.sumToMajorUnits(overdue.map(Record2::value2).orElse(null), currency),
        currency,
        monthlyTrend);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getPaymentsByContract(
      String contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);

    LocalDate today = LocalDate.now(clock);
    payments.forEach(payment -> updatePaymentStatus(payment, today));

    return enrichPaymentResponses(payments, teamId);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getOverduePayments(UserPrincipal principal) {
    List<Payment> payments = paymentRepository.findOverduePayments(principal.requireTeamId());

    return enrichPaymentResponses(payments, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse updatePayment(
      String identifier, UpdatePaymentRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Validate contract is still active
    Contract contract = contractRepository.getByIdAndTeamId(payment.getContractId(), teamId);
    if (contract.getStatus() != ACTIVE) {
      throw new BusinessRuleException(
          "Payments can only be edited for active contracts. Current status: "
              + contract.getStatus());
    }

    // Validate currency decimals if amount or currency is being changed
    BigDecimal effectiveAmount = request.amount() != null ? request.amount() : payment.getAmount();
    String effectiveCurrency =
        request.currency() != null ? request.currency() : payment.getCurrency();
    validateCurrencyDecimals(effectiveAmount, effectiveCurrency);

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    paymentMapper.updateEntity(payment, request);
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    Payment updatedPayment = paymentRepository.save(payment);
    PaymentResponse newState = enrichPaymentResponse(updatedPayment, teamId);

    log.info(
        "Updated payment {} by user {}", updatedPayment.getIdentifier(), principal.getUserId());

    auditService.logUpdate(
        teamId,
        "PAYMENT",
        updatedPayment.getId(),
        principal.getUserId(),
        oldState,
        newState,
        auditService.getChangedFields(oldState, newState));

    return newState;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse markPaymentAsPaid(
      String identifier, MarkPaidRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already marked as paid");
    }

    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot mark a cancelled payment as paid");
    }

    // Register a receival for the remaining balance
    String currency = payment.getCurrency();
    BigDecimal receivedAmount =
        receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency);
    BigDecimal remainingBalance = payment.getAmount().subtract(receivedAmount);

    if (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {
      PaymentReceival receival = new PaymentReceival();
      receival.setIdentifier(newPaymentReceivalId().value());
      receival.setTeamId(teamId);
      receival.setPaymentId(payment.getId());
      receival.setAmount(remainingBalance);
      receival.setCurrency(currency);
      receival.setReceivalDate(request.paymentDate());
      receival.setNotes(request.notes());
      receival.setCreatedBy(principal.getUserId());
      receival.setUpdatedBy(principal.getUserId());
      receival.setCreatedAt(clock.instant());
      receival.setUpdatedAt(clock.instant());
      receivalRepository.save(receival);
    }

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    payment.setPaymentDate(request.paymentDate());
    Payment.PaymentStatus oldStatus = payment.getStatus();
    payment.setStatus(PAID);
    if (request.notes() != null && !request.notes().isEmpty()) {
      payment.setNotes(request.notes());
    }
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    Payment updatedPayment = paymentRepository.save(payment);
    PaymentResponse newState = enrichPaymentResponse(updatedPayment, teamId);

    metricsService.incrementCounter("payment.marked.paid.total");
    metricsService.incrementCounter(
        "payment.status.changed.total", "from_status", oldStatus.name(), "to_status", "PAID");

    log.info(
        "Marked payment {} as PAID on {} by user {}",
        updatedPayment.getIdentifier(),
        request.paymentDate(),
        principal.getUserId());

    auditService.logUpdate(
        teamId,
        "PAYMENT",
        updatedPayment.getId(),
        principal.getUserId(),
        oldState,
        newState,
        auditService.getChangedFields(oldState, newState));

    sendPaymentPaidNotification(updatedPayment, teamId, principal);

    return newState;
  }

  // --- Receival operations ---

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse registerReceival(
      String paymentIdentifier, CreatePaymentReceivalRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already fully paid");
    }

    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot register receival on a cancelled payment");
    }

    String currency = payment.getCurrency();
    validateCurrencyDecimals(request.amount(), currency);

    // Validate amount does not exceed balance
    BigDecimal currentReceived =
        receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId, currency);
    BigDecimal currentBalance = payment.getAmount().subtract(currentReceived);

    if (request.amount().compareTo(currentBalance) > 0) {
      throw new BusinessRuleException(
          "Receival amount ("
              + request.amount()
              + ") exceeds remaining balance ("
              + currentBalance
              + ")");
    }

    PaymentReceival receival = new PaymentReceival();
    receival.setIdentifier(newPaymentReceivalId().value());
    receival.setTeamId(teamId);
    receival.setPaymentId(paymentId);
    receival.setAmount(request.amount());
    receival.setCurrency(currency);
    receival.setReceivalDate(request.receivalDate());
    receival.setNotes(request.notes());
    receival.setCreatedBy(principal.getUserId());
    receival.setUpdatedBy(principal.getUserId());
    receival.setCreatedAt(clock.instant());
    receival.setUpdatedAt(clock.instant());

    receivalRepository.save(receival);

    metricsService.incrementCounter("payment.receival.total", "action", "registered");

    // Recalculate payment status
    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    log.info(
        "Registered receival of {} for payment {} by user {}",
        request.amount(),
        payment.getIdentifier(),
        principal.getUserId());

    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put("receivalRegistered", request.amount() + " on " + request.receivalDate());
    auditService.logUpdate(
        teamId, "PAYMENT", paymentId, principal.getUserId(), oldState, newState, changedFields);

    sendReceivalNotification(payment, request.amount(), teamId, principal);

    if (payment.getStatus() == PAID) {
      sendPaymentPaidNotification(payment, teamId, principal);
    }

    return newState;
  }

  @Transactional(readOnly = true)
  public List<PaymentReceivalResponse> getReceivalsForPayment(
      String paymentIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    return receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId).stream()
        .map(receivalMapper::toResponse)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse updateReceival(
      String paymentIdentifier,
      String receivalIdentifier,
      UpdatePaymentReceivalRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    String currency = payment.getCurrency();
    validateCurrencyDecimals(request.amount(), currency);

    PaymentReceival receival =
        receivalRepository.getByIdentifierAndPaymentIdAndTeamId(
            receivalIdentifier, paymentId, teamId);

    // Validate new amount: total received minus old amount plus new amount must not exceed payment
    // amount
    BigDecimal currentReceived =
        receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId, currency);
    BigDecimal receivedWithoutThis = currentReceived.subtract(receival.getAmount());
    BigDecimal newBalance =
        payment.getAmount().subtract(receivedWithoutThis).subtract(request.amount());

    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException(
          "Updated receival amount (" + request.amount() + ") would exceed the payment amount");
    }

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    BigDecimal oldAmount = receival.getAmount();
    receivalRepository.update(
        receival.getId(),
        teamId,
        request.amount(),
        request.receivalDate(),
        request.notes(),
        principal.getUserId(),
        currency);

    // Recalculate payment status
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    metricsService.incrementCounter("payment.receival.total", "action", "updated");

    log.info(
        "Updated receival {} for payment {} by user {}",
        receivalIdentifier,
        payment.getIdentifier(),
        principal.getUserId());

    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put(
        "receivalUpdated", oldAmount + " -> " + request.amount() + " on " + request.receivalDate());
    auditService.logUpdate(
        teamId, "PAYMENT", paymentId, principal.getUserId(), oldState, newState, changedFields);

    return newState;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse deleteReceival(
      String paymentIdentifier, String receivalIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    PaymentReceival receival =
        receivalRepository.getByIdentifierAndPaymentIdAndTeamId(
            receivalIdentifier, paymentId, teamId);

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    receivalRepository.softDeleteByIdAndTeamId(receival.getId(), teamId);

    metricsService.incrementCounter("payment.receival.total", "action", "deleted");

    // Recalculate payment status
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    log.info(
        "Deleted receival {} for payment {} by user {}",
        receivalIdentifier,
        payment.getIdentifier(),
        principal.getUserId());

    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put(
        "receivalDeleted", receival.getAmount() + " from " + receival.getReceivalDate());
    auditService.logUpdate(
        teamId, "PAYMENT", paymentId, principal.getUserId(), oldState, newState, changedFields);

    return newState;
  }

  private void recalculatePaymentStatus(Payment payment, UserPrincipal principal) {
    String currency = payment.getCurrency();
    BigDecimal totalReceived =
        receivalRepository.sumByPaymentIdAndTeamId(
            payment.getId(), principal.requireTeamId(), currency);
    BigDecimal balance = payment.getAmount().subtract(totalReceived);

    Payment.PaymentStatus newStatus;
    if (balance.compareTo(BigDecimal.ZERO) <= 0) {
      newStatus = PAID;
      // Set payment date to the latest receival date
      List<PaymentReceival> receivals =
          receivalRepository.findByPaymentIdAndTeamId(payment.getId(), principal.requireTeamId());
      if (!receivals.isEmpty()) {
        LocalDate latestDate =
            receivals.stream()
                .map(PaymentReceival::getReceivalDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now(clock));
        payment.setPaymentDate(latestDate);
      }
    } else if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
      newStatus = PARTIALLY_PAID;
      payment.setPaymentDate(null);
    } else {
      // No receivals - check if overdue
      if (payment.getDueDate().isBefore(LocalDate.now(clock))) {
        newStatus = OVERDUE;
      } else {
        newStatus = PENDING;
      }
      payment.setPaymentDate(null);
    }

    payment.setStatus(newStatus);
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());
    paymentRepository.save(payment);
  }

  // --- End receival operations ---

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deletePayment(String identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    UUID paymentId = payment.getId();

    // Cascade: soft-delete receivals
    receivalRepository.softDeleteByPaymentIdAndTeamId(paymentId, teamId);

    // Cascade: delete documents and S3 files
    List<com.buurman.domain.Document> docs =
        documentRepository.findByEntityAndTeamId("PAYMENT", paymentId, teamId);
    for (com.buurman.domain.Document doc : docs) {
      s3StorageService.deleteFile(doc.getFileKey());
    }
    documentRepository.softDeleteByEntityAndTeamId("PAYMENT", paymentId, teamId);

    // Clean up audit logs
    auditLogRepository.deleteByEntityAndTeamId("PAYMENT", paymentId, teamId);

    // Soft-delete the payment itself
    paymentRepository.softDeleteByIdAndTeamId(paymentId, teamId);

    log.info("Deleted payment {} by user {}", payment.getIdentifier(), principal.getUserId());

    auditService.logDelete(teamId, "PAYMENT", paymentId, principal.getUserId(), payment);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<PaymentResponse> bulkGeneratePayments(
      BulkGeneratePaymentsRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    YearMonth month = YearMonth.parse(request.forMonth());

    List<Contract> activeContracts = contractRepository.findByStatus(ACTIVE, teamId);

    if (activeContracts.isEmpty()) {
      log.warn("No active contracts found for team {} to generate payments", teamId);
      return List.of();
    }

    List<Payment> generatedPayments = new ArrayList<>();

    for (Contract contract : activeContracts) {
      LocalDate dueDate = calculateDueDate(month, contract);

      List<Payment> existingPayments = paymentRepository.findByContractId(contract.getId(), teamId);
      boolean paymentExists =
          existingPayments.stream().anyMatch(p -> p.getDueDate().equals(dueDate));

      if (paymentExists) {
        log.debug(
            "Payment already exists for contract {} on {}, skipping",
            contract.getIdentifier(),
            dueDate);
        continue;
      }

      Payment payment = new Payment();
      payment.setIdentifier(newPaymentId().value());
      payment.setTeamId(teamId);
      payment.setContractId(contract.getId());
      payment.setAmount(contract.getRentAmount());
      payment.setCurrency(contract.getRentAmountCurrency());
      payment.setDueDate(dueDate);
      payment.setStatus(PENDING);
      payment.setNotes("Auto-generated for " + month);
      payment.setCreatedBy(principal.getUserId());
      payment.setUpdatedBy(principal.getUserId());
      payment.setCreatedAt(clock.instant());
      payment.setUpdatedAt(clock.instant());

      Payment savedPayment = paymentRepository.save(payment);
      generatedPayments.add(savedPayment);

      log.info(
          "Generated payment {} for contract {} due on {}",
          savedPayment.getIdentifier(),
          contract.getIdentifier(),
          dueDate);
    }

    if (!generatedPayments.isEmpty()) {
      metricsService.incrementCounter("payment.bulk.generated.total");
      metricsService.recordHistogram("payment.bulk.generated.count", generatedPayments.size());

      auditService.logCreate(
          teamId,
          "BULK_PAYMENT_GENERATION",
          generatedPayments.get(0).getId(),
          principal.getUserId(),
          "Generated " + generatedPayments.size() + " payments for " + month);
    }

    return enrichPaymentResponses(generatedPayments, teamId);
  }

  // --- Document delegation methods (resolve identifier to UUID) ---

  public DocumentResponse uploadDocument(
      String paymentIdentifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file, "PAYMENT", payment.getId(), payment.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getDocuments(String paymentIdentifier, UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return documentService.getDocuments("PAYMENT", payment.getId(), principal);
  }

  public URL getDocumentDownloadUrl(String documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    return documentService.getDownloadUrl(document.getIdentifier(), principal);
  }

  public void deleteDocument(String documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    documentService.deleteDocument(document.getIdentifier(), principal);
  }

  public List<RecentActivityResponse> getAuditLog(
      String paymentIdentifier, UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "PAYMENT", payment.getId());
  }

  // --- Helper methods ---

  private LocalDate calculateDueDate(YearMonth month, Contract contract) {
    @Nullable Integer paymentDueDay = contract.getPaymentDueDay();
    if (paymentDueDay == null) {
      paymentDueDay = 1;
    }

    int maxDayInMonth = month.lengthOfMonth();
    int actualDay = Math.min(paymentDueDay, maxDayInMonth);

    return month.atDay(actualDay);
  }

  private void updatePaymentStatus(Payment payment, LocalDate today) {
    if (payment.getStatus() == PENDING && payment.getDueDate().isBefore(today)) {
      payment.setStatus(OVERDUE);
    }
  }

  private void sendPaymentPaidNotification(Payment payment, UUID teamId, UserPrincipal principal) {
    @Nullable Contract contract =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
    String propertyName = "N/A";
    String tenantName = "N/A";
    if (contract != null) {
      @Nullable Property property =
          propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
      @Nullable Tenant tenant =
          contractPartyService.findPrimaryTenantForContract(contract.getId(), teamId).orElse(null);
      if (property != null) {
        propertyName =
            property.getStreet() != null
                ? property.getStreet() + ", " + property.getCity()
                : property.getIdentifier();
      }
      if (tenant != null) {
        tenantName = tenant.getFirstName() + " " + tenant.getLastName();
      }
    }
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(teamId)
            .notificationType(NotificationType.PAYMENT_PAID)
            .templateName("payment-paid")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "tenantName",
                    tenantName,
                    "amount",
                    payment.getCurrency() + " " + payment.getAmount(),
                    "paymentDate",
                    payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "N/A",
                    "baseUrl",
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());
  }

  private void sendReceivalNotification(
      Payment payment, BigDecimal receivalAmount, UUID teamId, UserPrincipal principal) {
    @Nullable Contract contract =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
    String propertyName = "N/A";
    String tenantName = "N/A";
    if (contract != null) {
      @Nullable Property property =
          propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
      @Nullable Tenant tenant =
          contractPartyService.findPrimaryTenantForContract(contract.getId(), teamId).orElse(null);
      if (property != null) {
        propertyName =
            property.getStreet() != null
                ? property.getStreet() + ", " + property.getCity()
                : property.getIdentifier();
      }
      if (tenant != null) {
        tenantName = tenant.getFirstName() + " " + tenant.getLastName();
      }
    }
    String currency = payment.getCurrency();
    BigDecimal totalReceived =
        receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency);
    BigDecimal remainingBalance = payment.getAmount().subtract(totalReceived);

    Map<String, Object> vars = new HashMap<>();
    vars.put("propertyName", propertyName);
    vars.put("tenantName", tenantName);
    vars.put("receivalAmount", currency + " " + receivalAmount);
    vars.put("amount", currency + " " + payment.getAmount());
    vars.put("remainingBalance", currency + " " + remainingBalance);
    vars.put("baseUrl", appProperties.email().baseUrl());

    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(teamId)
            .notificationType(NotificationType.PAYMENT_RECEIVAL)
            .templateName("payment-receival")
            .templateVariables(vars)
            .createdBy(principal.getUserId())
            .build());
  }

  private List<PaymentResponse> enrichPaymentResponses(List<Payment> payments, UUID teamId) {
    if (payments.isEmpty()) {
      return List.of();
    }

    List<UUID> paymentIds = payments.stream().map(Payment::getId).toList();

    // Batch-fetch receivals
    List<PaymentReceival> allReceivals =
        receivalRepository.findByPaymentIdsAndTeamId(paymentIds, teamId);
    Map<UUID, List<PaymentReceival>> receivalsByPaymentId =
        allReceivals.stream().collect(groupingBy(PaymentReceival::getPaymentId));

    // Batch-fetch contracts
    Set<UUID> contractIds = payments.stream().map(Payment::getContractId).collect(toSet());
    Map<UUID, Contract> contractsById =
        contractRepository.findByIdsAndTeamId(contractIds, teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    // Batch-fetch properties from contracts
    Set<UUID> propertyIds =
        contractsById.values().stream()
            .map(Contract::getPropertyId)
            .filter(java.util.Objects::nonNull)
            .collect(toSet());

    Map<UUID, com.buurman.domain.Property> propertiesById =
        propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
            .collect(toMap(com.buurman.domain.Property::getId, identity()));

    // Batch-fetch primary tenants via contract_parties
    Map<UUID, com.buurman.domain.Tenant> primaryTenantByContract =
        contractPartyService.getPrimaryTenantsForContracts(contractIds, teamId);

    // Batch-fetch documents for all payments
    List<com.buurman.domain.Document> allDocs =
        documentRepository.findByEntityTypeAndEntityIdsAndTeamId("PAYMENT", paymentIds, teamId);
    Map<UUID, List<com.buurman.domain.Document>> docsByPaymentId =
        allDocs.stream().collect(groupingBy(com.buurman.domain.Document::getEntityId));

    // Build responses
    List<PaymentResponse> responses = new ArrayList<>(payments.size());
    for (Payment payment : payments) {
      PaymentResponse base = paymentMapper.toResponse(payment);

      List<PaymentReceival> receivals =
          receivalsByPaymentId.getOrDefault(payment.getId(), List.of());
      BigDecimal receivedAmount =
          receivals.stream()
              .map(PaymentReceival::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal balance = payment.getAmount().subtract(receivedAmount);
      List<PaymentReceivalResponse> receivalResponses =
          receivals.stream().map(receivalMapper::toResponse).toList();

      @Nullable Contract contract = contractsById.get(payment.getContractId());
      @Nullable ContractSummary contractSummary =
          contract != null ? contractMapper.toSummary(contract) : null;
      @Nullable PropertySummary propertySummary = null;
      @Nullable TenantSummary tenantSummary = null;

      if (contract != null) {
        com.buurman.domain.@Nullable Property property =
            contract.getPropertyId() != null ? propertiesById.get(contract.getPropertyId()) : null;
        com.buurman.domain.@Nullable Tenant tenant = primaryTenantByContract.get(contract.getId());
        if (property != null) {
          propertySummary = propertyMapper.toSummary(property);
        }
        if (tenant != null) {
          tenantSummary = tenantMapper.toSummary(tenant);
        }
      }

      List<com.buurman.domain.Document> paymentDocs =
          docsByPaymentId.getOrDefault(payment.getId(), List.of());
      @Nullable DocumentResponse proofOfPayment =
          paymentDocs.stream()
              .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Proof"))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);
      @Nullable DocumentResponse receipt =
          paymentDocs.stream()
              .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Receipt"))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      responses.add(
          new PaymentResponse(
              base.identifier(),
              contractSummary,
              tenantSummary,
              propertySummary,
              base.amount(),
              base.currency(),
              receivedAmount,
              balance,
              base.paymentDate(),
              base.dueDate(),
              base.status(),
              base.notes(),
              proofOfPayment,
              receipt,
              receivalResponses,
              base.createdAt(),
              base.updatedAt()));
    }
    return responses;
  }

  private PaymentResponse enrichPaymentResponse(Payment payment, UUID teamId) {
    PaymentResponse response = paymentMapper.toResponse(payment);

    // Get receivals and calculate balance
    List<PaymentReceival> receivals =
        receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId);
    BigDecimal receivedAmount =
        receivals.stream().map(PaymentReceival::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal balance = payment.getAmount().subtract(receivedAmount);
    List<PaymentReceivalResponse> receivalResponses =
        receivals.stream().map(receivalMapper::toResponse).toList();

    @Nullable Contract contract =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);

    if (contract != null) {
      ContractSummary contractSummary = contractMapper.toSummary(contract);

      @Nullable PropertySummary propertySummary =
          propertyRepository
              .findByIdAndTeamId(contract.getPropertyId(), teamId)
              .map(propertyMapper::toSummary)
              .orElse(null);

      @Nullable TenantSummary tenantSummary =
          contractPartyService
              .findPrimaryTenantForContract(contract.getId(), teamId)
              .map(tenantMapper::toSummary)
              .orElse(null);

      @Nullable DocumentResponse proofOfPayment =
          documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId).stream()
              .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Proof"))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      @Nullable DocumentResponse receipt =
          documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId).stream()
              .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Receipt"))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      return new PaymentResponse(
          response.identifier(),
          contractSummary,
          tenantSummary,
          propertySummary,
          response.amount(),
          response.currency(),
          receivedAmount,
          balance,
          response.paymentDate(),
          response.dueDate(),
          response.status(),
          response.notes(),
          proofOfPayment,
          receipt,
          receivalResponses,
          response.createdAt(),
          response.updatedAt());
    }

    return new PaymentResponse(
        response.identifier(),
        response.contract(),
        response.tenant(),
        response.property(),
        response.amount(),
        response.currency(),
        receivedAmount,
        balance,
        response.paymentDate(),
        response.dueDate(),
        response.status(),
        response.notes(),
        response.proofOfPayment(),
        response.receipt(),
        receivalResponses,
        response.createdAt(),
        response.updatedAt());
  }

  private void validateCurrencyDecimals(
      @Nullable BigDecimal amount, @Nullable String currencyCode) {
    if (amount == null || currencyCode == null || currencyCode.isBlank()) {
      return;
    }
    if (!CurrencyUtils.isAmountValidForCurrency(amount, currencyCode)) {
      int allowed = CurrencyUtils.getFractionalDigits(currencyCode);
      throw new BusinessRuleException(
          String.format(
              "%s amounts cannot have more than %d decimal place%s",
              currencyCode, allowed, allowed == 1 ? "" : "s"));
    }
  }
}
