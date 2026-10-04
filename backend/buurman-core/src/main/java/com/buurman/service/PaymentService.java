package com.buurman.service;

import static com.buurman.domain.NotificationType.PAYMENT_PAID;
import static com.buurman.domain.NotificationType.PAYMENT_RECEIVAL;
import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.SidGenerator.newContactCreditId;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;
import static java.math.BigDecimal.ZERO;
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
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.AmountStats;
import com.buurman.domain.Contact;
import com.buurman.domain.ContactCredit;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.Document;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentType;
import com.buurman.domain.PaymentPlan;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.domain.identifier.PaymentReceivalIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.ApplyCreditRequest;
import com.buurman.dto.request.BulkCancelPaymentsRequest;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.BulkMarkPaidRequest;
import com.buurman.dto.request.CancelPaymentRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.request.WriteOffPaymentRequest;
import com.buurman.dto.response.BulkActionResult;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.ContractSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentStatsResponse;
import com.buurman.dto.response.PaymentSummary;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PaymentMapper;
import com.buurman.mapper.PaymentReceivalMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContactCreditRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PaymentPlanRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;
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
  private final ContactCreditRepository creditRepository;
  private final PaymentPlanRepository paymentPlanRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final ContractTerminationRepository contractTerminationRepository;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;
  private final ContractPartyService contractPartyService;
  private final CurrencyEnforcementService currencyEnforcement;
  private final DocumentRepository documentRepository;
  private final PaymentMapper paymentMapper;
  private final PaymentReceivalMapper receivalMapper;
  private final ContractMapper contractMapper;
  private final PropertyMapper propertyMapper;
  private final ContactMapper contactMapper;
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
      CreatePaymentRequest request = requests.get(i);

      Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);
      if (!violations.isEmpty()) {
        String errorMsg =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
        results.add(BulkCreateResult.error(i, errorMsg));
        continue;
      }

      try {
        PaymentResponse response =
            txTemplate.execute(status -> performCreatePayment(request, principal));
        results.add(BulkCreateResult.success(i, response));
      } catch (Exception e) {
        log.warn("Bulk payment creation failed for item {}: {}", i, e.getMessage());
        results.add(BulkCreateResult.error(i, extractPaymentErrorMessage(e)));
      }
    }

    return results;
  }

  private PaymentResponse performCreatePayment(
      CreatePaymentRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract =
        contractRepository.getByIdentifierAndTeamId(request.contractIdentifier(), teamId);

    if (!contract.getStatus().isInForce()) {
      throw new BusinessRuleException(
          "Payments can only be created for in-force contracts. Current status: "
              + contract.getStatus());
    }

    validateCurrencyDecimals(request.amount(), request.currency());
    currencyEnforcement.validateCurrency(request.currency(), teamId);

    boolean markAsPaid = request.markAsPaid().orElse(false);
    LocalDate paymentDate = markAsPaid ? request.paymentDate().orElse(LocalDate.now(clock)) : null;

    Optional<UUID> contactId =
        request
            .contactIdentifier()
            .map(cid -> contactRepository.getByIdentifierAndTeamId(cid, teamId).getId());

    Payment payment = paymentMapper.toEntity(request);
    payment.setContractId(contract.getId());
    payment.setContactId(contactId);
    payment.setIdentifier(Optional.of(newPaymentId()));
    payment.setTeamId(teamId);
    payment.setStatus(markAsPaid ? PAID : PENDING);
    payment.setCreatedBy(principal.getUserId());
    payment.setUpdatedBy(principal.getUserId());
    payment.setCreatedAt(clock.instant());
    payment.setUpdatedAt(clock.instant());

    if (markAsPaid) {
      payment.setPaymentDate(Optional.ofNullable(paymentDate));
    }

    if (payment.getAmount() == null
        || payment.getAmount().currency() == null
        || payment.getAmount().currency().isEmpty()) {
      payment.setAmount(
          MoneyAmount.of(payment.getAmount().value(), contract.getRentAmount().currency()));
    }

    Payment savedPayment = paymentRepository.save(payment);

    if (markAsPaid) {
      PaymentReceival receival = new PaymentReceival();
      receival.setIdentifier(Optional.of(newPaymentReceivalId()));
      receival.setTeamId(teamId);
      receival.setPaymentId(savedPayment.getId());
      receival.setAmount(savedPayment.getAmount());
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
        savedPayment.getAmount().value().doubleValue(),
        "currency",
        savedPayment.getAmount().currency(),
        "status",
        savedPayment.getStatus().name());

    log.info(
        "Created payment {} for contract {} by user {}",
        savedPayment.getIdentifier().orElseThrow(),
        contract.getIdentifier().orElseThrow(),
        principal.getUserId());

    auditService.logCreate(
        teamId, "PAYMENT", savedPayment.getId(), principal.getUserId(), savedPayment);

    return enrichPaymentResponse(savedPayment, teamId);
  }

  /** Only rule/validation messages are safe to echo back; anything else stays generic. */
  private static String extractPaymentErrorMessage(Exception e) {
    boolean clientFacing =
        e instanceof BusinessRuleException
            || e instanceof BadRequestException
            || e instanceof NotFoundException
            || e instanceof jakarta.validation.ValidationException;
    String message = e.getMessage();
    if (!clientFacing || message == null || message.isBlank()) {
      return "An unexpected error occurred";
    }
    return message;
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPayment(PaymentIdentifier identifier, UserPrincipal principal) {
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
      @Nullable ContractIdentifier contractIdentifier,
      @Nullable PropertyIdentifier propertyIdentifier,
      @Nullable ContactIdentifier contactIdentifier,
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    UUID contractId = null;
    if (contractIdentifier != null) {
      Contract contract =
          contractRepository.getByIdentifierAndTeamId(
              contractIdentifier, principal.requireTeamId());
      contractId = contract.getId();
    }
    UUID propertyId = null;
    if (propertyIdentifier != null) {
      var property =
          propertyRepository.getByIdentifierAndTeamId(
              propertyIdentifier, principal.requireTeamId());
      propertyId = property.getId();
    }
    UUID contactId = null;
    if (contactIdentifier != null) {
      var contact =
          contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
      contactId = contact.getId();
    }
    PaginatedResult<Payment> result =
        paymentRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(),
            status,
            contractId,
            propertyId,
            contactId,
            dateFrom,
            dateTo,
            pageRequest);

    LocalDate today = LocalDate.now(clock);
    result.items().forEach(payment -> updatePaymentStatus(payment, today));

    List<PaymentResponse> responses =
        enrichPaymentResponses(result.items(), principal.requireTeamId());
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public PaymentStatsResponse getPaymentStats(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Optional<AmountStats> pending = paymentRepository.getPendingStats(teamId);
    Optional<AmountStats> overdue = paymentRepository.getOverdueStats(teamId);
    String currency = paymentRepository.findCurrencyByTeamId(teamId).orElse(null);

    List<PaymentStatsResponse.MonthlyTrend> monthlyTrend =
        paymentRepository.getMonthlyPaidTrend(teamId, 12).stream()
            .map(
                r ->
                    new PaymentStatsResponse.MonthlyTrend(
                        r.month(), MoneyAmount.sumToMajorUnits(r.amount().orElse(null), currency)))
            .toList();

    return new PaymentStatsResponse(
        pending.map(AmountStats::count).orElse(0),
        MoneyAmount.sumToMajorUnits(pending.flatMap(AmountStats::total).orElse(null), currency),
        overdue.map(AmountStats::count).orElse(0),
        MoneyAmount.sumToMajorUnits(overdue.flatMap(AmountStats::total).orElse(null), currency),
        Optional.ofNullable(currency),
        monthlyTrend);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getOverduePayments(UserPrincipal principal) {
    List<Payment> payments = paymentRepository.findOverduePayments(principal.requireTeamId());

    return enrichPaymentResponses(payments, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse updatePayment(
      PaymentIdentifier identifier, UpdatePaymentRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Validate contract is still in force
    Contract contract = contractRepository.getByIdAndTeamId(payment.getContractId(), teamId);
    if (!contract.getStatus().isInForce()) {
      throw new BusinessRuleException(
          "Payments can only be edited for in-force contracts. Current status: "
              + contract.getStatus());
    }

    // Validate currency decimals if amount or currency is being changed
    BigDecimal effectiveAmount = request.amount().orElse(payment.getAmount().value());
    String effectiveCurrency = request.currency().orElse(payment.getAmount().currency());
    validateCurrencyDecimals(effectiveAmount, effectiveCurrency);
    request.currency().ifPresent(c -> currencyEnforcement.validateCurrency(c, teamId));

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    paymentMapper.updateEntity(payment, request);
    request
        .contactIdentifier()
        .ifPresent(
            cid -> {
              UUID resolvedContactId =
                  contactRepository.getByIdentifierAndTeamId(cid, teamId).getId();
              payment.setContactId(Optional.of(resolvedContactId));
            });
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    Payment updatedPayment = paymentRepository.save(payment);
    PaymentResponse newState = enrichPaymentResponse(updatedPayment, teamId);

    log.info(
        "Updated payment {} by user {}",
        updatedPayment.getIdentifier().orElseThrow(),
        principal.getUserId());

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
      PaymentIdentifier identifier, MarkPaidRequest request, UserPrincipal principal) {
    return performMarkPaid(identifier, request, principal);
  }

  /**
   * Marks each payment paid in its own transaction and reports per-identifier outcomes, so one
   * already-paid or cancelled payment in the selection does not roll back the others.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkActionResult<PaymentResponse>> bulkMarkPaymentsAsPaid(
      BulkMarkPaidRequest request, UserPrincipal principal) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    MarkPaidRequest single = new MarkPaidRequest(request.paymentDate(), request.notes());
    List<BulkActionResult<PaymentResponse>> results = new ArrayList<>();
    for (PaymentIdentifier identifier : request.identifiers().stream().distinct().toList()) {
      try {
        PaymentResponse response =
            txTemplate.execute(status -> performMarkPaid(identifier, single, principal));
        results.add(BulkActionResult.success(identifier.value(), response));
      } catch (Exception e) {
        log.warn("Bulk mark-paid failed for payment {}: {}", identifier, e.getMessage());
        results.add(BulkActionResult.error(identifier.value(), extractPaymentErrorMessage(e)));
      }
    }
    metricsService.incrementCounterBy(
        "payment.bulk.marked.paid.total",
        (double) results.stream().filter(BulkActionResult::isSuccess).count());
    return results;
  }

  private PaymentResponse performMarkPaid(
      PaymentIdentifier identifier, MarkPaidRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already marked as paid");
    }

    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot mark a cancelled payment as paid");
    }

    // Register a receival for the remaining balance
    String currency = payment.getAmount().currency();
    BigDecimal receivedAmount =
        receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency);
    BigDecimal remainingBalance = payment.getAmount().value().subtract(receivedAmount);

    if (remainingBalance.compareTo(ZERO) > 0) {
      PaymentReceival receival = new PaymentReceival();
      receival.setIdentifier(Optional.of(newPaymentReceivalId()));
      receival.setTeamId(teamId);
      receival.setPaymentId(payment.getId());
      receival.setAmount(MoneyAmount.of(remainingBalance, currency));
      receival.setReceivalDate(request.paymentDate());
      receival.setNotes(request.notes());
      receival.setCreatedBy(principal.getUserId());
      receival.setUpdatedBy(principal.getUserId());
      receival.setCreatedAt(clock.instant());
      receival.setUpdatedAt(clock.instant());
      receivalRepository.save(receival);
    }

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    payment.setPaymentDate(Optional.of(request.paymentDate()));
    Payment.PaymentStatus oldStatus = payment.getStatus();
    payment.setStatus(PAID);
    request.notes().filter(n -> !n.isEmpty()).ifPresent(n -> payment.setNotes(Optional.of(n)));
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    Payment updatedPayment = paymentRepository.save(payment);
    PaymentResponse newState = enrichPaymentResponse(updatedPayment, teamId);

    metricsService.incrementCounter("payment.marked.paid.total");
    metricsService.incrementCounter(
        "payment.status.changed.total", "from_status", oldStatus.name(), "to_status", "PAID");
    metricsService.incrementCounterBy(
        "payment.amount.paid.total",
        payment.getAmount().value().doubleValue(),
        "currency",
        payment.getAmount().currency());

    log.info(
        "Marked payment {} as PAID on {} by user {}",
        updatedPayment.getIdentifier().orElseThrow(),
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

    refreshPlanCompletion(updatedPayment, teamId, principal.getUserId());
    sendPaymentPaidNotification(updatedPayment, teamId, principal);

    return newState;
  }

  // --- Receival operations ---

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse registerReceival(
      PaymentIdentifier paymentIdentifier,
      CreatePaymentReceivalRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already fully paid");
    }

    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot register receival on a cancelled payment");
    }

    String currency = payment.getAmount().currency();
    validateCurrencyDecimals(request.amount(), currency);

    // Validate amount does not exceed balance
    BigDecimal currentReceived =
        receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId, currency);
    BigDecimal currentBalance = payment.getAmount().value().subtract(currentReceived);

    // An amount above the open balance is not an error: the balance is settled and the
    // excess becomes a credit the tenant can use on a later payment or have refunded.
    BigDecimal applied = request.amount().min(currentBalance);
    BigDecimal excess = request.amount().subtract(currentBalance);
    Optional<UUID> creditContactId =
        excess.signum() > 0
            ? Optional.of(
                payment
                    .getContactId()
                    .or(
                        () ->
                            contractPartyService
                                .findPrimaryContactForContract(payment.getContractId(), teamId)
                                .map(Contact::getId))
                    .orElseThrow(
                        () ->
                            new BusinessRuleException(
                                "Receival amount ("
                                    + request.amount()
                                    + ") exceeds remaining balance ("
                                    + currentBalance
                                    + ") and no tenant contact is linked to hold the credit")))
            : Optional.empty();

    PaymentReceival receival = new PaymentReceival();
    receival.setIdentifier(Optional.of(newPaymentReceivalId()));
    receival.setTeamId(teamId);
    receival.setPaymentId(paymentId);
    receival.setAmount(MoneyAmount.of(applied, currency));
    receival.setReceivalDate(request.receivalDate());
    receival.setNotes(request.notes());
    receival.setCreatedBy(principal.getUserId());
    receival.setUpdatedBy(principal.getUserId());
    receival.setCreatedAt(clock.instant());
    receival.setUpdatedAt(clock.instant());

    receivalRepository.save(receival);

    metricsService.incrementCounter("payment.receival.total", "action", "registered");

    creditContactId.ifPresent(
        contactId -> {
          ContactCredit credit =
              ContactCredit.builder()
                  .identifier(Optional.of(newContactCreditId()))
                  .teamId(teamId)
                  .contactId(contactId)
                  .contractId(Optional.of(payment.getContractId()))
                  .amount(MoneyAmount.of(excess, currency))
                  .remainingAmount(excess)
                  .source(ContactCredit.CreditSource.OVERPAYMENT)
                  .reason(
                      Optional.of(
                          "Overpayment on payment "
                              + payment.getIdentifier().map(Object::toString).orElse("")))
                  .sourcePaymentId(Optional.of(paymentId))
                  .sourceReceivalId(Optional.ofNullable(receival.getId()))
                  .createdBy(principal.getUserId())
                  .updatedBy(principal.getUserId())
                  .build();
          ContactCredit savedCredit = creditRepository.save(credit);
          auditService.logCreate(
              teamId, "CONTACT_CREDIT", savedCredit.getId(), principal.getUserId(), savedCredit);
          metricsService.incrementCounter("contact.credit.created.total", "source", "OVERPAYMENT");
          log.info(
              "Overpayment of {} {} on payment {} recorded as credit {}",
              excess,
              currency,
              payment.getIdentifier().orElseThrow(),
              savedCredit.getIdentifier().orElseThrow());
        });

    // Recalculate payment status
    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    log.info(
        "Registered receival of {} for payment {} by user {}",
        request.amount(),
        payment.getIdentifier().orElseThrow(),
        principal.getUserId());

    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put("receivalRegistered", applied + " on " + request.receivalDate());
    if (excess.signum() > 0) {
      changedFields.put("overpaymentCredited", excess.toPlainString());
    }
    auditService.logUpdate(
        teamId, "PAYMENT", paymentId, principal.getUserId(), oldState, newState, changedFields);

    sendReceivalNotification(payment, applied, teamId, principal);

    if (payment.getStatus() == PAID) {
      refreshPlanCompletion(payment, teamId, principal.getUserId());
      sendPaymentPaidNotification(payment, teamId, principal);
    }

    return newState;
  }

  @Transactional(readOnly = true)
  public List<PaymentReceivalResponse> getReceivalsForPayment(
      PaymentIdentifier paymentIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    return receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId).stream()
        .map(receivalMapper::toResponse)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse updateReceival(
      PaymentIdentifier paymentIdentifier,
      PaymentReceivalIdentifier receivalIdentifier,
      UpdatePaymentReceivalRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    String currency = payment.getAmount().currency();
    validateCurrencyDecimals(request.amount(), currency);

    PaymentReceival receival =
        receivalRepository.getByIdentifierAndPaymentIdAndTeamId(
            receivalIdentifier, paymentId, teamId);
    requireManualReceival(receival);
    creditRepository
        .findBySourceReceivalIdAndTeamId(receival.getId(), teamId)
        .ifPresent(
            c -> {
              throw new BusinessRuleException(
                  "This receival created tenant credit "
                      + c.getIdentifier().map(Object::toString).orElse("")
                      + "; delete it and register the corrected amount instead");
            });

    // Validate new amount: total received minus old amount plus new amount must not exceed payment
    // amount
    BigDecimal currentReceived =
        receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId, currency);
    BigDecimal receivedWithoutThis = currentReceived.subtract(receival.getAmount().value());
    BigDecimal newBalance =
        payment.getAmount().value().subtract(receivedWithoutThis).subtract(request.amount());

    if (newBalance.compareTo(ZERO) < 0) {
      throw new BusinessRuleException(
          "Updated receival amount (" + request.amount() + ") would exceed the payment amount");
    }

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    BigDecimal oldAmount = receival.getAmount().value();
    receivalRepository.update(
        receival.getId(),
        teamId,
        request.amount(),
        request.receivalDate(),
        request.notes().orElse(null),
        principal.getUserId(),
        currency);

    // Recalculate payment status
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    metricsService.incrementCounter("payment.receival.total", "action", "updated");

    log.info(
        "Updated receival {} for payment {} by user {}",
        receivalIdentifier,
        payment.getIdentifier().orElseThrow(),
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
      PaymentIdentifier paymentIdentifier,
      PaymentReceivalIdentifier receivalIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);

    UUID paymentId = payment.getId();

    PaymentReceival receival =
        receivalRepository.getByIdentifierAndPaymentIdAndTeamId(
            receivalIdentifier, paymentId, teamId);
    requireManualReceival(receival);
    Optional<ContactCredit> producedCredit =
        creditRepository.findBySourceReceivalIdAndTeamId(receival.getId(), teamId);
    producedCredit.ifPresent(
        c -> {
          boolean touched =
              c.getRemainingAmount().compareTo(c.getAmount().value()) < 0
                  || c.getRefundedAt().isPresent();
          if (touched) {
            throw new BusinessRuleException(
                "This receival created tenant credit "
                    + c.getIdentifier().map(Object::toString).orElse("")
                    + " that has already been applied or refunded; reverse that first");
          }
        });

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

    receivalRepository.softDeleteByIdAndTeamId(receival.getId(), teamId);
    producedCredit.ifPresent(
        c -> {
          creditRepository.softDeleteByIdAndTeamId(c.getId(), teamId, principal.getUserId());
          auditService.logDelete(teamId, "CONTACT_CREDIT", c.getId(), principal.getUserId(), c);
          metricsService.incrementCounter("contact.credit.reversed.total");
          log.info(
              "Reversed credit {} together with its source receival {}",
              c.getIdentifier().map(Object::toString).orElse("?"),
              receivalIdentifier);
        });

    metricsService.incrementCounter("payment.receival.total", "action", "deleted");

    // Recalculate payment status
    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);

    log.info(
        "Deleted receival {} for payment {} by user {}",
        receivalIdentifier,
        payment.getIdentifier().orElseThrow(),
        principal.getUserId());

    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put(
        "receivalDeleted", receival.getAmount().value() + " from " + receival.getReceivalDate());
    producedCredit.ifPresent(
        c -> changedFields.put("creditReversed", c.getAmount().value().toPlainString()));
    auditService.logUpdate(
        teamId, "PAYMENT", paymentId, principal.getUserId(), oldState, newState, changedFields);

    return newState;
  }

  private void recalculatePaymentStatus(Payment payment, UserPrincipal principal) {
    String currency = payment.getAmount().currency();
    BigDecimal totalReceived =
        receivalRepository.sumByPaymentIdAndTeamId(
            payment.getId(), principal.requireTeamId(), currency);
    BigDecimal balance = payment.getAmount().value().subtract(totalReceived);

    Payment.PaymentStatus newStatus;
    if (balance.compareTo(ZERO) <= 0) {
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
        payment.setPaymentDate(Optional.of(latestDate));
      }
    } else if (totalReceived.compareTo(ZERO) > 0) {
      newStatus = PARTIALLY_PAID;
      payment.setPaymentDate(Optional.empty());
    } else {
      // No receivals - check if overdue
      if (payment.getDueDate().isBefore(LocalDate.now(clock))) {
        newStatus = OVERDUE;
      } else {
        newStatus = PENDING;
      }
      payment.setPaymentDate(Optional.empty());
    }

    payment.setStatus(newStatus);
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());
    paymentRepository.save(payment);
  }

  /** Closes the open balance without money changing hands (bad debt, goodwill). */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse writeOffPayment(
      PaymentIdentifier identifier, WriteOffPaymentRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already fully paid");
    }
    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot write off a cancelled payment");
    }
    String currency = payment.getAmount().currency();
    BigDecimal balance =
        payment
            .getAmount()
            .value()
            .subtract(
                receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency));
    if (balance.signum() <= 0) {
      throw new BusinessRuleException("Payment has no outstanding balance");
    }
    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);
    PaymentReceival writeOff = new PaymentReceival();
    writeOff.setIdentifier(Optional.of(newPaymentReceivalId()));
    writeOff.setTeamId(teamId);
    writeOff.setPaymentId(payment.getId());
    writeOff.setAmount(MoneyAmount.of(balance, currency));
    writeOff.setReceivalDate(request.writeOffDate().orElse(LocalDate.now(clock)));
    writeOff.setNotes(Optional.of("Write-off: " + request.reason()));
    writeOff.setReceivalType(PaymentReceival.ReceivalType.WRITE_OFF);
    writeOff.setCreatedBy(principal.getUserId());
    writeOff.setUpdatedBy(principal.getUserId());
    writeOff.setCreatedAt(clock.instant());
    writeOff.setUpdatedAt(clock.instant());
    receivalRepository.save(writeOff);
    recalculatePaymentStatus(payment, principal);
    refreshPlanCompletion(payment, teamId, principal.getUserId());
    cancelOpenLateFees(payment, "Parent payment written off", principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);
    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put("writtenOff", balance + " " + currency + ": " + request.reason());
    auditService.logUpdate(
        teamId,
        "PAYMENT",
        payment.getId(),
        principal.getUserId(),
        oldState,
        newState,
        changedFields);
    metricsService.incrementCounter("payment.written_off.total");
    metricsService.incrementCounterBy(
        "payment.written_off.amount.total", balance.doubleValue(), "currency", currency);
    log.info(
        "Wrote off {} {} on payment {} by user {}",
        balance,
        currency,
        payment.getIdentifier().orElseThrow(),
        principal.getUserId());
    return newState;
  }

  /** Settles (part of) the open balance from a credit the tenant holds. */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse applyCredit(
      PaymentIdentifier identifier, ApplyCreditRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already fully paid");
    }
    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot apply credit to a cancelled payment");
    }
    ContactCredit credit =
        creditRepository.getByIdentifierAndTeamId(request.creditIdentifier(), teamId);
    UUID payerContactId =
        payment
            .getContactId()
            .or(
                () ->
                    contractPartyService
                        .findPrimaryContactForContract(payment.getContractId(), teamId)
                        .map(Contact::getId))
            .orElseThrow(
                () -> new BusinessRuleException("No tenant contact is linked to this payment"));
    if (!credit.getContactId().equals(payerContactId)) {
      throw new BusinessRuleException("Credit belongs to a different contact");
    }
    String currency = payment.getAmount().currency();
    if (!credit.getAmount().currency().equals(currency)) {
      throw new BusinessRuleException("Credit currency does not match the payment currency");
    }
    BigDecimal balance =
        payment
            .getAmount()
            .value()
            .subtract(
                receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency));
    BigDecimal maxApplicable = balance.min(credit.getRemainingAmount());
    if (maxApplicable.signum() <= 0) {
      throw new BusinessRuleException("Nothing to apply: no open balance or credit exhausted");
    }
    BigDecimal amount = request.amount().orElse(maxApplicable);
    if (amount.compareTo(maxApplicable) > 0) {
      throw new BusinessRuleException(
          "Amount " + amount + " exceeds the applicable maximum of " + maxApplicable);
    }
    validateCurrencyDecimals(amount, currency);

    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);
    PaymentReceival applied = new PaymentReceival();
    applied.setIdentifier(Optional.of(newPaymentReceivalId()));
    applied.setTeamId(teamId);
    applied.setPaymentId(payment.getId());
    applied.setAmount(MoneyAmount.of(amount, currency));
    applied.setReceivalDate(LocalDate.now(clock));
    applied.setNotes(Optional.of("Credit " + credit.getIdentifier().orElseThrow() + " applied"));
    applied.setReceivalType(PaymentReceival.ReceivalType.CREDIT);
    applied.setCreditId(Optional.of(credit.getId()));
    applied.setCreatedBy(principal.getUserId());
    applied.setUpdatedBy(principal.getUserId());
    applied.setCreatedAt(clock.instant());
    applied.setUpdatedAt(clock.instant());
    receivalRepository.save(applied);

    credit.setRemainingAmount(credit.getRemainingAmount().subtract(amount));
    credit.setUpdatedBy(principal.getUserId());
    credit.setUpdatedAt(clock.instant());
    creditRepository.save(credit);

    recalculatePaymentStatus(payment, principal);
    PaymentResponse newState = enrichPaymentResponse(payment, teamId);
    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put(
        "creditApplied", amount + " " + currency + " from " + credit.getIdentifier().orElseThrow());
    auditService.logUpdate(
        teamId,
        "PAYMENT",
        payment.getId(),
        principal.getUserId(),
        oldState,
        newState,
        changedFields);
    metricsService.incrementCounter("payment.credit.applied.total");
    if (payment.getStatus() == PAID) {
      refreshPlanCompletion(payment, teamId, principal.getUserId());
      sendPaymentPaidNotification(payment, teamId, principal);
    }
    return newState;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentResponse cancelPayment(
      PaymentIdentifier identifier, CancelPaymentRequest request, UserPrincipal principal) {
    return performCancel(identifier, request.reason(), principal);
  }

  /** Cancels each open payment in its own transaction and reports per-identifier outcomes. */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkActionResult<PaymentResponse>> bulkCancelPayments(
      BulkCancelPaymentsRequest request, UserPrincipal principal) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    List<BulkActionResult<PaymentResponse>> results = new ArrayList<>();
    for (PaymentIdentifier identifier : request.identifiers().stream().distinct().toList()) {
      try {
        PaymentResponse response =
            txTemplate.execute(status -> performCancel(identifier, request.reason(), principal));
        results.add(BulkActionResult.success(identifier.value(), response));
      } catch (Exception e) {
        log.warn("Bulk cancel failed for payment {}: {}", identifier, e.getMessage());
        results.add(BulkActionResult.error(identifier.value(), extractPaymentErrorMessage(e)));
      }
    }
    return results;
  }

  private PaymentResponse performCancel(
      PaymentIdentifier identifier, String reason, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("A paid payment cannot be cancelled");
    }
    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Payment is already cancelled");
    }
    if (payment.getPaymentType() == PaymentType.INSTALMENT) {
      throw new BusinessRuleException(
          "Instalments cannot be cancelled on their own; cancel the payment plan instead");
    }
    BigDecimal received =
        receivalRepository.sumByPaymentIdAndTeamId(
            payment.getId(), teamId, payment.getAmount().currency());
    if (received.signum() > 0) {
      throw new BusinessRuleException(
          "A payment with registered receivals cannot be cancelled; write off the balance instead");
    }
    PaymentResponse oldState = enrichPaymentResponse(payment, teamId);
    Payment.PaymentStatus oldStatus = payment.getStatus();
    payment.setStatus(CANCELLED);
    payment.setCancelReason(Optional.of(reason));
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());
    Payment saved = paymentRepository.save(payment);
    PaymentResponse newState = enrichPaymentResponse(saved, teamId);
    Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
    changedFields.put("cancelReason", reason);
    auditService.logUpdate(
        teamId, "PAYMENT", saved.getId(), principal.getUserId(), oldState, newState, changedFields);
    metricsService.incrementCounter(
        "payment.status.changed.total", "from_status", oldStatus.name(), "to_status", "CANCELLED");
    log.info(
        "Cancelled payment {} by user {}: {}",
        saved.getIdentifier().orElseThrow(),
        principal.getUserId(),
        reason);
    cancelOpenLateFees(saved, "Parent payment cancelled", principal);
    return newState;
  }

  /**
   * A late fee only exists because its parent rent was owed; once the parent is cancelled or
   * written off the fee is closed too, so it is not chased on its own.
   */
  private void cancelOpenLateFees(Payment parent, String reason, UserPrincipal principal) {
    if (parent.getPaymentType() != PaymentType.RENT) {
      return;
    }
    for (Payment fee :
        paymentRepository.findOpenLateFeesByParentId(parent.getId(), parent.getTeamId())) {
      fee.setStatus(CANCELLED);
      fee.setCancelReason(Optional.of(reason));
      fee.setUpdatedBy(principal.getUserId());
      fee.setUpdatedAt(clock.instant());
      paymentRepository.save(fee);
      metricsService.incrementCounter("payment.late_fee.cancelled_with_parent.total");
      log.info(
          "Cancelled late fee {} together with its parent {}",
          fee.getIdentifier().map(Object::toString).orElse("?"),
          parent.getIdentifier().map(Object::toString).orElse("?"));
    }
  }

  /** Receivals created by credits, write-offs or plans are reversed through their own actions. */
  private static void requireManualReceival(PaymentReceival receival) {
    if (receival.getReceivalType() != PaymentReceival.ReceivalType.PAYMENT) {
      throw new BusinessRuleException(
          "This entry was created by a "
              + receival
                  .getReceivalType()
                  .name()
                  .toLowerCase(java.util.Locale.ROOT)
                  .replace('_', ' ')
              + " and cannot be edited or deleted directly");
    }
  }

  // --- End receival operations ---

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deletePayment(PaymentIdentifier identifier, UserPrincipal principal) {
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

    log.info(
        "Deleted payment {} by user {}",
        payment.getIdentifier().orElseThrow(),
        principal.getUserId());

    auditService.logDelete(teamId, "PAYMENT", paymentId, principal.getUserId(), payment);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<PaymentResponse> bulkGeneratePayments(
      BulkGeneratePaymentsRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    YearMonth month = YearMonth.parse(request.forMonth());

    // In force = ACTIVE or NOTICE_GIVEN: a tenant under notice still owes rent until the
    // effective end date, past which no payment is generated (see the billingEndDate check below).
    List<Contract> activeContracts = contractRepository.findInForceByTeamId(teamId);

    if (activeContracts.isEmpty()) {
      log.warn("No active contracts found for team {} to generate payments", teamId);
      return List.of();
    }

    List<Payment> generatedPayments = new ArrayList<>();

    List<UUID> contractIds = activeContracts.stream().map(Contract::getId).toList();
    Map<UUID, List<ContractExtension>> extensionsByContract =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.groupingBy(ContractExtension::getContractId));
    Map<UUID, ContractTermination> terminationsByContract =
        contractTerminationRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<Payment>> paymentsByContract =
        paymentRepository.findByContractIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.groupingBy(Payment::getContractId));

    for (Contract contract : activeContracts) {
      LocalDate dueDate = calculateDueDate(month, contract);

      Optional<LocalDate> billingEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(
              contract.getEndDate(),
              extensionsByContract.getOrDefault(contract.getId(), List.of()),
              Optional.ofNullable(terminationsByContract.get(contract.getId())));
      if (billingEndDate.isPresent() && dueDate.isAfter(billingEndDate.get())) {
        log.debug(
            "Due date {} is after contract {}'s end date {}, skipping",
            dueDate,
            contract.getIdentifier().orElseThrow(),
            billingEndDate.get());
        continue;
      }

      List<Payment> existingPayments = paymentsByContract.getOrDefault(contract.getId(), List.of());
      boolean paymentExists =
          existingPayments.stream().anyMatch(p -> p.getDueDate().equals(dueDate));

      if (paymentExists) {
        log.debug(
            "Payment already exists for contract {} on {}, skipping",
            contract.getIdentifier().orElseThrow(),
            dueDate);
        continue;
      }

      Payment payment = new Payment();
      payment.setIdentifier(Optional.of(newPaymentId()));
      payment.setTeamId(teamId);
      payment.setContractId(contract.getId());
      payment.setAmount(contract.getRentAmount());
      payment.setDueDate(dueDate);
      payment.setStatus(PENDING);
      payment.setNotes(Optional.of("Auto-generated for " + month));
      payment.setCreatedBy(principal.getUserId());
      payment.setUpdatedBy(principal.getUserId());
      payment.setCreatedAt(clock.instant());
      payment.setUpdatedAt(clock.instant());

      Payment savedPayment = paymentRepository.save(payment);
      generatedPayments.add(savedPayment);

      log.info(
          "Generated payment {} for contract {} due on {}",
          savedPayment.getIdentifier().orElseThrow(),
          contract.getIdentifier().orElseThrow(),
          dueDate);
    }

    if (!generatedPayments.isEmpty()) {
      metricsService.incrementCounter("payment.bulk.generated.total");
      metricsService.recordHistogram("payment.bulk.generated.count", generatedPayments.size());

      auditService.logCreate(
          teamId,
          "BULK_PAYMENT_GENERATION",
          generatedPayments.getFirst().getId(),
          principal.getUserId(),
          "Generated " + generatedPayments.size() + " payments for " + month);
    }

    return enrichPaymentResponses(generatedPayments, teamId);
  }

  // --- Document delegation methods (resolve identifier to UUID) ---

  public DocumentResponse uploadDocument(
      PaymentIdentifier paymentIdentifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file,
        "PAYMENT",
        payment.getId(),
        payment.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getDocuments(
      PaymentIdentifier paymentIdentifier, UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return documentService.getDocuments("PAYMENT", payment.getId(), principal);
  }

  public URL getDocumentDownloadUrl(
      DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    return documentService.getDownloadUrl(documentIdentifier, principal);
  }

  public void deleteDocument(DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    documentService.deleteDocument(documentIdentifier, principal);
  }

  public List<RecentActivityResponse> getAuditLog(
      PaymentIdentifier paymentIdentifier, UserPrincipal principal) {
    Payment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "PAYMENT", payment.getId());
  }

  // --- Helper methods ---

  private LocalDate calculateDueDate(YearMonth month, Contract contract) {
    int paymentDueDay = contract.getPaymentDueDay().orElse(1);

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
    Optional<Contract> contractOpt =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId);
    String propertyName =
        contractOpt
            .flatMap(c -> propertyRepository.findByIdAndTeamId(c.getPropertyId(), teamId))
            .map(p -> p.getStreet() + ", " + p.getCity())
            .orElse("N/A");
    String contactName =
        contractOpt
            .flatMap(c -> contractPartyService.findPrimaryContactForContract(c.getId(), teamId))
            .map(Contact::getDisplayName)
            .orElse("N/A");
    String base = appProperties.email().baseUrl();
    String paymentSid = payment.getIdentifier().map(s -> s.value()).orElse("");
    String contractSid =
        contractOpt.flatMap(Contract::getIdentifier).map(s -> s.value()).orElse("");
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(PAYMENT_PAID)
            .relatedPaymentId(Optional.of(payment.getId()))
            .relatedContractId(Optional.of(payment.getContractId()))
            .templateName("payment-paid")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "contactName",
                    contactName,
                    "amount",
                    payment.getAmount().currency() + " " + payment.getAmount().value(),
                    "paymentDate",
                    payment.getPaymentDate().map(LocalDate::toString).orElse("N/A"),
                    "baseUrl",
                    base,
                    "primaryUrl",
                    base + "/payments/" + paymentSid,
                    "secondaryUrl",
                    base + "/contracts/" + contractSid))
            .createdBy(principal.getUserId())
            .build());
  }

  private void sendReceivalNotification(
      Payment payment, BigDecimal receivalAmount, UUID teamId, UserPrincipal principal) {
    Optional<Contract> contractOpt =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId);
    String propertyName =
        contractOpt
            .flatMap(c -> propertyRepository.findByIdAndTeamId(c.getPropertyId(), teamId))
            .map(p -> p.getStreet() + ", " + p.getCity())
            .orElse("N/A");
    String contactName =
        contractOpt
            .flatMap(c -> contractPartyService.findPrimaryContactForContract(c.getId(), teamId))
            .map(Contact::getDisplayName)
            .orElse("N/A");
    String currency = payment.getAmount().currency();
    BigDecimal totalReceived =
        receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency);
    BigDecimal remainingBalance = payment.getAmount().value().subtract(totalReceived);

    Map<String, Object> vars = new HashMap<>();
    vars.put("propertyName", propertyName);
    vars.put("contactName", contactName);
    vars.put("receivalAmount", currency + " " + receivalAmount);
    vars.put("amount", currency + " " + payment.getAmount().value());
    vars.put("remainingBalance", currency + " " + remainingBalance);
    String base = appProperties.email().baseUrl();
    String paymentSid = payment.getIdentifier().map(s -> s.value()).orElse("");
    String contractSid =
        contractOpt.flatMap(Contract::getIdentifier).map(s -> s.value()).orElse("");
    vars.put("baseUrl", base);
    vars.put("primaryUrl", base + "/payments/" + paymentSid);
    vars.put("secondaryUrl", base + "/contracts/" + contractSid);

    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(PAYMENT_RECEIVAL)
            .relatedPaymentId(Optional.of(payment.getId()))
            .relatedContractId(Optional.of(payment.getContractId()))
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

    Map<UUID, Property> propertiesById =
        propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
            .collect(toMap(Property::getId, identity()));

    // Batch-fetch primary contacts via contract_parties
    Map<UUID, Contact> primaryContactByContract =
        contractPartyService.getPrimaryContactsForContracts(contractIds, teamId);

    // Batch-fetch parent rents of late fees and plans of instalments (identifiers only)
    Map<UUID, Sid> parentIdentifiers =
        paymentRepository
            .findByIdsAndTeamId(
                payments.stream()
                    .map(Payment::getParentPaymentId)
                    .flatMap(Optional::stream)
                    .collect(toSet()),
                teamId)
            .stream()
            .filter(p -> p.getIdentifier().isPresent())
            .collect(toMap(Payment::getId, p -> p.getIdentifier().orElseThrow()));
    Map<UUID, List<PaymentSummary>> lateFeesByParent =
        paymentRepository
            .findLateFeesByParentIds(
                payments.stream()
                    .filter(p -> p.getPaymentType() == PaymentType.RENT)
                    .map(Payment::getId)
                    .toList(),
                teamId)
            .entrySet()
            .stream()
            .collect(
                toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream().map(paymentMapper::toSummary).toList()));

    Map<UUID, Sid> planIdentifiers =
        paymentPlanRepository
            .findByIdsAndTeamId(
                payments.stream()
                    .map(Payment::getPaymentPlanId)
                    .flatMap(Optional::stream)
                    .collect(toSet()),
                teamId)
            .stream()
            .filter(p -> p.getIdentifier().isPresent())
            .collect(toMap(PaymentPlan::getId, p -> p.getIdentifier().orElseThrow()));

    // Batch-fetch explicit contacts assigned to payments
    Set<UUID> explicitContactIds =
        payments.stream()
            .map(Payment::getContactId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());
    Map<UUID, Contact> explicitContactsById =
        explicitContactIds.isEmpty()
            ? Map.of()
            : contactRepository.findByIdsAndTeamId(explicitContactIds, teamId).stream()
                .collect(toMap(Contact::getId, identity()));

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
          receivals.stream().map(r -> r.getAmount().value()).reduce(ZERO, BigDecimal::add);
      BigDecimal balance = payment.getAmount().value().subtract(receivedAmount);
      List<PaymentReceivalResponse> receivalResponses =
          receivals.stream().map(receivalMapper::toResponse).toList();

      Contract contract = contractsById.get(payment.getContractId());
      ContractSummary contractSummary =
          contract != null ? contractMapper.toSummary(contract) : null;
      PropertySummary propertySummary = null;
      ContactSummary contactSummary = null;

      if (contract != null) {
        Property property = propertiesById.get(contract.getPropertyId());
        // Prefer explicit contact on payment, fall back to contract's primary contact
        Contact contact =
            payment
                .getContactId()
                .map(explicitContactsById::get)
                .orElseGet(() -> primaryContactByContract.get(contract.getId()));
        if (property != null) {
          propertySummary = propertyMapper.toSummary(property);
        }
        if (contact != null) {
          contactSummary = contactMapper.toSummary(contact);
        }
      }

      List<com.buurman.domain.Document> paymentDocs =
          docsByPaymentId.getOrDefault(payment.getId(), List.of());
      DocumentResponse proofOfPayment =
          paymentDocs.stream()
              .filter(doc -> doc.getTitle().map(t -> t.contains("Proof")).orElse(false))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);
      DocumentResponse receipt =
          paymentDocs.stream()
              .filter(doc -> doc.getTitle().map(t -> t.contains("Receipt")).orElse(false))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      responses.add(
          new PaymentResponse(
              base.identifier(),
              Optional.ofNullable(contractSummary),
              Optional.ofNullable(contactSummary),
              Optional.ofNullable(propertySummary),
              base.amount(),
              base.currency(),
              Optional.of(receivedAmount),
              Optional.of(balance),
              base.paymentDate(),
              base.dueDate(),
              base.status(),
              base.notes(),
              Optional.ofNullable(proofOfPayment),
              Optional.ofNullable(receipt),
              receivalResponses,
              base.createdAt(),
              base.updatedAt(),
              base.paymentType(),
              payment.getParentPaymentId().map(parentIdentifiers::get),
              base.cancelReason(),
              base.waivedAt(),
              base.waiveReason(),
              payment.getPaymentPlanId().map(planIdentifiers::get),
              lateFeesByParent.getOrDefault(payment.getId(), List.of())));
    }
    return responses;
  }

  private PaymentResponse enrichPaymentResponse(Payment payment, UUID teamId) {
    PaymentResponse response = paymentMapper.toResponse(payment);

    // Get receivals and calculate balance
    List<PaymentReceival> receivals =
        receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId);
    BigDecimal receivedAmount =
        receivals.stream().map(r -> r.getAmount().value()).reduce(ZERO, BigDecimal::add);
    BigDecimal balance = payment.getAmount().value().subtract(receivedAmount);
    List<PaymentReceivalResponse> receivalResponses =
        receivals.stream().map(receivalMapper::toResponse).toList();

    Contract contract =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);

    if (contract != null) {
      ContractSummary contractSummary = contractMapper.toSummary(contract);

      PropertySummary propertySummary =
          propertyRepository
              .findByIdAndTeamId(contract.getPropertyId(), teamId)
              .map(propertyMapper::toSummary)
              .orElse(null);

      ContactSummary contactSummary =
          payment
              .getContactId()
              .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
              .or(
                  () ->
                      contractPartyService.findPrimaryContactForContract(contract.getId(), teamId))
              .map(contactMapper::toSummary)
              .orElse(null);

      DocumentResponse proofOfPayment =
          documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId).stream()
              .filter(doc -> doc.getTitle().map(t -> t.contains("Proof")).orElse(false))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      DocumentResponse receipt =
          documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId).stream()
              .filter(doc -> doc.getTitle().map(t -> t.contains("Receipt")).orElse(false))
              .findFirst()
              .map(documentMapper::toResponse)
              .orElse(null);

      return new PaymentResponse(
          response.identifier(),
          Optional.of(contractSummary),
          Optional.ofNullable(contactSummary),
          Optional.ofNullable(propertySummary),
          response.amount(),
          response.currency(),
          Optional.of(receivedAmount),
          Optional.of(balance),
          response.paymentDate(),
          response.dueDate(),
          response.status(),
          response.notes(),
          Optional.ofNullable(proofOfPayment),
          Optional.ofNullable(receipt),
          receivalResponses,
          response.createdAt(),
          response.updatedAt(),
          response.paymentType(),
          parentIdentifier(payment, teamId),
          response.cancelReason(),
          response.waivedAt(),
          response.waiveReason(),
          planIdentifier(payment, teamId),
          lateFeeSummaries(payment, teamId));
    }

    return new PaymentResponse(
        response.identifier(),
        response.contract(),
        response.contact(),
        response.property(),
        response.amount(),
        response.currency(),
        Optional.of(receivedAmount),
        Optional.of(balance),
        response.paymentDate(),
        response.dueDate(),
        response.status(),
        response.notes(),
        response.proofOfPayment(),
        response.receipt(),
        receivalResponses,
        response.createdAt(),
        response.updatedAt(),
        response.paymentType(),
        parentIdentifier(payment, teamId),
        response.cancelReason(),
        response.waivedAt(),
        response.waiveReason(),
        planIdentifier(payment, teamId),
        lateFeeSummaries(payment, teamId));
  }

  /** The late fees charged on this rent, so the timeline shows what it incurred. */
  private List<PaymentSummary> lateFeeSummaries(Payment payment, UUID teamId) {
    if (payment.getPaymentType() != PaymentType.RENT) {
      return List.of();
    }
    return paymentRepository
        .findLateFeesByParentIds(List.of(payment.getId()), teamId)
        .getOrDefault(payment.getId(), List.of())
        .stream()
        .map(paymentMapper::toSummary)
        .toList();
  }

  private Optional<Sid> planIdentifier(Payment payment, UUID teamId) {
    return payment
        .getPaymentPlanId()
        .flatMap(id -> paymentPlanRepository.findByIdAndTeamId(id, teamId))
        .flatMap(PaymentPlan::getIdentifier);
  }

  /** An instalment reaching PAID completes its plan once every instalment is paid. */
  private void refreshPlanCompletion(Payment payment, UUID teamId, UUID userId) {
    payment
        .getPaymentPlanId()
        .flatMap(id -> paymentPlanRepository.findByIdAndTeamId(id, teamId))
        .filter(plan -> plan.getStatus() == PaymentPlan.PlanStatus.ACTIVE)
        .ifPresent(
            plan -> {
              boolean allPaid =
                  paymentRepository.findByPaymentPlanId(plan.getId(), teamId).stream()
                      .allMatch(p -> p.getStatus() == PAID || p.getStatus() == CANCELLED);
              if (allPaid) {
                plan.setStatus(PaymentPlan.PlanStatus.COMPLETED);
                plan.setUpdatedBy(userId);
                paymentPlanRepository.save(plan);
                metricsService.incrementCounter("payment.plan.completed.total");
              }
            });
  }

  private Optional<Sid> parentIdentifier(Payment payment, UUID teamId) {
    return payment
        .getParentPaymentId()
        .flatMap(id -> paymentRepository.findByIdAndTeamId(id, teamId))
        .flatMap(Payment::getIdentifier);
  }

  private void validateCurrencyDecimals(
      @Nullable BigDecimal amount, @Nullable String currencyCode) {
    if (amount == null || currencyCode == null || currencyCode.isBlank()) {
      return;
    }
    if (!CurrencyUtils.isAmountValidForCurrency(amount, currencyCode)) {
      int allowed = CurrencyUtils.getFractionalDigits(currencyCode);
      String msg =
          "%s amounts cannot have more than %d decimal place%s"
              .formatted(currencyCode, allowed, allowed == 1 ? "" : "s");
      throw new BusinessRuleException(msg);
    }
  }
}
