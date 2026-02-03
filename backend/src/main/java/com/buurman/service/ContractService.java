package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.TenantSummary;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ContractMapper contractMapper;
    private final PropertyMapper propertyMapper;
    private final TenantMapper tenantMapper;
    private final AuditService auditService;
    private final PaymentSchedulingService paymentSchedulingService;

    public ContractService(
            ContractRepository contractRepository,
            PropertyRepository propertyRepository,
            TenantRepository tenantRepository,
            ContractMapper contractMapper,
            PropertyMapper propertyMapper,
            TenantMapper tenantMapper,
            AuditService auditService,
            PaymentSchedulingService paymentSchedulingService) {
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
        this.contractMapper = contractMapper;
        this.propertyMapper = propertyMapper;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
        this.paymentSchedulingService = paymentSchedulingService;
    }

    @Transactional
    public ContractResponse createContract(CreateContractRequest request, UserPrincipal principal) {
        // Validate property exists and belongs to team
        Property property = propertyRepository.findByIdAndTeamId(request.propertyId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        // Validate tenant exists and belongs to team
        Tenant tenant = tenantRepository.findByIdAndTeamId(request.tenantId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        // Check no active contract exists for property
        contractRepository.findActiveContractByPropertyId(request.propertyId(), principal.getTeamId())
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
        contract.setIdentifier(UlidGenerator.generate());
        contract.setTeamId(principal.getTeamId());
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
        log.info("Contract created: {} for property {} and tenant {} in team {}",
                savedContract.getId(), property.getId(), tenant.getId(), principal.getTeamId());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
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

    public List<ContractResponse> getContractsByProperty(UUID propertyId, UserPrincipal principal) {
        // Verify property exists and belongs to team
        propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        List<Contract> contracts = contractRepository.findByPropertyId(propertyId, principal.getTeamId());
        return contracts.stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
    }

    public List<ContractResponse> getContractsByTenant(UUID tenantId, UserPrincipal principal) {
        // Verify tenant exists and belongs to team
        tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        List<Contract> contracts = contractRepository.findByTenantId(tenantId, principal.getTeamId());
        return contracts.stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
    }

    public List<ContractResponse> getContractsByStatus(Contract.ContractStatus status, UserPrincipal principal) {
        List<Contract> contracts = contractRepository.findByStatus(status, principal.getTeamId());
        return contracts.stream()
                .map(contract -> toResponse(contract, principal.getTeamId()))
                .toList();
    }

    public ContractResponse getContract(UUID contractId, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));
        return toResponse(contract, principal.getTeamId());
    }

    @Transactional
    public ContractResponse updateContract(UUID contractId, UpdateContractRequest request, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Prevent updates to ACTIVE contracts (except via status change)
        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot update ACTIVE contracts. Please change status first.");
        }

        // Validate property exists
        Property property = propertyRepository.findByIdAndTeamId(request.propertyId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found or access denied"));

        // Validate tenant exists
        Tenant tenant = tenantRepository.findByIdAndTeamId(request.tenantId(), principal.getTeamId())
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
        contract.setUpdatedBy(principal.getUserId());
        contract.setUpdatedAt(Instant.now());

        Contract updatedContract = contractRepository.save(contract);
        log.info("Contract updated: {} in team {}", contractId, principal.getTeamId());

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
        if (!oldContract.getRentAmount().equals(updatedContract.getRentAmount())) {
            changedFields.put("rentAmount", updatedContract.getRentAmount());
        }
        if (!java.util.Objects.equals(oldContract.getDepositAmount(), updatedContract.getDepositAmount())) {
            changedFields.put("depositAmount", updatedContract.getDepositAmount());
        }
        if (!java.util.Objects.equals(oldContract.getSecurityDeposit(), updatedContract.getSecurityDeposit())) {
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
        if (!java.util.Objects.equals(oldContract.getLateFeePercentage(), updatedContract.getLateFeePercentage())) {
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
                principal.getTeamId(),
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
    public void deleteContract(UUID contractId, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Prevent deletion of ACTIVE contracts
        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot delete ACTIVE contracts. Please terminate the contract first.");
        }

        contractRepository.softDeleteByIdAndTeamId(contractId, principal.getTeamId());
        log.info("Contract soft deleted: {} in team {}", contractId, principal.getTeamId());

        // Log to audit trail
        auditService.logDelete(
                principal.getTeamId(),
                "CONTRACT",
                contractId,
                principal.getUserId(),
                contract
        );
    }

    @Transactional
    public ContractResponse changeContractStatus(UUID contractId, ChangeContractStatusRequest request, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        Contract.ContractStatus oldStatus = contract.getStatus();
        Contract.ContractStatus newStatus = request.status();

        // Validate status transitions
        validateStatusTransition(oldStatus, newStatus);

        // If changing to ACTIVE, ensure no other active contract on property
        if (newStatus == Contract.ContractStatus.ACTIVE) {
            contractRepository.findActiveContractByPropertyId(contract.getPropertyId(), principal.getTeamId())
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
        log.info("Contract status changed: {} from {} to {} in team {}",
                contractId, oldStatus, newStatus, principal.getTeamId());

        // Trigger payment scheduling on status change
        paymentSchedulingService.handleContractStatusChange(
                contractId,
                newStatus,
                principal.getTeamId(),
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
                principal.getTeamId(),
                "CONTRACT",
                updatedContract.getId(),
                principal.getUserId(),
                oldContract,
                updatedContract,
                changedFields
        );

        return toResponse(updatedContract, principal.getTeamId());
    }

    @Transactional
    public ContractResponse reopenContract(UUID contractId, UserPrincipal principal) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
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
        log.info("Contract reopened: {} from {} to DRAFT in team {}", contractId, oldStatus, principal.getTeamId());

        // Log to audit trail
        Map<String, Object> changedFields = new HashMap<>();
        changedFields.put("status", Contract.ContractStatus.DRAFT);
        changedFields.put("statusChangeReason", "Contract reopened for editing");

        auditService.logUpdate(
                principal.getTeamId(),
                "CONTRACT",
                updatedContract.getId(),
                principal.getUserId(),
                oldContract,
                updatedContract,
                changedFields
        );

        return toResponse(updatedContract, principal.getTeamId());
    }

    @Transactional
    public ContractResponse duplicateContract(UUID contractId, UserPrincipal principal) {
        Contract sourceContract = contractRepository.findByIdAndTeamId(contractId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

        // Create new contract with same data
        Contract newContract = new Contract(
                null, // New ID will be generated
                UlidGenerator.generate(), // New identifier
                principal.getTeamId(),
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
        log.info("Contract duplicated: source {} -> new {} in team {}",
                sourceContract.getId(), savedContract.getId(), principal.getTeamId());

        // Log to audit trail
        Map<String, Object> changedFields = new HashMap<>();
        changedFields.put("duplicatedFrom", sourceContract.getIdentifier());

        auditService.logCreate(
                principal.getTeamId(),
                "CONTRACT",
                savedContract.getId(),
                principal.getUserId(),
                savedContract
        );

        return toResponse(savedContract, principal.getTeamId());
    }

    private void validateStatusTransition(Contract.ContractStatus from, Contract.ContractStatus to) {
        // Define valid transitions
        boolean isValid = switch (from) {
            case DRAFT -> to == Contract.ContractStatus.PENDING_SIGNATURE || to == Contract.ContractStatus.ACTIVE;
            case PENDING_SIGNATURE -> to == Contract.ContractStatus.DRAFT || to == Contract.ContractStatus.ACTIVE;
            case ACTIVE -> to == Contract.ContractStatus.TERMINATED || to == Contract.ContractStatus.EXPIRED;
            case EXPIRED, TERMINATED -> false; // Cannot transition from terminal states
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
                response.id(),
                response.identifier(),
                response.teamId(),
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

    private void updatePropertyStatusBasedOnContract(UUID propertyId, Contract.ContractStatus newStatus,
                                                       Contract.ContractStatus oldStatus, UserPrincipal principal) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElse(null);

        if (property == null) {
            log.warn("Property {} not found for contract status update", propertyId);
            return;
        }

        Property.PropertyStatus newPropertyStatus = null;

        // When contract becomes ACTIVE, set property to OCCUPIED
        if (newStatus == Contract.ContractStatus.ACTIVE && oldStatus != Contract.ContractStatus.ACTIVE) {
            newPropertyStatus = Property.PropertyStatus.OCCUPIED;
        }
        // When contract is no longer active, check if property should be VACANT
        else if (oldStatus == Contract.ContractStatus.ACTIVE &&
                 (newStatus == Contract.ContractStatus.EXPIRED || newStatus == Contract.ContractStatus.TERMINATED)) {
            // Check if there are any other active contracts for this property
            boolean hasOtherActiveContracts = contractRepository.findActiveContractByPropertyId(propertyId, principal.getTeamId())
                    .isPresent();

            if (!hasOtherActiveContracts) {
                newPropertyStatus = Property.PropertyStatus.VACANT;
            }
        }

        // Update property status if needed
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
