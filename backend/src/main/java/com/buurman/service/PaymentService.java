package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReceival;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.ContractSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentStatsResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantSummary;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Record2;
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
import com.buurman.domain.NotificationType;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.config.models.AppProperties;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentReceivalRepository receivalRepository;
    private final ContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
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
    private final AppProperties appProperties;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentReceivalRepository receivalRepository,
            ContractRepository contractRepository,
            PropertyRepository propertyRepository,
            TenantRepository tenantRepository,
            DocumentRepository documentRepository,
            PaymentMapper paymentMapper,
            PaymentReceivalMapper receivalMapper,
            ContractMapper contractMapper,
            PropertyMapper propertyMapper,
            TenantMapper tenantMapper,
            AuditService auditService,
            DocumentService documentService,
            com.buurman.mapper.DocumentMapper documentMapper,
            MetricsService metricsService,
            NotificationService notificationService,
            AppProperties appProperties,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.receivalRepository = receivalRepository;
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
        this.documentRepository = documentRepository;
        this.paymentMapper = paymentMapper;
        this.receivalMapper = receivalMapper;
        this.contractMapper = contractMapper;
        this.propertyMapper = propertyMapper;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
        this.documentService = documentService;
        this.documentMapper = documentMapper;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse createPayment(CreatePaymentRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        // Resolve contract by identifier
        Contract contract = contractRepository.findByIdentifierAndTeamId(request.contractIdentifier(), teamId)
                .orElseThrow(() -> new NotFoundException("Contract not found or access denied"));

        Payment payment = paymentMapper.toEntity(request);
        payment.setContractId(contract.getId());
        payment.setIdentifier(UlidGenerator.generate(EntityPrefix.PAY));
        payment.setTeamId(teamId);
        payment.setStatus(PENDING);
        payment.setCreatedBy(principal.getUserId());
        payment.setUpdatedBy(principal.getUserId());
        payment.setCreatedAt(clock.instant());
        payment.setUpdatedAt(clock.instant());

        if (payment.getCurrency() == null || payment.getCurrency().isEmpty()) {
            payment.setCurrency(contract.getCurrency() != null ? contract.getCurrency() : "EUR");
        }

        Payment savedPayment = paymentRepository.save(payment);

        metricsService.incrementCounter("payment.total");
        metricsService.recordHistogram("payment.amount", savedPayment.getAmount().doubleValue(),
                "currency", savedPayment.getCurrency(), "status", savedPayment.getStatus().name());

        log.info("Created payment {} for contract {} by user {}",
                savedPayment.getIdentifier(), contract.getIdentifier(), principal.getUserId());

        auditService.logCreate(teamId, "PAYMENT", savedPayment.getId(), principal.getUserId(), savedPayment);

        return enrichPaymentResponse(savedPayment, teamId);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String identifier, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        return enrichPaymentResponse(payment, principal.getTeamId());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(UserPrincipal principal) {
        List<Payment> payments = paymentRepository.findAllByTeamId(principal.getTeamId());

        LocalDate today = LocalDate.now(clock);
        payments.forEach(payment -> updatePaymentStatus(payment, today));

        return enrichPaymentResponses(payments, principal.getTeamId());
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentsPaginated(UserPrincipal principal, String status, UUID contractId, PageRequest pageRequest) {
        PaginatedResult<Payment> result = paymentRepository.findAllByTeamIdPaginated(
                principal.getTeamId(), status, contractId, pageRequest);

        LocalDate today = LocalDate.now(clock);
        result.items().forEach(payment -> updatePaymentStatus(payment, today));

        List<PaymentResponse> responses = enrichPaymentResponses(result.items(), principal.getTeamId());
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    public PaymentStatsResponse getPaymentStats(UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Record2<Integer, BigDecimal> pending = paymentRepository.getPendingStats(teamId);
        Record2<Integer, BigDecimal> overdue = paymentRepository.getOverdueStats(teamId);
        String currency = paymentRepository.findCurrencyByTeamId(teamId);

        List<PaymentStatsResponse.MonthlyTrend> monthlyTrend = paymentRepository.getMonthlyPaidTrend(teamId, 12)
                .stream()
                .map(r -> new PaymentStatsResponse.MonthlyTrend(
                        r.value1(),
                        r.value2() != null ? r.value2() : BigDecimal.ZERO))
                .toList();

        return new PaymentStatsResponse(
                pending.value1(),
                pending.value2() != null ? pending.value2() : BigDecimal.ZERO,
                overdue.value1(),
                overdue.value2() != null ? overdue.value2() : BigDecimal.ZERO,
                currency,
                monthlyTrend
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByContract(String contractIdentifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract contract = contractRepository.findByIdentifierAndTeamId(contractIdentifier, teamId)
                .orElseThrow(() -> new NotFoundException("Contract not found or access denied"));

        List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);

        LocalDate today = LocalDate.now(clock);
        payments.forEach(payment -> updatePaymentStatus(payment, today));

        return enrichPaymentResponses(payments, teamId);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getOverduePayments(UserPrincipal principal) {
        List<Payment> payments = paymentRepository.findOverduePayments(principal.getTeamId());

        return enrichPaymentResponses(payments, principal.getTeamId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse updatePayment(String identifier, UpdatePaymentRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

        paymentMapper.updateEntity(payment, request);
        payment.setUpdatedBy(principal.getUserId());
        payment.setUpdatedAt(clock.instant());

        Payment updatedPayment = paymentRepository.save(payment);
        PaymentResponse newState = enrichPaymentResponse(updatedPayment, teamId);

        log.info("Updated payment {} by user {}", updatedPayment.getIdentifier(), principal.getUserId());

        auditService.logUpdate(teamId, "PAYMENT", updatedPayment.getId(), principal.getUserId(),
                oldState, newState, auditService.getChangedFields(oldState, newState));

        return newState;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse markPaymentAsPaid(String identifier, MarkPaidRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        if (payment.getStatus() == PAID) {
            throw new BusinessRuleException("Payment is already marked as paid");
        }

        if (payment.getStatus() == CANCELLED) {
            throw new BusinessRuleException("Cannot mark a cancelled payment as paid");
        }

        // Register a receival for the remaining balance
        BigDecimal receivedAmount = receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId);
        BigDecimal remainingBalance = payment.getAmount().subtract(receivedAmount);

        if (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {
            PaymentReceival receival = new PaymentReceival();
            receival.setIdentifier(UlidGenerator.generate(EntityPrefix.PRE));
            receival.setTeamId(teamId);
            receival.setPaymentId(payment.getId());
            receival.setAmount(remainingBalance);
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
        metricsService.incrementCounter("payment.status.changed.total",
                "from_status", oldStatus.name(), "to_status", "PAID");

        log.info("Marked payment {} as PAID on {} by user {}",
                updatedPayment.getIdentifier(), request.paymentDate(), principal.getUserId());

        auditService.logUpdate(teamId, "PAYMENT", updatedPayment.getId(), principal.getUserId(),
                oldState, newState, auditService.getChangedFields(oldState, newState));

        sendPaymentPaidNotification(updatedPayment, teamId, principal);

        return newState;
    }

    // --- Receival operations ---

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse registerReceival(String paymentIdentifier, CreatePaymentReceivalRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        UUID paymentId = payment.getId();

        if (payment.getStatus() == PAID) {
            throw new BusinessRuleException("Payment is already fully paid");
        }

        if (payment.getStatus() == CANCELLED) {
            throw new BusinessRuleException("Cannot register receival on a cancelled payment");
        }

        // Validate amount does not exceed balance
        BigDecimal currentReceived = receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId);
        BigDecimal currentBalance = payment.getAmount().subtract(currentReceived);

        if (request.amount().compareTo(currentBalance) > 0) {
            throw new BusinessRuleException("Receival amount (" + request.amount() +
                    ") exceeds remaining balance (" + currentBalance + ")");
        }

        PaymentReceival receival = new PaymentReceival();
        receival.setIdentifier(UlidGenerator.generate(EntityPrefix.PRE));
        receival.setTeamId(teamId);
        receival.setPaymentId(paymentId);
        receival.setAmount(request.amount());
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

        log.info("Registered receival of {} for payment {} by user {}",
                request.amount(), payment.getIdentifier(), principal.getUserId());

        Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
        changedFields.put("receivalRegistered", request.amount() + " on " + request.receivalDate());
        auditService.logUpdate(teamId, "PAYMENT", paymentId, principal.getUserId(),
                oldState, newState, changedFields);

        sendReceivalNotification(payment, request.amount(), teamId, principal);

        if (payment.getStatus() == PAID) {
            sendPaymentPaidNotification(payment, teamId, principal);
        }

        return newState;
    }

    @Transactional(readOnly = true)
    public List<PaymentReceivalResponse> getReceivalsForPayment(String paymentIdentifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        return receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId)
                .stream()
                .map(receivalMapper::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse updateReceival(String paymentIdentifier, String receivalIdentifier,
                                          UpdatePaymentReceivalRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        UUID paymentId = payment.getId();

        PaymentReceival receival = receivalRepository.findByIdentifierAndPaymentIdAndTeamId(
                        receivalIdentifier, paymentId, teamId)
                .orElseThrow(() -> new NotFoundException("Receival not found or access denied"));

        // Validate new amount: total received minus old amount plus new amount must not exceed payment amount
        BigDecimal currentReceived = receivalRepository.sumByPaymentIdAndTeamId(paymentId, teamId);
        BigDecimal receivedWithoutThis = currentReceived.subtract(receival.getAmount());
        BigDecimal newBalance = payment.getAmount().subtract(receivedWithoutThis).subtract(request.amount());

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Updated receival amount (" + request.amount() +
                    ") would exceed the payment amount");
        }

        PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

        BigDecimal oldAmount = receival.getAmount();
        receivalRepository.update(receival.getId(), teamId,
                request.amount(), request.receivalDate(), request.notes(), principal.getUserId());

        // Recalculate payment status
        recalculatePaymentStatus(payment, principal);
        PaymentResponse newState = enrichPaymentResponse(payment, teamId);

        metricsService.incrementCounter("payment.receival.total", "action", "updated");

        log.info("Updated receival {} for payment {} by user {}",
                receivalIdentifier, payment.getIdentifier(), principal.getUserId());

        Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
        changedFields.put("receivalUpdated", oldAmount + " -> " + request.amount() + " on " + request.receivalDate());
        auditService.logUpdate(teamId, "PAYMENT", paymentId, principal.getUserId(),
                oldState, newState, changedFields);

        return newState;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse deleteReceival(String paymentIdentifier, String receivalIdentifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        UUID paymentId = payment.getId();

        PaymentReceival receival = receivalRepository.findByIdentifierAndPaymentIdAndTeamId(
                        receivalIdentifier, paymentId, teamId)
                .orElseThrow(() -> new NotFoundException("Receival not found or access denied"));

        PaymentResponse oldState = enrichPaymentResponse(payment, teamId);

        receivalRepository.softDeleteByIdAndTeamId(receival.getId(), teamId);

        metricsService.incrementCounter("payment.receival.total", "action", "deleted");

        // Recalculate payment status
        recalculatePaymentStatus(payment, principal);
        PaymentResponse newState = enrichPaymentResponse(payment, teamId);

        log.info("Deleted receival {} for payment {} by user {}",
                receivalIdentifier, payment.getIdentifier(), principal.getUserId());

        Map<String, Object> changedFields = auditService.getChangedFields(oldState, newState);
        changedFields.put("receivalDeleted", receival.getAmount() + " from " + receival.getReceivalDate());
        auditService.logUpdate(teamId, "PAYMENT", paymentId, principal.getUserId(),
                oldState, newState, changedFields);

        return newState;
    }

    private void recalculatePaymentStatus(Payment payment, UserPrincipal principal) {
        BigDecimal totalReceived = receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), principal.getTeamId());
        BigDecimal balance = payment.getAmount().subtract(totalReceived);

        Payment.PaymentStatus newStatus;
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            newStatus = PAID;
            // Set payment date to the latest receival date
            List<PaymentReceival> receivals = receivalRepository.findByPaymentIdAndTeamId(payment.getId(), principal.getTeamId());
            if (!receivals.isEmpty()) {
                LocalDate latestDate = receivals.stream()
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
        UUID teamId = principal.getTeamId();

        Payment payment = paymentRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));

        if (payment.getStatus() == PAID) {
            throw new BusinessRuleException("Cannot delete a paid payment. Please cancel it instead.");
        }

        paymentRepository.softDeleteByIdAndTeamId(payment.getId(), teamId);

        log.info("Deleted payment {} by user {}", payment.getIdentifier(), principal.getUserId());

        auditService.logDelete(teamId, "PAYMENT", payment.getId(), principal.getUserId(), payment);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public List<PaymentResponse> bulkGeneratePayments(BulkGeneratePaymentsRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();
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
            boolean paymentExists = existingPayments.stream()
                    .anyMatch(p -> p.getDueDate().equals(dueDate));

            if (paymentExists) {
                log.debug("Payment already exists for contract {} on {}, skipping",
                        contract.getIdentifier(), dueDate);
                continue;
            }

            Payment payment = new Payment();
            payment.setIdentifier(UlidGenerator.generate(EntityPrefix.PAY));
            payment.setTeamId(teamId);
            payment.setContractId(contract.getId());
            payment.setAmount(contract.getRentAmount());
            payment.setCurrency(contract.getCurrency());
            payment.setDueDate(dueDate);
            payment.setStatus(PENDING);
            payment.setNotes("Auto-generated for " + month);
            payment.setCreatedBy(principal.getUserId());
            payment.setUpdatedBy(principal.getUserId());
            payment.setCreatedAt(clock.instant());
            payment.setUpdatedAt(clock.instant());

            Payment savedPayment = paymentRepository.save(payment);
            generatedPayments.add(savedPayment);

            log.info("Generated payment {} for contract {} due on {}",
                    savedPayment.getIdentifier(), contract.getIdentifier(), dueDate);
        }

        if (!generatedPayments.isEmpty()) {
            metricsService.incrementCounter("payment.bulk.generated.total");
            metricsService.recordHistogram("payment.bulk.generated.count", generatedPayments.size());

            auditService.logCreate(teamId, "BULK_PAYMENT_GENERATION",
                    generatedPayments.get(0).getId(), principal.getUserId(),
                    "Generated " + generatedPayments.size() + " payments for " + month);
        }

        return enrichPaymentResponses(generatedPayments, teamId);
    }

    // --- Document delegation methods (resolve identifier to UUID) ---

    public DocumentResponse uploadDocument(String paymentIdentifier, MultipartFile file,
                                           String title, String notes, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));
        return documentService.uploadDocument(file, "PAYMENT", payment.getId(), payment.getIdentifier(), title, notes, principal);
    }

    public List<DocumentResponse> getDocuments(String paymentIdentifier, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));
        return documentService.getDocuments("PAYMENT", payment.getId(), principal);
    }

    public URL getDocumentDownloadUrl(String documentIdentifier, UserPrincipal principal) {
        Document document = documentRepository.findByIdentifierAndTeamId(documentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Document not found or access denied"));
        return documentService.getDownloadUrl(document.getIdentifier(), principal);
    }

    public void deleteDocument(String documentIdentifier, UserPrincipal principal) {
        Document document = documentRepository.findByIdentifierAndTeamId(documentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Document not found or access denied"));
        documentService.deleteDocument(document.getIdentifier(), principal);
    }

    public List<RecentActivityResponse> getAuditLog(String paymentIdentifier, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdentifierAndTeamId(paymentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new NotFoundException("Payment not found or access denied"));
        return auditService.getEntityAuditLog(principal.getTeamId(), "PAYMENT", payment.getId());
    }

    // --- Helper methods ---

    private LocalDate calculateDueDate(YearMonth month, Contract contract) {
        Integer paymentDueDay = contract.getPaymentDueDay();
        if (paymentDueDay == null) {
            paymentDueDay = 1;
        }

        int maxDayInMonth = month.lengthOfMonth();
        int actualDay = Math.min(paymentDueDay, maxDayInMonth);

        return month.atDay(actualDay);
    }

    private void updatePaymentStatus(Payment payment, LocalDate today) {
        if (payment.getStatus() == PENDING &&
                payment.getDueDate().isBefore(today)) {
            payment.setStatus(OVERDUE);
        }
    }

    private void sendPaymentPaidNotification(Payment payment, UUID teamId, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
        String propertyName = "N/A";
        String tenantName = "N/A";
        if (contract != null) {
            Property property = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
            Tenant tenant = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId).orElse(null);
            if (property != null) {
                propertyName = property.getStreet() != null ? property.getStreet() + ", " + property.getCity() : property.getIdentifier();
            }
            if (tenant != null) {
                tenantName = tenant.getFirstName() + " " + tenant.getLastName();
            }
        }
        notificationService.sendToTeam(SendNotificationRequest.builder()
                .teamId(teamId)
                .notificationType(NotificationType.PAYMENT_PAID)
                .templateName("payment-paid")
                .templateVariables(Map.of(
                        "propertyName", propertyName,
                        "tenantName", tenantName,
                        "amount", (payment.getCurrency() != null ? payment.getCurrency() : "EUR") + " " + payment.getAmount(),
                        "paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "N/A",
                        "baseUrl", appProperties.email().baseUrl()
                ))
                .createdBy(principal.getUserId())
                .build());
    }

    private void sendReceivalNotification(Payment payment, BigDecimal receivalAmount, UUID teamId, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
        String propertyName = "N/A";
        String tenantName = "N/A";
        if (contract != null) {
            Property property = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
            Tenant tenant = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId).orElse(null);
            if (property != null) {
                propertyName = property.getStreet() != null ? property.getStreet() + ", " + property.getCity() : property.getIdentifier();
            }
            if (tenant != null) {
                tenantName = tenant.getFirstName() + " " + tenant.getLastName();
            }
        }
        String currency = payment.getCurrency() != null ? payment.getCurrency() : "EUR";
        BigDecimal totalReceived = receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId);
        BigDecimal remainingBalance = payment.getAmount().subtract(totalReceived);

        Map<String, Object> vars = new HashMap<>();
        vars.put("propertyName", propertyName);
        vars.put("tenantName", tenantName);
        vars.put("receivalAmount", currency + " " + receivalAmount);
        vars.put("amount", currency + " " + payment.getAmount());
        vars.put("remainingBalance", currency + " " + remainingBalance);
        vars.put("baseUrl", appProperties.email().baseUrl());

        notificationService.sendToTeam(SendNotificationRequest.builder()
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
        List<PaymentReceival> allReceivals = receivalRepository.findByPaymentIdsAndTeamId(paymentIds, teamId);
        Map<UUID, List<PaymentReceival>> receivalsByPaymentId = allReceivals.stream()
                .collect(groupingBy(PaymentReceival::getPaymentId));

        // Batch-fetch contracts
        Set<UUID> contractIds = payments.stream().map(Payment::getContractId).collect(toSet());
        Map<UUID, Contract> contractsById = contractRepository.findByIdsAndTeamId(contractIds, teamId).stream()
                .collect(toMap(Contract::getId, identity()));

        // Batch-fetch properties and tenants from contracts
        Set<UUID> propertyIds = contractsById.values().stream()
                .map(Contract::getPropertyId).filter(java.util.Objects::nonNull).collect(toSet());
        Set<UUID> tenantIds = contractsById.values().stream()
                .map(Contract::getTenantId).filter(java.util.Objects::nonNull).collect(toSet());

        Map<UUID, com.buurman.domain.Property> propertiesById = propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
                .collect(toMap(com.buurman.domain.Property::getId, identity()));
        Map<UUID, com.buurman.domain.Tenant> tenantsById = tenantRepository.findByIdsAndTeamId(tenantIds, teamId).stream()
                .collect(toMap(com.buurman.domain.Tenant::getId, identity()));

        // Batch-fetch documents for all payments
        List<com.buurman.domain.Document> allDocs = documentRepository.findByEntityTypeAndEntityIdsAndTeamId("PAYMENT", paymentIds, teamId);
        Map<UUID, List<com.buurman.domain.Document>> docsByPaymentId = allDocs.stream()
                .collect(groupingBy(com.buurman.domain.Document::getEntityId));

        // Build responses
        List<PaymentResponse> responses = new ArrayList<>(payments.size());
        for (Payment payment : payments) {
            PaymentResponse base = paymentMapper.toResponse(payment);

            List<PaymentReceival> receivals = receivalsByPaymentId.getOrDefault(payment.getId(), List.of());
            BigDecimal receivedAmount = receivals.stream()
                    .map(PaymentReceival::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = payment.getAmount().subtract(receivedAmount);
            List<PaymentReceivalResponse> receivalResponses = receivals.stream()
                    .map(receivalMapper::toResponse)
                    .toList();

            Contract contract = contractsById.get(payment.getContractId());
            ContractSummary contractSummary = contract != null ? contractMapper.toSummary(contract) : null;
            PropertySummary propertySummary = null;
            TenantSummary tenantSummary = null;

            if (contract != null) {
                com.buurman.domain.Property property = contract.getPropertyId() != null ? propertiesById.get(contract.getPropertyId()) : null;
                com.buurman.domain.Tenant tenant = contract.getTenantId() != null ? tenantsById.get(contract.getTenantId()) : null;
                if (property != null) propertySummary = propertyMapper.toSummary(property);
                if (tenant != null) tenantSummary = tenantMapper.toSummary(tenant);
            }

            List<com.buurman.domain.Document> paymentDocs = docsByPaymentId.getOrDefault(payment.getId(), List.of());
            DocumentResponse proofOfPayment = paymentDocs.stream()
                    .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Proof"))
                    .findFirst()
                    .map(documentMapper::toResponse)
                    .orElse(null);
            DocumentResponse receipt = paymentDocs.stream()
                    .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Receipt"))
                    .findFirst()
                    .map(documentMapper::toResponse)
                    .orElse(null);

            responses.add(new PaymentResponse(
                    base.identifier(),
                    contractSummary, tenantSummary, propertySummary,
                    base.amount(), base.currency(),
                    receivedAmount, balance,
                    base.paymentDate(), base.dueDate(), base.status(), base.notes(),
                    proofOfPayment, receipt, receivalResponses,
                    base.createdAt(), base.updatedAt()
            ));
        }
        return responses;
    }

    private PaymentResponse enrichPaymentResponse(Payment payment, UUID teamId) {
        PaymentResponse response = paymentMapper.toResponse(payment);

        // Get receivals and calculate balance
        List<PaymentReceival> receivals = receivalRepository.findByPaymentIdAndTeamId(payment.getId(), teamId);
        BigDecimal receivedAmount = receivals.stream()
                .map(PaymentReceival::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = payment.getAmount().subtract(receivedAmount);
        List<PaymentReceivalResponse> receivalResponses = receivals.stream()
                .map(receivalMapper::toResponse)
                .toList();

        Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), teamId)
                .orElse(null);

        if (contract != null) {
            ContractSummary contractSummary = contractMapper.toSummary(contract);

            PropertySummary propertySummary = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId)
                    .map(propertyMapper::toSummary)
                    .orElse(null);

            TenantSummary tenantSummary = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId)
                    .map(tenantMapper::toSummary)
                    .orElse(null);

            DocumentResponse proofOfPayment = documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId)
                    .stream()
                    .filter(doc -> doc.getTitle() != null && doc.getTitle().contains("Proof"))
                    .findFirst()
                    .map(documentMapper::toResponse)
                    .orElse(null);

            DocumentResponse receipt = documentRepository.findByEntityAndTeamId("PAYMENT", payment.getId(), teamId)
                    .stream()
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
                    response.updatedAt()
            );
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
                response.updatedAt()
        );
    }
}
