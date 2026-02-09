package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantSummary;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final ContractMapper contractMapper;
    private final PropertyMapper propertyMapper;
    private final TenantMapper tenantMapper;
    private final AuditService auditService;
    private final DocumentService documentService;
    private final PaymentSchedulingService paymentSchedulingService;
    private final MetricsService metricsService;

    public ContractService(
            ContractRepository contractRepository,
            PropertyRepository propertyRepository,
            TenantRepository tenantRepository,
            DocumentRepository documentRepository,
            ContractMapper contractMapper,
            PropertyMapper propertyMapper,
            TenantMapper tenantMapper,
            AuditService auditService,
            DocumentService documentService,
            PaymentSchedulingService paymentSchedulingService,
            MetricsService metricsService) {
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
        this.documentRepository = documentRepository;
        this.contractMapper = contractMapper;
        this.propertyMapper = propertyMapper;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
        this.documentService = documentService;
        this.paymentSchedulingService = paymentSchedulingService;
        this.metricsService = metricsService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse createContract(CreateContractRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        // Resolve property by identifier
        Property property = propertyRepository.findByIdentifierAndTeamId(request.propertyIdentifier(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        // Resolve tenant by identifier
        Tenant tenant = tenantRepository.findByIdentifierAndTeamId(request.tenantIdentifier(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        // Check no active contract exists for property
        contractRepository.findActiveContractByPropertyId(property.getId(), teamId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Property already has an active contract. Please terminate the existing contract first.");
                });

        // Validate dates
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        // Validate FIXED_TERM contracts have end date
        if (request.contractType() == Contract.ContractType.FIXED_TERM && request.endDate() == null) {
            throw new IllegalArgumentException("FIXED_TERM contracts must have an end date");
        }

        Contract contract = contractMapper.toEntity(request);
        contract.setPropertyId(property.getId());
        contract.setTenantId(tenant.getId());
        contract.setIdentifier(UlidGenerator.generate(EntityPrefix.CON));
        contract.setTeamId(teamId);
        contract.setStatus(Contract.ContractStatus.DRAFT);
        contract.setCreatedBy(principal.getUserId());
        contract.setUpdatedBy(principal.getUserId());
        contract.setCreatedAt(Instant.now());
        contract.setUpdatedAt(Instant.now());

        // Set defaults
        if (contract.getCurrency() == null) {
            contract.setCurrency("EUR");
        }
        if (contract.getAutoRenewal() == null) {
            contract.setAutoRenewal(false);
        }

        Contract savedContract = contractRepository.save(contract);

        metricsService.incrementCounter("contract.total");
        metricsService.recordHistogram("contract.rent.amount", savedContract.getRentAmount().doubleValue(),
                "currency", savedContract.getCurrency());

        log.info("Contract created: {} for property {} and tenant {} in team {}",
                savedContract.getId(), property.getId(), tenant.getId(), teamId);

        // Log to audit trail
        auditService.logCreate(
                teamId,
                "CONTRACT",
                savedContract.getId(),
                principal.getUserId(),
                savedContract
        );

        return toResponse(savedContract, property, tenant);
    }

    public List<ContractResponse> getAllContracts(UserPrincipal principal) {
        List<Contract> contracts = contractRepository.findAllByTeamId(principal.getTeamId());
        return contracts.stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
    }

    public PageResponse<ContractResponse> getContractsPaginated(UserPrincipal principal, String status, PageRequest pageRequest) {
        PaginatedResult<Contract> result = contractRepository.findAllByTeamIdPaginated(
                principal.getTeamId(), status, null, null, pageRequest);
        List<ContractResponse> responses = result.items().stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    public List<ContractResponse> getContractsByProperty(String propertyIdentifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Property property = propertyRepository.findByIdentifierAndTeamId(propertyIdentifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
        return contracts.stream()
                .map(contract -> toResponse(contract, teamId))
                .toList();
    }

    public List<ContractResponse> getContractsByTenant(String tenantIdentifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Tenant tenant = tenantRepository.findByIdentifierAndTeamId(tenantIdentifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        List<Contract> contracts = contractRepository.findByTenantId(tenant.getId(), teamId);
        return contracts.stream()
                .map(contract -> toResponse(contract, teamId))
                .toList();
    }

    public List<ContractResponse> getContractsByStatus(Contract.ContractStatus status, UserPrincipal principal) {
        List<Contract> contracts = contractRepository.findByStatus(status, principal.getTeamId());
        return contracts.stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
    }

    public ContractResponse getContract(String identifier, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        return toResponse(contract, principal.getTeamId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse updateContract(String identifier, UpdateContractRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract contract = contractRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Prevent updates to ACTIVE, TERMINATED, or EXPIRED contracts (except via status change)
        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot update ACTIVE contracts. Please change status first.");
        }
        if (contract.getStatus() == Contract.ContractStatus.TERMINATED) {
            throw new IllegalArgumentException("Cannot update TERMINATED contracts.");
        }
        if (contract.getStatus() == Contract.ContractStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot update EXPIRED contracts.");
        }

        // Resolve property by identifier
        Property property = propertyRepository.findByIdentifierAndTeamId(request.propertyIdentifier(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        // Resolve tenant by identifier
        Tenant tenant = tenantRepository.findByIdentifierAndTeamId(request.tenantIdentifier(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        // Validate dates
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        // Store old values for audit
        Contract oldContract = new Contract(
                contract.getId(), contract.getIdentifier(), contract.getTeamId(),
                contract.getPropertyId(), contract.getTenantId(), contract.getContractType(),
                contract.getStartDate(), contract.getEndDate(), contract.getSignedDate(),
                contract.getRentAmount(), contract.getDepositAmount(), contract.getSecurityDeposit(),
                contract.getCurrency(), contract.getPaymentFrequency(), contract.getPaymentDueDay(),
                contract.getAutoRenewal(), contract.getRenewalNoticeDays(), contract.getTerminationNoticeDays(),
                contract.getLateFeePercentage(), contract.getStatus(), contract.getTermsAndConditions(),
                contract.getNotes(), contract.getCreatedAt(), contract.getUpdatedAt(),
                contract.getCreatedBy(), contract.getUpdatedBy(), contract.getDeletedAt()
        );

        // Update fields
        contractMapper.updateEntity(contract, request);
        contract.setPropertyId(property.getId());
        contract.setTenantId(tenant.getId());
        contract.setUpdatedBy(principal.getUserId());
        contract.setUpdatedAt(Instant.now());

        Contract updatedContract = contractRepository.save(contract);
        log.info("Contract updated: {} in team {}", identifier, teamId);

        // Determine changed fields for audit
        Map<String, Object> changedFields = new HashMap<>();
        if (!oldContract.getPropertyId().equals(updatedContract.getPropertyId())) {
            changedFields.put("propertyId", updatedContract.getPropertyId());
        }
        if (!oldContract.getTenantId().equals(updatedContract.getTenantId())) {
            changedFields.put("tenantId", updatedContract.getTenantId());
        }
        if (!oldContract.getContractType().equals(updatedContract.getContractType())) {
            changedFields.put("contractType", updatedContract.getContractType());
        }
        if (!oldContract.getStartDate().equals(updatedContract.getStartDate())) {
            changedFields.put("startDate", updatedContract.getStartDate());
        }
        if (!java.util.Objects.equals(oldContract.getEndDate(), updatedContract.getEndDate())) {
            changedFields.put("endDate", updatedContract.getEndDate());
        }
        if (!java.util.Objects.equals(oldContract.getSignedDate(), updatedContract.getSignedDate())) {
            changedFields.put("signedDate", updatedContract.getSignedDate());
        }
        if (oldContract.getRentAmount().compareTo(updatedContract.getRentAmount()) != 0) {
            changedFields.put("rentAmount", updatedContract.getRentAmount());
        }
        if (!bigDecimalEquals(oldContract.getDepositAmount(), updatedContract.getDepositAmount())) {
            changedFields.put("depositAmount", updatedContract.getDepositAmount());
        }
        if (!bigDecimalEquals(oldContract.getSecurityDeposit(), updatedContract.getSecurityDeposit())) {
            changedFields.put("securityDeposit", updatedContract.getSecurityDeposit());
        }
        if (!oldContract.getCurrency().equals(updatedContract.getCurrency())) {
            changedFields.put("currency", updatedContract.getCurrency());
        }
        if (!oldContract.getPaymentFrequency().equals(updatedContract.getPaymentFrequency())) {
            changedFields.put("paymentFrequency", updatedContract.getPaymentFrequency());
        }
        if (!java.util.Objects.equals(oldContract.getPaymentDueDay(), updatedContract.getPaymentDueDay())) {
            changedFields.put("paymentDueDay", updatedContract.getPaymentDueDay());
        }
        if (!oldContract.getAutoRenewal().equals(updatedContract.getAutoRenewal())) {
            changedFields.put("autoRenewal", updatedContract.getAutoRenewal());
        }
        if (!java.util.Objects.equals(oldContract.getRenewalNoticeDays(), updatedContract.getRenewalNoticeDays())) {
            changedFields.put("renewalNoticeDays", updatedContract.getRenewalNoticeDays());
        }
        if (!java.util.Objects.equals(oldContract.getTerminationNoticeDays(), updatedContract.getTerminationNoticeDays())) {
            changedFields.put("terminationNoticeDays", updatedContract.getTerminationNoticeDays());
        }
        if (!bigDecimalEquals(oldContract.getLateFeePercentage(), updatedContract.getLateFeePercentage())) {
            changedFields.put("lateFeePercentage", updatedContract.getLateFeePercentage());
        }
        if (!java.util.Objects.equals(oldContract.getTermsAndConditions(), updatedContract.getTermsAndConditions())) {
            changedFields.put("termsAndConditions", updatedContract.getTermsAndConditions());
        }
        if (!java.util.Objects.equals(oldContract.getNotes(), updatedContract.getNotes())) {
            changedFields.put("notes", updatedContract.getNotes());
        }

        // Log to audit trail
        auditService.logUpdate(
                teamId,
                "CONTRACT",
                updatedContract.getId(),
                principal.getUserId(),
                oldContract,
                updatedContract,
                changedFields
        );

        return toResponse(updatedContract, property, tenant);
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteContract(String identifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract contract = contractRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Prevent deletion of ACTIVE contracts
        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot delete ACTIVE contracts. Please terminate the contract first.");
        }

        contractRepository.softDeleteByIdAndTeamId(contract.getId(), teamId);
        log.info("Contract soft deleted: {} in team {}", identifier, teamId);

        // Log to audit trail
        auditService.logDelete(
                teamId,
                "CONTRACT",
                contract.getId(),
                principal.getUserId(),
                contract
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse changeContractStatus(String identifier, ChangeContractStatusRequest request, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract contract = contractRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        UUID contractId = contract.getId();
        Contract.ContractStatus oldStatus = contract.getStatus();
        Contract.ContractStatus newStatus = request.status();

        // Validate status transitions
        validateStatusTransition(oldStatus, newStatus);

        // If changing to ACTIVE, ensure no other active contract on property
        if (newStatus == Contract.ContractStatus.ACTIVE) {
            contractRepository.findActiveContractByPropertyId(contract.getPropertyId(), teamId)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(contractId)) {
                            throw new IllegalArgumentException(
                                    "Property already has an active contract. Please terminate the existing contract first.");
                        }
                    });
        }

        // Store old values for audit
        Contract oldContract = new Contract(
                contract.getId(), contract.getIdentifier(), contract.getTeamId(),
                contract.getPropertyId(), contract.getTenantId(), contract.getContractType(),
                contract.getStartDate(), contract.getEndDate(), contract.getSignedDate(),
                contract.getRentAmount(), contract.getDepositAmount(), contract.getSecurityDeposit(),
                contract.getCurrency(), contract.getPaymentFrequency(), contract.getPaymentDueDay(),
                contract.getAutoRenewal(), contract.getRenewalNoticeDays(), contract.getTerminationNoticeDays(),
                contract.getLateFeePercentage(), contract.getStatus(), contract.getTermsAndConditions(),
                contract.getNotes(), contract.getCreatedAt(), contract.getUpdatedAt(),
                contract.getCreatedBy(), contract.getUpdatedBy(), contract.getDeletedAt()
        );

        contract.setStatus(newStatus);
        contract.setUpdatedBy(principal.getUserId());
        contract.setUpdatedAt(Instant.now());

        Contract updatedContract = contractRepository.save(contract);

        metricsService.incrementCounter("contract.status.changed.total",
                "from_status", oldStatus.name(), "to_status", newStatus.name());

        log.info("Contract status changed: {} from {} to {} in team {}",
                identifier, oldStatus, newStatus, teamId);

        // Trigger payment scheduling on status change
        paymentSchedulingService.handleContractStatusChange(
                contractId,
                newStatus,
                teamId,
                principal.getUserId()
        );

        // Update property status based on contract status
        updatePropertyStatusBasedOnContract(contract.getPropertyId(), newStatus, oldStatus, principal);

        // Log to audit trail
        Map<String, Object> changedFields = new HashMap<>();
        changedFields.put("status", newStatus);
        if (request.reason() != null) {
            changedFields.put("statusChangeReason", request.reason());
        }

        auditService.logUpdate(
                teamId,
                "CONTRACT",
                updatedContract.getId(),
                principal.getUserId(),
                oldContract,
                updatedContract,
                changedFields
        );

        return toResponse(updatedContract, teamId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse reopenContract(String identifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract contract = contractRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Only TERMINATED or EXPIRED contracts can be reopened
        if (contract.getStatus() != Contract.ContractStatus.TERMINATED &&
            contract.getStatus() != Contract.ContractStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    String.format("Only TERMINATED or EXPIRED contracts can be reopened. Current status: %s",
                            contract.getStatus()));
        }

        Contract.ContractStatus oldStatus = contract.getStatus();

        // Store old values for audit
        Contract oldContract = new Contract(
                contract.getId(), contract.getIdentifier(), contract.getTeamId(),
                contract.getPropertyId(), contract.getTenantId(), contract.getContractType(),
                contract.getStartDate(), contract.getEndDate(), contract.getSignedDate(),
                contract.getRentAmount(), contract.getDepositAmount(), contract.getSecurityDeposit(),
                contract.getCurrency(), contract.getPaymentFrequency(), contract.getPaymentDueDay(),
                contract.getAutoRenewal(), contract.getRenewalNoticeDays(), contract.getTerminationNoticeDays(),
                contract.getLateFeePercentage(), contract.getStatus(), contract.getTermsAndConditions(),
                contract.getNotes(), contract.getCreatedAt(), contract.getUpdatedAt(),
                contract.getCreatedBy(), contract.getUpdatedBy(), contract.getDeletedAt()
        );

        contract.setStatus(Contract.ContractStatus.DRAFT);
        contract.setUpdatedBy(principal.getUserId());
        contract.setUpdatedAt(Instant.now());

        Contract updatedContract = contractRepository.save(contract);

        metricsService.incrementCounter("contract.reopened.total");

        log.info("Contract reopened: {} from {} to DRAFT in team {}", identifier, oldStatus, teamId);

        // Log to audit trail
        Map<String, Object> changedFields = new HashMap<>();
        changedFields.put("status", Contract.ContractStatus.DRAFT);
        changedFields.put("statusChangeReason", "Contract reopened for editing");

        auditService.logUpdate(
                teamId,
                "CONTRACT",
                updatedContract.getId(),
                principal.getUserId(),
                oldContract,
                updatedContract,
                changedFields
        );

        return toResponse(updatedContract, teamId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse duplicateContract(String identifier, UserPrincipal principal) {
        UUID teamId = principal.getTeamId();

        Contract sourceContract = contractRepository.findByIdentifierAndTeamId(identifier, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Create new contract with same data
        Contract newContract = new Contract(
                null, // New ID will be generated
                UlidGenerator.generate(EntityPrefix.CON), // New identifier
                teamId,
                sourceContract.getPropertyId(),
                sourceContract.getTenantId(),
                sourceContract.getContractType(),
                sourceContract.getStartDate(),
                sourceContract.getEndDate(),
                sourceContract.getSignedDate(),
                sourceContract.getRentAmount(),
                sourceContract.getDepositAmount(),
                sourceContract.getSecurityDeposit(),
                sourceContract.getCurrency(),
                sourceContract.getPaymentFrequency(),
                sourceContract.getPaymentDueDay(),
                sourceContract.getAutoRenewal(),
                sourceContract.getRenewalNoticeDays(),
                sourceContract.getTerminationNoticeDays(),
                sourceContract.getLateFeePercentage(),
                Contract.ContractStatus.DRAFT, // Always start as DRAFT
                sourceContract.getTermsAndConditions(),
                sourceContract.getNotes(),
                Instant.now(),
                Instant.now(),
                principal.getUserId(),
                principal.getUserId(),
                null
        );

        Contract savedContract = contractRepository.save(newContract);

        metricsService.incrementCounter("contract.duplicated.total");

        log.info("Contract duplicated: source {} -> new {} in team {}",
                sourceContract.getId(), savedContract.getId(), teamId);

        // Log to audit trail
        auditService.logCreate(
                teamId,
                "CONTRACT",
                savedContract.getId(),
                principal.getUserId(),
                savedContract
        );

        return toResponse(savedContract, teamId);
    }

    // --- Document delegation methods (resolve identifier to UUID) ---

    public DocumentResponse uploadDocument(String contractIdentifier, MultipartFile file,
                                           String title, String notes, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdentifierAndTeamId(contractIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        return documentService.uploadDocument(file, "CONTRACT", contract.getId(), title, notes, principal);
    }

    public List<DocumentResponse> getDocuments(String contractIdentifier, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdentifierAndTeamId(contractIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        return documentService.getDocuments("CONTRACT", contract.getId(), principal);
    }

    public URL getDocumentDownloadUrl(String documentIdentifier, UserPrincipal principal) {
        Document document = documentRepository.findByIdentifierAndTeamId(documentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found or access denied"));
        return documentService.getDownloadUrl(document.getIdentifier(), principal);
    }

    public void deleteDocument(String documentIdentifier, UserPrincipal principal) {
        Document document = documentRepository.findByIdentifierAndTeamId(documentIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found or access denied"));
        documentService.deleteDocument(document.getIdentifier(), principal);
    }

    public List<RecentActivityResponse> getAuditLog(String contractIdentifier, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdentifierAndTeamId(contractIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        return auditService.getEntityAuditLog(principal.getTeamId(), "CONTRACT", contract.getId());
    }

    public Map<String, Object> generatePayments(String contractIdentifier, GeneratePaymentsRequest request, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdentifierAndTeamId(contractIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        int generated = paymentSchedulingService.generatePaymentsManually(
                contract.getId(),
                principal.getTeamId(),
                principal.getUserId(),
                request.count()
        );
        return Map.of(
                "generated", generated,
                "requested", request.count()
        );
    }

    // --- Private helpers ---

    private void validateStatusTransition(Contract.ContractStatus from, Contract.ContractStatus to) {
        boolean isValid = switch (from) {
            case DRAFT -> to == Contract.ContractStatus.PENDING_SIGNATURE || to == Contract.ContractStatus.ACTIVE;
            case PENDING_SIGNATURE -> to == Contract.ContractStatus.DRAFT || to == Contract.ContractStatus.ACTIVE;
            case ACTIVE -> to == Contract.ContractStatus.TERMINATED || to == Contract.ContractStatus.EXPIRED;
            case EXPIRED, TERMINATED -> false;
        };

        if (!isValid) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    private ContractResponse toResponse(Contract contract, UUID teamId) {
        Property property = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId)
                .orElseThrow(() -> new IllegalStateException("Contract property not found"));
        Tenant tenant = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId)
                .orElseThrow(() -> new IllegalStateException("Contract tenant not found"));
        return toResponse(contract, property, tenant);
    }

    private ContractResponse toResponse(Contract contract, Property property, Tenant tenant) {
        ContractResponse response = contractMapper.toResponse(contract);
        PropertySummary propertySummary = propertyMapper.toSummary(property);
        TenantSummary tenantSummary = tenantMapper.toSummary(tenant);

        return new ContractResponse(
                response.identifier(),
                propertySummary,
                tenantSummary,
                response.contractType(),
                response.startDate(),
                response.endDate(),
                response.signedDate(),
                response.rentAmount(),
                response.depositAmount(),
                response.securityDeposit(),
                response.currency(),
                response.paymentFrequency(),
                response.paymentDueDay(),
                response.autoRenewal(),
                response.renewalNoticeDays(),
                response.terminationNoticeDays(),
                response.lateFeePercentage(),
                response.status(),
                response.termsAndConditions(),
                response.notes(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private static boolean bigDecimalEquals(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    private void updatePropertyStatusBasedOnContract(UUID propertyId, Contract.ContractStatus newStatus,
                                                       Contract.ContractStatus oldStatus, UserPrincipal principal) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElse(null);

        if (property == null) {
            log.warn("Property {} not found for contract status update", propertyId);
            return;
        }

        Property.PropertyStatus newPropertyStatus = null;

        if (newStatus == Contract.ContractStatus.ACTIVE && oldStatus != Contract.ContractStatus.ACTIVE) {
            newPropertyStatus = Property.PropertyStatus.OCCUPIED;
        }
        else if (oldStatus == Contract.ContractStatus.ACTIVE &&
                 (newStatus == Contract.ContractStatus.EXPIRED || newStatus == Contract.ContractStatus.TERMINATED)) {
            boolean hasOtherActiveContracts = contractRepository.findActiveContractByPropertyId(propertyId, principal.getTeamId())
                    .isPresent();

            if (!hasOtherActiveContracts) {
                newPropertyStatus = Property.PropertyStatus.VACANT;
            }
        }

        if (newPropertyStatus != null && property.getStatus() != newPropertyStatus) {
            property.setStatus(newPropertyStatus);
            property.setUpdatedBy(principal.getUserId());
            property.setUpdatedAt(Instant.now());
            propertyRepository.save(property);

            log.info("Updated property {} status to {} based on contract status change to {}",
                    propertyId, newPropertyStatus, newStatus);
        }
    }
}
