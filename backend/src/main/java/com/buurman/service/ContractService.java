package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.DRAFT;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.PENDING_SIGNATURE;
import static com.buurman.domain.Contract.ContractStatus.TERMINATED;
import static com.buurman.domain.Contract.ContractType.FIXED_TERM;
import static com.buurman.domain.Property.PropertyStatus.OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.VACANT;
import static com.buurman.util.UlidGenerator.newContractId;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Ulid;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Document;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantSummary;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

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
  private final NotificationService notificationService;
  private final ContractPartyService contractPartyService;
  private final ContractRentPeriodService contractRentPeriodService;
  private final CountryMetadataSerializer countryMetadataSerializer;
  private final CountryMetadataValidator countryMetadataValidator;
  private final AppProperties appProperties;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse createContract(CreateContractRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    // Resolve property by identifier
    Property property =
        propertyRepository.getByIdentifierAndTeamId(request.propertyIdentifier(), teamId);

    // Check no active contract exists for property
    contractRepository
        .findActiveContractByPropertyId(property.getId(), teamId)
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException(
                  "Property already has an active contract. Please terminate the existing contract"
                      + " first.");
            });

    // Validate dates
    if (request.endDate().isPresent() && request.endDate().get().isBefore(request.startDate())) {
      throw new IllegalArgumentException("End date must be on or after start date");
    }

    // Validate FIXED_TERM contracts have end date
    if (request.contractType() == FIXED_TERM && request.endDate().isEmpty()) {
      throw new IllegalArgumentException("FIXED_TERM contracts must have an end date");
    }

    Contract contract = contractMapper.toEntity(request);
    contract.setPropertyId(property.getId());
    contract.setIdentifier(Optional.of(newContractId()));
    contract.setTeamId(teamId);
    contract.setStatus(DRAFT);
    contract.setCreatedBy(principal.getUserId());
    contract.setUpdatedBy(principal.getUserId());
    contract.setCreatedAt(clock.instant());
    contract.setUpdatedAt(clock.instant());

    // Resolve country code from property
    String countryCode = CountryMetadataRegistry.normalizeCountryCode(property.getCountry());
    contract.setCountryCode(Optional.ofNullable(countryCode));

    // Deserialize and validate country metadata from request
    if (request.countryMetadata() != null && countryCode != null) {
      ContractCountryMetadata metadata =
          countryMetadataSerializer.deserializeFromMap(request.countryMetadata(), countryCode);
      if (metadata != null) {
        countryMetadataValidator.validate(countryCode, metadata);
        contract.setCountryMetadata(Optional.of(metadata));
      }
    }

    // Validate currencies
    validateCurrencyRequired(contract.getRentAmountCurrency(), contract.getRentAmount());
    validateCurrencyRequired(
        contract.getDepositAmountCurrency().orElse(null), contract.getDepositAmount().orElse(null));
    validateCurrencyRequired(
        contract.getSecurityDepositCurrency().orElse(null),
        contract.getSecurityDeposit().orElse(null));

    Contract savedContract = contractRepository.save(contract);

    // Create parties
    contractPartyService.createPartiesForContract(
        savedContract.getId(), request.parties(), principal);

    // Create initial rent period
    contractRentPeriodService.createInitialRentPeriod(savedContract, principal);

    metricsService.incrementCounter("contract.total");
    metricsService.recordHistogram(
        "contract.rent.amount",
        savedContract.getRentAmount().doubleValue(),
        "currency",
        savedContract.getRentAmountCurrency());

    log.info(
        "Contract created: {} for property {} in team {}",
        savedContract.getId(),
        property.getId(),
        teamId);

    // Log to audit trail
    auditService.logCreate(
        teamId, "CONTRACT", savedContract.getId(), principal.getUserId(), savedContract);

    // Get primary tenant for notification
    Tenant primaryTenant =
        contractPartyService.getPrimaryTenantForContract(savedContract.getId(), teamId);

    String propertyName = property.getStreet() + ", " + property.getCity();
    String tenantName =
        primaryTenant.getFirstName() + primaryTenant.getLastName().map(n -> " " + n).orElse("");
    Map<String, Object> contractVars = new HashMap<>();
    contractVars.put("propertyName", propertyName);
    contractVars.put("tenantName", tenantName);
    contractVars.put(
        "rentAmount", savedContract.getRentAmountCurrency() + " " + savedContract.getRentAmount());
    contractVars.put("startDate", savedContract.getStartDate().toString());
    contractVars.put("endDate", savedContract.getEndDate().map(LocalDate::toString).orElse(""));
    contractVars.put("baseUrl", appProperties.email().baseUrl());
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(teamId)
            .notificationType(NotificationType.CONTRACT_CREATED)
            .templateName("contract-created")
            .templateVariables(contractVars)
            .createdBy(principal.getUserId())
            .build());

    return toResponse(savedContract, property, teamId);
  }

  public List<ContractResponse> getAllContracts(UserPrincipal principal) {
    List<Contract> contracts = contractRepository.findAllByTeamId(principal.requireTeamId());
    return toResponses(contracts, principal.requireTeamId());
  }

  public PageResponse<ContractResponse> getContractsPaginated(
      UserPrincipal principal, @Nullable String status, PageRequest pageRequest) {
    PaginatedResult<Contract> result =
        contractRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(), status, null, null, pageRequest);
    List<ContractResponse> responses = toResponses(result.items(), principal.requireTeamId());
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public List<ContractResponse> getContractsByProperty(
      Ulid propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    return toResponses(contracts, teamId);
  }

  public List<ContractResponse> getContractsByTenant(
      Ulid tenantIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, teamId);

    List<Contract> contracts = contractRepository.findByTenantIdViaParties(tenant.getId(), teamId);
    return toResponses(contracts, teamId);
  }

  public List<ContractResponse> getContractsByStatus(
      Contract.ContractStatus status, UserPrincipal principal) {
    List<Contract> contracts = contractRepository.findByStatus(status, principal.requireTeamId());
    return toResponses(contracts, principal.requireTeamId());
  }

  public ContractResponse getContract(Ulid identifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return toResponse(contract, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse updateContract(
      Ulid identifier, UpdateContractRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Prevent updates to ACTIVE, TERMINATED, or EXPIRED contracts (except via status change)
    if (contract.getStatus() == ACTIVE) {
      throw new IllegalArgumentException(
          "Cannot update ACTIVE contracts. Please change status first.");
    }
    if (contract.getStatus() == TERMINATED) {
      throw new IllegalArgumentException("Cannot update TERMINATED contracts.");
    }
    if (contract.getStatus() == EXPIRED) {
      throw new IllegalArgumentException("Cannot update EXPIRED contracts.");
    }

    // Resolve property by identifier
    Property property =
        propertyRepository.getByIdentifierAndTeamId(request.propertyIdentifier(), teamId);

    // Validate dates
    if (request.endDate().isPresent() && request.endDate().get().isBefore(request.startDate())) {
      throw new IllegalArgumentException("End date must be on or after start date");
    }

    // Store old values for audit
    Contract oldContract =
        new Contract(
            contract.getId(),
            contract.getIdentifier(),
            contract.getTeamId(),
            contract.getPropertyId(),
            contract.getContractType(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getSignedDate(),
            contract.getRentAmount(),
            contract.getDepositAmount(),
            contract.getSecurityDeposit(),
            contract.getRentAmountCurrency(),
            contract.getDepositAmountCurrency(),
            contract.getSecurityDepositCurrency(),
            contract.getPaymentFrequency(),
            contract.getPaymentDueDay(),
            contract.getAutoRenewal(),
            contract.getRenewalNoticeDays(),
            contract.getTerminationNoticeDays(),
            contract.getLateFeePercentage(),
            contract.getStatus(),
            contract.getTermsAndConditions(),
            contract.getNotes(),
            contract.getCountryCode(),
            contract.getCountryMetadata(),
            contract.getCreatedAt(),
            contract.getUpdatedAt(),
            contract.getCreatedBy(),
            contract.getUpdatedBy(),
            contract.getDeletedAt());

    // Update fields
    contractMapper.updateEntity(contract, request);
    // Explicitly set clearable Optional fields — MapStruct's IGNORE strategy
    // treats Optional.empty() as non-null, preventing these from being cleared.
    contract.setEndDate(request.endDate());
    contract.setSignedDate(request.signedDate());
    contract.setPropertyId(property.getId());
    contract.setUpdatedBy(principal.getUserId());
    contract.setUpdatedAt(clock.instant());

    // Update country code and metadata (only while DRAFT — locked after activation)
    if (contract.getStatus() == DRAFT) {
      String countryCode = CountryMetadataRegistry.normalizeCountryCode(property.getCountry());
      contract.setCountryCode(Optional.ofNullable(countryCode));

      if (request.countryMetadata() != null && countryCode != null) {
        ContractCountryMetadata metadata =
            countryMetadataSerializer.deserializeFromMap(request.countryMetadata(), countryCode);
        if (metadata != null) {
          countryMetadataValidator.validate(countryCode, metadata);
          contract.setCountryMetadata(Optional.of(metadata));
        } else {
          contract.setCountryMetadata(Optional.empty());
        }
      }
    } else if (request.countryMetadata() != null) {
      throw new BadRequestException(
          "Country metadata can only be modified while the contract is in DRAFT status");
    }

    // Validate currencies
    validateCurrencyRequired(contract.getRentAmountCurrency(), contract.getRentAmount());
    validateCurrencyRequired(
        contract.getDepositAmountCurrency().orElse(null), contract.getDepositAmount().orElse(null));
    validateCurrencyRequired(
        contract.getSecurityDepositCurrency().orElse(null),
        contract.getSecurityDeposit().orElse(null));

    Contract updatedContract = contractRepository.save(contract);

    // Update initial rent period if rent or start date changed on DRAFT
    if (oldContract.getRentAmount().compareTo(updatedContract.getRentAmount()) != 0
        || !oldContract.getStartDate().equals(updatedContract.getStartDate())) {
      contractRentPeriodService.updateInitialRentPeriod(updatedContract, principal);
    }

    log.info("Contract updated: {} in team {}", identifier, teamId);

    // Determine changed fields for audit
    Map<String, Object> changedFields = new HashMap<>();
    if (!oldContract.getPropertyId().equals(updatedContract.getPropertyId())) {
      changedFields.put("propertyId", updatedContract.getPropertyId());
    }
    if (!oldContract.getContractType().equals(updatedContract.getContractType())) {
      changedFields.put("contractType", updatedContract.getContractType());
    }
    if (!oldContract.getStartDate().equals(updatedContract.getStartDate())) {
      changedFields.put("startDate", updatedContract.getStartDate());
    }
    if (!oldContract.getEndDate().equals(updatedContract.getEndDate())) {
      changedFields.put("endDate", updatedContract.getEndDate().orElse(null));
    }
    if (!oldContract.getSignedDate().equals(updatedContract.getSignedDate())) {
      changedFields.put("signedDate", updatedContract.getSignedDate().orElse(null));
    }
    if (oldContract.getRentAmount().compareTo(updatedContract.getRentAmount()) != 0) {
      changedFields.put("rentAmount", updatedContract.getRentAmount());
    }
    if (optionalBigDecimalNotEquals(
        oldContract.getDepositAmount(), updatedContract.getDepositAmount())) {
      changedFields.put("depositAmount", updatedContract.getDepositAmount().orElse(null));
    }
    if (optionalBigDecimalNotEquals(
        oldContract.getSecurityDeposit(), updatedContract.getSecurityDeposit())) {
      changedFields.put("securityDeposit", updatedContract.getSecurityDeposit().orElse(null));
    }
    if (!java.util.Objects.equals(
        oldContract.getRentAmountCurrency(), updatedContract.getRentAmountCurrency())) {
      changedFields.put("rentAmountCurrency", updatedContract.getRentAmountCurrency());
    }
    if (!oldContract
        .getDepositAmountCurrency()
        .equals(updatedContract.getDepositAmountCurrency())) {
      changedFields.put(
          "depositAmountCurrency", updatedContract.getDepositAmountCurrency().orElse(null));
    }
    if (!oldContract
        .getSecurityDepositCurrency()
        .equals(updatedContract.getSecurityDepositCurrency())) {
      changedFields.put(
          "securityDepositCurrency", updatedContract.getSecurityDepositCurrency().orElse(null));
    }
    if (!oldContract.getPaymentFrequency().equals(updatedContract.getPaymentFrequency())) {
      changedFields.put("paymentFrequency", updatedContract.getPaymentFrequency());
    }
    if (!oldContract.getPaymentDueDay().equals(updatedContract.getPaymentDueDay())) {
      changedFields.put("paymentDueDay", updatedContract.getPaymentDueDay().orElse(null));
    }
    if (!java.util.Objects.equals(oldContract.getAutoRenewal(), updatedContract.getAutoRenewal())) {
      changedFields.put("autoRenewal", updatedContract.getAutoRenewal());
    }
    if (!java.util.Objects.equals(
        oldContract.getRenewalNoticeDays(), updatedContract.getRenewalNoticeDays())) {
      changedFields.put("renewalNoticeDays", updatedContract.getRenewalNoticeDays());
    }
    if (!java.util.Objects.equals(
        oldContract.getTerminationNoticeDays(), updatedContract.getTerminationNoticeDays())) {
      changedFields.put("terminationNoticeDays", updatedContract.getTerminationNoticeDays());
    }
    if (optionalBigDecimalNotEquals(
        oldContract.getLateFeePercentage(), updatedContract.getLateFeePercentage())) {
      changedFields.put("lateFeePercentage", updatedContract.getLateFeePercentage().orElse(null));
    }
    if (!oldContract.getTermsAndConditions().equals(updatedContract.getTermsAndConditions())) {
      changedFields.put("termsAndConditions", updatedContract.getTermsAndConditions().orElse(null));
    }
    if (!oldContract.getNotes().equals(updatedContract.getNotes())) {
      changedFields.put("notes", updatedContract.getNotes().orElse(null));
    }

    // Log to audit trail
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        updatedContract.getId(),
        principal.getUserId(),
        oldContract,
        updatedContract,
        changedFields);

    return toResponse(updatedContract, property, teamId);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteContract(Ulid identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Prevent deletion of ACTIVE contracts
    if (contract.getStatus() == ACTIVE) {
      throw new IllegalArgumentException(
          "Cannot delete ACTIVE contracts. Please terminate the contract first.");
    }

    contractPartyService.softDeletePartiesForContract(contract.getId(), teamId);
    contractRepository.softDeleteByIdAndTeamId(contract.getId(), teamId);
    log.info("Contract soft deleted: {} in team {}", identifier, teamId);

    // Log to audit trail
    auditService.logDelete(teamId, "CONTRACT", contract.getId(), principal.getUserId(), contract);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse changeContractStatus(
      Ulid identifier, ChangeContractStatusRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    UUID contractId = contract.getId();
    Contract.ContractStatus oldStatus = contract.getStatus();
    Contract.ContractStatus newStatus = request.status();

    // Validate status transitions
    validateStatusTransition(oldStatus, newStatus);

    // If changing to ACTIVE, ensure no other active contract on property
    if (newStatus == ACTIVE) {
      contractRepository
          .findActiveContractByPropertyId(contract.getPropertyId(), teamId)
          .ifPresent(
              existing -> {
                if (!existing.getId().equals(contractId)) {
                  throw new IllegalArgumentException(
                      "Property already has an active contract. Please terminate the existing"
                          + " contract first.");
                }
              });
    }

    // Store old values for audit
    Contract oldContract =
        new Contract(
            contract.getId(),
            contract.getIdentifier(),
            contract.getTeamId(),
            contract.getPropertyId(),
            contract.getContractType(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getSignedDate(),
            contract.getRentAmount(),
            contract.getDepositAmount(),
            contract.getSecurityDeposit(),
            contract.getRentAmountCurrency(),
            contract.getDepositAmountCurrency(),
            contract.getSecurityDepositCurrency(),
            contract.getPaymentFrequency(),
            contract.getPaymentDueDay(),
            contract.getAutoRenewal(),
            contract.getRenewalNoticeDays(),
            contract.getTerminationNoticeDays(),
            contract.getLateFeePercentage(),
            contract.getStatus(),
            contract.getTermsAndConditions(),
            contract.getNotes(),
            contract.getCountryCode(),
            contract.getCountryMetadata(),
            contract.getCreatedAt(),
            contract.getUpdatedAt(),
            contract.getCreatedBy(),
            contract.getUpdatedBy(),
            contract.getDeletedAt());

    contract.setStatus(newStatus);
    contract.setUpdatedBy(principal.getUserId());
    contract.setUpdatedAt(clock.instant());

    Contract updatedContract = contractRepository.save(contract);

    metricsService.incrementCounter(
        "contract.status.changed.total",
        "from_status",
        oldStatus.name(),
        "to_status",
        newStatus.name());

    log.info(
        "Contract status changed: {} from {} to {} in team {}",
        identifier,
        oldStatus,
        newStatus,
        teamId);

    // Trigger payment scheduling on status change
    paymentSchedulingService.handleContractStatusChange(
        contractId, newStatus, teamId, principal.getUserId());

    // Update property status based on contract status
    updatePropertyStatusBasedOnContract(contract.getPropertyId(), newStatus, oldStatus, principal);

    // Log to audit trail
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("status", newStatus);
    request.reason().ifPresent(r -> changedFields.put("statusChangeReason", r));

    auditService.logUpdate(
        teamId,
        "CONTRACT",
        updatedContract.getId(),
        principal.getUserId(),
        oldContract,
        updatedContract,
        changedFields);

    Property statusChangeProperty =
        propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
    Tenant primaryTenant =
        contractPartyService.getPrimaryTenantForContract(contract.getId(), teamId);
    String scPropertyName =
        statusChangeProperty != null
            ? statusChangeProperty.getStreet() + ", " + statusChangeProperty.getCity()
            : identifier.value();
    String scTenantName =
        primaryTenant.getFirstName() + primaryTenant.getLastName().map(n -> " " + n).orElse("");
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(teamId)
            .notificationType(NotificationType.CONTRACT_STATUS_CHANGED)
            .templateName("contract-status-changed")
            .templateVariables(
                Map.of(
                    "propertyName", scPropertyName,
                    "tenantName", scTenantName,
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name(),
                    "baseUrl", appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return toResponse(updatedContract, teamId);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse reopenContract(Ulid identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Only TERMINATED or EXPIRED contracts can be reopened
    if (contract.getStatus() != TERMINATED && contract.getStatus() != EXPIRED) {
      throw new IllegalArgumentException(
          String.format(
              "Only TERMINATED or EXPIRED contracts can be reopened. Current status: %s",
              contract.getStatus()));
    }

    Contract.ContractStatus oldStatus = contract.getStatus();

    // Store old values for audit
    Contract oldContract =
        new Contract(
            contract.getId(),
            contract.getIdentifier(),
            contract.getTeamId(),
            contract.getPropertyId(),
            contract.getContractType(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getSignedDate(),
            contract.getRentAmount(),
            contract.getDepositAmount(),
            contract.getSecurityDeposit(),
            contract.getRentAmountCurrency(),
            contract.getDepositAmountCurrency(),
            contract.getSecurityDepositCurrency(),
            contract.getPaymentFrequency(),
            contract.getPaymentDueDay(),
            contract.getAutoRenewal(),
            contract.getRenewalNoticeDays(),
            contract.getTerminationNoticeDays(),
            contract.getLateFeePercentage(),
            contract.getStatus(),
            contract.getTermsAndConditions(),
            contract.getNotes(),
            contract.getCountryCode(),
            contract.getCountryMetadata(),
            contract.getCreatedAt(),
            contract.getUpdatedAt(),
            contract.getCreatedBy(),
            contract.getUpdatedBy(),
            contract.getDeletedAt());

    contract.setStatus(DRAFT);
    contract.setUpdatedBy(principal.getUserId());
    contract.setUpdatedAt(clock.instant());

    Contract updatedContract = contractRepository.save(contract);

    metricsService.incrementCounter("contract.reopened.total");

    log.info("Contract reopened: {} from {} to DRAFT in team {}", identifier, oldStatus, teamId);

    // Log to audit trail
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("status", DRAFT);
    changedFields.put("statusChangeReason", "Contract reopened for editing");

    auditService.logUpdate(
        teamId,
        "CONTRACT",
        updatedContract.getId(),
        principal.getUserId(),
        oldContract,
        updatedContract,
        changedFields);

    Property reopenProperty =
        propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
    Tenant primaryTenant =
        contractPartyService.getPrimaryTenantForContract(contract.getId(), teamId);
    String reopenPropertyName =
        reopenProperty != null
            ? reopenProperty.getStreet() + ", " + reopenProperty.getCity()
            : identifier.value();
    String reopenTenantName =
        primaryTenant.getFirstName() + primaryTenant.getLastName().map(n -> " " + n).orElse("");
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(teamId)
            .notificationType(NotificationType.CONTRACT_REOPENED)
            .templateName("contract-reopened")
            .templateVariables(
                Map.of(
                    "propertyName",
                    reopenPropertyName,
                    "tenantName",
                    reopenTenantName,
                    "oldStatus",
                    oldStatus.name(),
                    "baseUrl",
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return toResponse(updatedContract, teamId);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse duplicateContract(Ulid identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract sourceContract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Create new contract with same data (without tenantId)
    @SuppressWarnings("NullAway") // ID is null for new entities, assigned by repository on save
    Contract newContract =
        new Contract(
            null, // New ID will be generated
            Optional.of(newContractId()), // New identifier
            teamId,
            sourceContract.getPropertyId(),
            sourceContract.getContractType(),
            sourceContract.getStartDate(),
            sourceContract.getEndDate(),
            sourceContract.getSignedDate(),
            sourceContract.getRentAmount(),
            sourceContract.getDepositAmount(),
            sourceContract.getSecurityDeposit(),
            sourceContract.getRentAmountCurrency(),
            sourceContract.getDepositAmountCurrency(),
            sourceContract.getSecurityDepositCurrency(),
            sourceContract.getPaymentFrequency(),
            sourceContract.getPaymentDueDay(),
            sourceContract.getAutoRenewal(),
            sourceContract.getRenewalNoticeDays(),
            sourceContract.getTerminationNoticeDays(),
            sourceContract.getLateFeePercentage(),
            DRAFT, // Always start as DRAFT
            sourceContract.getTermsAndConditions(),
            sourceContract.getNotes(),
            sourceContract.getCountryCode(),
            sourceContract.getCountryMetadata(),
            clock.instant(),
            clock.instant(),
            principal.getUserId(),
            principal.getUserId(),
            Optional.empty());

    Contract savedContract = contractRepository.save(newContract);

    // Duplicate parties
    contractPartyService.duplicateParties(
        sourceContract.getId(), savedContract.getId(), teamId, principal.getUserId());

    // Create initial rent period for duplicated contract
    contractRentPeriodService.createInitialRentPeriod(savedContract, principal);

    metricsService.incrementCounter("contract.duplicated.total");

    log.info(
        "Contract duplicated: source {} -> new {} in team {}",
        sourceContract.getId(),
        savedContract.getId(),
        teamId);

    // Log to audit trail
    auditService.logCreate(
        teamId, "CONTRACT", savedContract.getId(), principal.getUserId(), savedContract);

    return toResponse(savedContract, teamId);
  }

  // --- Document delegation methods (resolve identifier to UUID) ---

  public DocumentResponse uploadDocument(
      Ulid contractIdentifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file, "CONTRACT", contract.getId(), contract.getIdentifier().orElseThrow(), title, notes, principal);
  }

  public List<DocumentResponse> getDocuments(Ulid contractIdentifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return documentService.getDocuments("CONTRACT", contract.getId(), principal);
  }

  public URL getDocumentDownloadUrl(Ulid documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    return documentService.getDownloadUrl(document.getIdentifier().orElseThrow(), principal);
  }

  public void deleteDocument(Ulid documentIdentifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(documentIdentifier, principal.requireTeamId());
    documentService.deleteDocument(document.getIdentifier().orElseThrow(), principal);
  }

  public List<RecentActivityResponse> getAuditLog(
      Ulid contractIdentifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "CONTRACT", contract.getId());
  }

  public Map<String, Object> generatePayments(
      Ulid contractIdentifier, GeneratePaymentsRequest request, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    boolean markAsPaid = request.markAsPaid().map(Boolean.TRUE::equals).orElse(false);
    LocalDate paymentDate = markAsPaid ? request.paymentDate().orElse(LocalDate.now(clock)) : null;
    int generated =
        paymentSchedulingService.generatePaymentsManually(
            contract.getId(),
            principal.requireTeamId(),
            principal.getUserId(),
            request.count(),
            markAsPaid,
            paymentDate);
    Map<String, Object> result = new HashMap<>();
    result.put("generated", generated);
    result.put("requested", request.count());
    if (markAsPaid) {
      result.put("markedAsPaid", generated);
    }
    return result;
  }

  // --- Private helpers ---

  private void validateStatusTransition(Contract.ContractStatus from, Contract.ContractStatus to) {
    boolean isValid =
        switch (from) {
          case DRAFT -> to == PENDING_SIGNATURE || to == ACTIVE;
          case PENDING_SIGNATURE -> to == DRAFT || to == ACTIVE;
          case ACTIVE -> to == TERMINATED || to == EXPIRED;
          case EXPIRED, TERMINATED -> false;
        };

    if (!isValid) {
      throw new IllegalArgumentException(
          String.format("Invalid status transition from %s to %s", from, to));
    }
  }

  private ContractResponse toResponse(Contract contract, UUID teamId) {
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    return toResponse(contract, property, teamId);
  }

  private ContractResponse toResponse(Contract contract, Property property, UUID teamId) {
    List<ContractParty> parties =
        contractPartyService.getPartiesForContract(contract.getId(), teamId);
    List<ContractPartyResponse> partyResponses =
        contractPartyService.buildPartyResponses(parties, teamId);

    Optional<TenantSummary> primaryTenant =
        partyResponses.stream()
            .filter(p -> p.role() == ContractPartyRole.PRIMARY_TENANT)
            .map(ContractPartyResponse::tenant)
            .flatMap(Optional::stream)
            .findFirst();

    PropertySummary propertySummary = propertyMapper.toSummary(property);

    return new ContractResponse(
        contract.getIdentifier().orElseThrow(),
        Optional.ofNullable(propertySummary),
        partyResponses,
        primaryTenant,
        contract.getContractType(),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getSignedDate(),
        contract.getRentAmount(),
        contract.getDepositAmount(),
        contract.getSecurityDeposit(),
        contract.getRentAmountCurrency(),
        contract.getDepositAmountCurrency(),
        contract.getSecurityDepositCurrency(),
        contract.getPaymentFrequency(),
        contract.getPaymentDueDay(),
        contract.getAutoRenewal(),
        contract.getRenewalNoticeDays(),
        contract.getTerminationNoticeDays(),
        contract.getLateFeePercentage(),
        contract.getStatus(),
        contract.getTermsAndConditions(),
        contract.getNotes(),
        contract.getCountryCode(),
        contract.getCountryMetadata(),
        contract.getCreatedAt(),
        Optional.of(contract.getUpdatedAt()));
  }

  /** Batch build responses for a list of contracts (avoids N+1 for parties and tenants). */
  private List<ContractResponse> toResponses(List<Contract> contracts, UUID teamId) {
    if (contracts.isEmpty()) {
      return List.of();
    }

    // Batch load properties
    List<UUID> propertyIds = contracts.stream().map(Contract::getPropertyId).distinct().toList();
    Map<UUID, Property> propertyMap =
        propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
            .collect(Collectors.toMap(Property::getId, p -> p));

    // Batch load parties
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    Map<UUID, List<ContractParty>> partiesByContract =
        contractPartyService.getPartiesForContracts(contractIds, teamId);

    // Batch load tenants for all parties
    List<UUID> allTenantIds =
        partiesByContract.values().stream()
            .flatMap(List::stream)
            .flatMap(p -> p.getTenantId().stream())
            .distinct()
            .toList();
    Map<UUID, Tenant> tenantMap =
        tenantRepository.findByIdsAndTeamId(allTenantIds, teamId).stream()
            .collect(Collectors.toMap(Tenant::getId, t -> t));

    return contracts.stream()
        .map(
            contract -> {
              Property property = propertyMap.get(contract.getPropertyId());
              Optional<PropertySummary> propertySummary =
                  Optional.ofNullable(property).map(propertyMapper::toSummary);

              List<ContractParty> parties =
                  partiesByContract.getOrDefault(contract.getId(), List.of());
              List<ContractPartyResponse> partyResponses =
                  parties.stream()
                      .map(
                          party -> {
                            Tenant tenant = party.getTenantId().map(tenantMap::get).orElse(null);
                            Optional<TenantSummary> summary =
                                Optional.ofNullable(tenant).map(tenantMapper::toSummary);
                            return new ContractPartyResponse(
                                party.getIdentifier().orElseThrow(), summary, party.getRole());
                          })
                      .toList();

              Optional<TenantSummary> primaryTenant =
                  partyResponses.stream()
                      .filter(p -> p.role() == ContractPartyRole.PRIMARY_TENANT)
                      .map(ContractPartyResponse::tenant)
                      .flatMap(Optional::stream)
                      .findFirst();

              return new ContractResponse(
                  contract.getIdentifier().orElseThrow(),
                  propertySummary,
                  partyResponses,
                  primaryTenant,
                  contract.getContractType(),
                  contract.getStartDate(),
                  contract.getEndDate(),
                  contract.getSignedDate(),
                  contract.getRentAmount(),
                  contract.getDepositAmount(),
                  contract.getSecurityDeposit(),
                  contract.getRentAmountCurrency(),
                  contract.getDepositAmountCurrency(),
                  contract.getSecurityDepositCurrency(),
                  contract.getPaymentFrequency(),
                  contract.getPaymentDueDay(),
                  contract.getAutoRenewal(),
                  contract.getRenewalNoticeDays(),
                  contract.getTerminationNoticeDays(),
                  contract.getLateFeePercentage(),
                  contract.getStatus(),
                  contract.getTermsAndConditions(),
                  contract.getNotes(),
                  contract.getCountryCode(),
                  contract.getCountryMetadata(),
                  contract.getCreatedAt(),
                  Optional.of(contract.getUpdatedAt()));
            })
        .toList();
  }

  private void validateCurrencyRequired(@Nullable String currency, @Nullable BigDecimal amount) {
    if (currency != null && !currency.isBlank()) {
      try {
        java.util.Currency.getInstance(currency);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid ISO 4217 currency code: " + currency);
      }
      return;
    }
    if (amount != null) {
      throw new BadRequestException("Currency is required when a monetary value is provided");
    }
  }

  private static boolean optionalBigDecimalNotEquals(
      Optional<BigDecimal> a, Optional<BigDecimal> b) {
    if (a.isEmpty() && b.isEmpty()) {
      return false;
    }
    if (a.isEmpty() || b.isEmpty()) {
      return true;
    }
    return a.get().compareTo(b.get()) != 0;
  }

  private void updatePropertyStatusBasedOnContract(
      UUID propertyId,
      Contract.ContractStatus newStatus,
      Contract.ContractStatus oldStatus,
      UserPrincipal principal) {
    Property property =
        propertyRepository.findByIdAndTeamId(propertyId, principal.requireTeamId()).orElse(null);

    if (property == null) {
      log.warn("Property {} not found for contract status update", propertyId);
      return;
    }

    Property.PropertyStatus newPropertyStatus = null;

    if (newStatus == ACTIVE && oldStatus != ACTIVE) {
      newPropertyStatus = OCCUPIED;
    } else if (oldStatus == ACTIVE && (newStatus == EXPIRED || newStatus == TERMINATED)) {
      boolean hasOtherActiveContracts =
          contractRepository
              .findActiveContractByPropertyId(propertyId, principal.requireTeamId())
              .isPresent();

      if (!hasOtherActiveContracts) {
        newPropertyStatus = VACANT;
      }
    }

    if (newPropertyStatus != null && property.getStatus() != newPropertyStatus) {
      property.setStatus(newPropertyStatus);
      property.setUpdatedBy(principal.getUserId());
      property.setUpdatedAt(clock.instant());
      propertyRepository.save(property);

      log.info(
          "Updated property {} status to {} based on contract status change to {}",
          propertyId,
          newPropertyStatus,
          newStatus);
    }
  }
}
