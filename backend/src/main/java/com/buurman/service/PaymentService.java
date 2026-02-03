package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.ContractSummary;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.TenantSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PaymentMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final PaymentMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final PropertyMapper propertyMapper;
    private final TenantMapper tenantMapper;
    private final AuditService auditService;
    private final com.buurman.mapper.DocumentMapper documentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            ContractRepository contractRepository,
            PropertyRepository propertyRepository,
            TenantRepository tenantRepository,
            DocumentRepository documentRepository,
            PaymentMapper paymentMapper,
            ContractMapper contractMapper,
            PropertyMapper propertyMapper,
            TenantMapper tenantMapper,
            AuditService auditService,
            com.buurman.mapper.DocumentMapper documentMapper) {
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
        this.documentRepository = documentRepository;
        this.paymentMapper = paymentMapper;
        this.contractMapper = contractMapper;
        this.propertyMapper = propertyMapper;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
        this.documentMapper = documentMapper;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse createPayment(CreatePaymentRequest request, UserPrincipal principal) {
        // Validate contract exists and belongs to team
        Contract contract = contractRepository.findByIdAndTeamId(request.contractId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        Payment payment = paymentMapper.toEntity(request);
        payment.setIdentifier(UlidGenerator.generate());
        payment.setTeamId(principal.getTeamId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedBy(principal.getUserId());
        payment.setUpdatedBy(principal.getUserId());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        // Set defaults
        if (payment.getCurrency() == null || payment.getCurrency().isEmpty()) {
            payment.setCurrency(contract.getCurrency() != null ? contract.getCurrency() : "EUR");
        }

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Created payment {} for contract {} by user {}",
                savedPayment.getIdentifier(), contract.getIdentifier(), principal.getUserId());

        auditService.logCreate(principal.getTeamId(), "PAYMENT", savedPayment.getId(), principal.getUserId(), savedPayment);

        return enrichPaymentResponse(savedPayment, principal.getTeamId());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));

        return enrichPaymentResponse(payment, principal.getTeamId());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(UserPrincipal principal) {
        List<Payment> payments = paymentRepository.findAllByTeamId(principal.getTeamId());

        // Update statuses for overdue payments
        LocalDate today = LocalDate.now();
        payments.forEach(payment -> updatePaymentStatus(payment, today));

        return payments.stream()
                .map(payment -> enrichPaymentResponse(payment, principal.getTeamId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByContract(UUID contractId, UserPrincipal principal) {
        // Verify contract belongs to team
        contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        List<Payment> payments = paymentRepository.findByContractId(contractId, principal.getTeamId());

        LocalDate today = LocalDate.now();
        payments.forEach(payment -> updatePaymentStatus(payment, today));

        return payments.stream()
                .map(payment -> enrichPaymentResponse(payment, principal.getTeamId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getOverduePayments(UserPrincipal principal) {
        List<Payment> payments = paymentRepository.findOverduePayments(principal.getTeamId());

        return payments.stream()
                .map(payment -> enrichPaymentResponse(payment, principal.getTeamId()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse updatePayment(UUID id, UpdatePaymentRequest request, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));

        PaymentResponse oldState = enrichPaymentResponse(payment, principal.getTeamId());

        paymentMapper.updateEntity(payment, request);
        payment.setUpdatedBy(principal.getUserId());
        payment.setUpdatedAt(Instant.now());

        Payment updatedPayment = paymentRepository.save(payment);
        PaymentResponse newState = enrichPaymentResponse(updatedPayment, principal.getTeamId());

        log.info("Updated payment {} by user {}", updatedPayment.getIdentifier(), principal.getUserId());

        auditService.logUpdate(principal.getTeamId(), "PAYMENT", updatedPayment.getId(), principal.getUserId(),
                oldState, newState, auditService.getChangedFields(oldState, newState));

        return newState;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse markPaymentAsPaid(UUID id, MarkPaidRequest request, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));

        // Validate payment can be marked as paid
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Payment is already marked as paid");
        }

        if (payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot mark a cancelled payment as paid");
        }

        PaymentResponse oldState = enrichPaymentResponse(payment, principal.getTeamId());

        payment.setPaymentDate(request.paymentDate());
        payment.setStatus(Payment.PaymentStatus.PAID);
        if (request.notes() != null && !request.notes().isEmpty()) {
            payment.setNotes(request.notes());
        }
        payment.setUpdatedBy(principal.getUserId());
        payment.setUpdatedAt(Instant.now());

        Payment updatedPayment = paymentRepository.save(payment);
        PaymentResponse newState = enrichPaymentResponse(updatedPayment, principal.getTeamId());

        log.info("Marked payment {} as PAID on {} by user {}",
                updatedPayment.getIdentifier(), request.paymentDate(), principal.getUserId());

        auditService.logUpdate(principal.getTeamId(), "PAYMENT", updatedPayment.getId(), principal.getUserId(),
                oldState, newState, auditService.getChangedFields(oldState, newState));

        return newState;
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deletePayment(UUID id, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdAndTeamId(id, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));

        // Prevent deleting paid payments
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Cannot delete a paid payment. Please cancel it instead.");
        }

        paymentRepository.softDeleteByIdAndTeamId(id, principal.getTeamId());

        log.info("Deleted payment {} by user {}", payment.getIdentifier(), principal.getUserId());

        auditService.logDelete(principal.getTeamId(), "PAYMENT", id, principal.getUserId(), payment);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public List<PaymentResponse> bulkGeneratePayments(BulkGeneratePaymentsRequest request, UserPrincipal principal) {
        // Parse the month (YYYY-MM format)
        YearMonth month = YearMonth.parse(request.forMonth());

        // Find all active contracts
        List<Contract> activeContracts = contractRepository.findByStatus(Contract.ContractStatus.ACTIVE, principal.getTeamId());

        if (activeContracts.isEmpty()) {
            log.warn("No active contracts found for team {} to generate payments", principal.getTeamId());
            return List.of();
        }

        List<Payment> generatedPayments = new ArrayList<>();

        for (Contract contract : activeContracts) {
            // Calculate due date based on payment frequency and payment due day
            LocalDate dueDate = calculateDueDate(month, contract);

            // Check if payment already exists for this contract and due date
            List<Payment> existingPayments = paymentRepository.findByContractId(contract.getId(), principal.getTeamId());
            boolean paymentExists = existingPayments.stream()
                    .anyMatch(p -> p.getDueDate().equals(dueDate));

            if (paymentExists) {
                log.debug("Payment already exists for contract {} on {}, skipping",
                        contract.getIdentifier(), dueDate);
                continue;
            }

            // Create payment
            Payment payment = new Payment();
            payment.setIdentifier(UlidGenerator.generate());
            payment.setTeamId(principal.getTeamId());
            payment.setContractId(contract.getId());
            payment.setAmount(contract.getRentAmount());
            payment.setCurrency(contract.getCurrency());
            payment.setDueDate(dueDate);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setNotes("Auto-generated for " + month);
            payment.setCreatedBy(principal.getUserId());
            payment.setUpdatedBy(principal.getUserId());
            payment.setCreatedAt(Instant.now());
            payment.setUpdatedAt(Instant.now());

            Payment savedPayment = paymentRepository.save(payment);
            generatedPayments.add(savedPayment);

            log.info("Generated payment {} for contract {} due on {}",
                    savedPayment.getIdentifier(), contract.getIdentifier(), dueDate);
        }

        // Log bulk generation as an audit entry
        if (!generatedPayments.isEmpty()) {
            auditService.logCreate(principal.getTeamId(), "BULK_PAYMENT_GENERATION",
                    generatedPayments.get(0).getId(), principal.getUserId(),
                    "Generated " + generatedPayments.size() + " payments for " + month);
        }

        return generatedPayments.stream()
                .map(payment -> enrichPaymentResponse(payment, principal.getTeamId()))
                .toList();
    }

    // Helper methods

    private LocalDate calculateDueDate(YearMonth month, Contract contract) {
        Integer paymentDueDay = contract.getPaymentDueDay();
        if (paymentDueDay == null) {
            paymentDueDay = 1; // Default to first day of month
        }

        // Ensure day is valid for the month
        int maxDayInMonth = month.lengthOfMonth();
        int actualDay = Math.min(paymentDueDay, maxDayInMonth);

        return month.atDay(actualDay);
    }

    private void updatePaymentStatus(Payment payment, LocalDate today) {
        // Update status to OVERDUE if payment is PENDING and past due date
        if (payment.getStatus() == Payment.PaymentStatus.PENDING &&
                payment.getDueDate().isBefore(today)) {
            payment.setStatus(Payment.PaymentStatus.OVERDUE);
            // Note: Not saving here as this is read-only context
            // In production, you might want a background job to update statuses
        }
    }

    private PaymentResponse enrichPaymentResponse(Payment payment, UUID teamId) {
        PaymentResponse response = paymentMapper.toResponse(payment);

        // Enrich with contract, tenant, property summaries
        Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), teamId)
                .orElse(null);

        if (contract != null) {
            ContractSummary contractSummary = contractMapper.toSummary(contract);

            // Get property and tenant
            PropertySummary propertySummary = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId)
                    .map(propertyMapper::toSummary)
                    .orElse(null);

            TenantSummary tenantSummary = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId)
                    .map(tenantMapper::toSummary)
                    .orElse(null);

            // Get proof of payment and receipt documents
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
                    response.id(),
                    response.identifier(),
                    response.teamId(),
                    contractSummary,
                    tenantSummary,
                    propertySummary,
                    response.amount(),
                    response.currency(),
                    response.paymentDate(),
                    response.dueDate(),
                    response.status(),
                    response.notes(),
                    proofOfPayment,
                    receipt,
                    response.createdAt(),
                    response.updatedAt()
            );
        }

        return response;
    }
}
