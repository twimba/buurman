package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.DRAFT;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.PENDING_SIGNATURE;
import static com.buurman.domain.Contract.ContractStatus.TERMINATED;
import static com.buurman.domain.Contract.ContractType.FIXED_TERM;
import static com.buurman.domain.NotificationType.CONTRACT_CREATED;
import static com.buurman.domain.NotificationType.CONTRACT_REOPENED;
import static com.buurman.domain.NotificationType.CONTRACT_STATUS_CHANGED;
import static com.buurman.domain.Property.PropertyStatus.OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.VACANT;
import static com.buurman.util.SidGenerator.newContractId;
import static com.buurman.util.SidGenerator.newRentComponentId;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
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
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Document;
import com.buurman.domain.Property;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.RentComponentRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.RentComponentResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.ContractRentComponentMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PropertyRepository;
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
  private final ContractExtensionRepository extensionRepository;
  private final ContractExtensionService contractExtensionService;
  private final ContractRentComponentRepository rentComponentRepository;
  private final ContractRentComponentMapper rentComponentMapper;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;
  private final DocumentRepository documentRepository;
  private final ContractMapper contractMapper;
  private final PropertyMapper propertyMapper;
  private final ContactMapper contactMapper;
  private final AuditService auditService;
  private final DocumentService documentService;
  private final PaymentSchedulingService paymentSchedulingService;
  private final MetricsService metricsService;
  private final NotificationService notificationService;
  private final ContractPartyService contractPartyService;
  private final ContractRentPeriodService contractRentPeriodService;
  private final CountryMetadataSerializer countryMetadataSerializer;
  private final CountryMetadataValidator countryMetadataValidator;
  private final CurrencyEnforcementService currencyEnforcement;
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
    String countryCode = CountryMetadataRegistry.normalizeCountryCode(property.getCountryCode());
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
    validateCurrencyRequired(contract.getRentAmount().currency(), contract.getRentAmount().value());
    validateCurrencyRequired(
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::currency).orElse(null),
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::value).orElse(null));
    validateCurrencyRequired(
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::currency).orElse(null),
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::value).orElse(null));

    // Enforce team currency
    currencyEnforcement.validateCurrency(contract.getRentAmount().currency(), teamId);
    contract
        .getDepositAmount()
        .ifPresent(d -> currencyEnforcement.validateCurrency(d.currency(), teamId));
    contract
        .getSecurityDeposit()
        .ifPresent(d -> currencyEnforcement.validateCurrency(d.currency(), teamId));

    // Override rent amount if components provided
    request
        .rentComponents()
        .filter(list -> !list.isEmpty())
        .ifPresent(
            components -> {
              validateRentComponents(components);
              contract.setRentAmount(
                  computeRentFromComponents(components, contract.getRentAmount().currency()));
            });

    Contract savedContract = contractRepository.save(contract);

    // Create parties
    contractPartyService.createPartiesForContract(
        savedContract.getId(), request.parties(), principal);

    // Create initial rent period and save rent components linked to it
    ContractRentPeriod initialPeriod =
        contractRentPeriodService.createInitialRentPeriod(savedContract, principal);
    request
        .rentComponents()
        .filter(list -> !list.isEmpty())
        .ifPresent(
            components ->
                saveRentComponents(savedContract, initialPeriod.getId(), components, principal));

    metricsService.incrementCounter("contract.total");
    metricsService.recordHistogram(
        "contract.rent.amount",
        savedContract.getRentAmount().value().doubleValue(),
        "currency",
        savedContract.getRentAmount().currency());

    log.info(
        "Contract created: {} for property {} in team {}",
        savedContract.getId(),
        property.getId(),
        teamId);

    // Log to audit trail
    auditService.logCreate(
        teamId, "CONTRACT", savedContract.getId(), principal.getUserId(), savedContract);

    // Get primary contact for notification
    Contact primaryContact =
        contractPartyService.getPrimaryContactForContract(savedContract.getId(), teamId);

    String propertyName = property.getStreet() + ", " + property.getCity();
    String contactName = primaryContact.getDisplayName();
    Map<String, Object> contractVars = new HashMap<>();
    contractVars.put("propertyName", propertyName);
    contractVars.put("contactName", contactName);
    contractVars.put(
        "rentAmount",
        savedContract.getRentAmount().currency() + " " + savedContract.getRentAmount().value());
    contractVars.put("startDate", savedContract.getStartDate().toString());
    contractVars.put("endDate", savedContract.getEndDate().map(LocalDate::toString).orElse(""));
    contractVars.put("baseUrl", appProperties.email().baseUrl());
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_CREATED)
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
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    return toResponses(contracts, teamId);
  }

  public List<ContractResponse> getContractsByContact(
      ContactIdentifier contactIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    List<Contract> contracts =
        contractRepository.findByContactIdViaParties(contact.getId(), teamId);
    return toResponses(contracts, teamId);
  }

  public List<ContractResponse> getContractsByStatus(
      Contract.ContractStatus status, UserPrincipal principal) {
    List<Contract> contracts = contractRepository.findByStatus(status, principal.requireTeamId());
    return toResponses(contracts, principal.requireTeamId());
  }

  public ContractResponse getContract(ContractIdentifier identifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return toResponse(contract, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse updateContract(
      ContractIdentifier identifier, UpdateContractRequest request, UserPrincipal principal) {
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
        Contract.builder()
            .id(contract.getId())
            .identifier(contract.getIdentifier())
            .teamId(contract.getTeamId())
            .propertyId(contract.getPropertyId())
            .contractType(contract.getContractType())
            .startDate(contract.getStartDate())
            .endDate(contract.getEndDate())
            .signedDate(contract.getSignedDate())
            .rentAmount(contract.getRentAmount())
            .depositAmount(contract.getDepositAmount())
            .securityDeposit(contract.getSecurityDeposit())
            .paymentFrequency(contract.getPaymentFrequency())
            .paymentDueDay(contract.getPaymentDueDay())
            .terminationNoticeDays(contract.getTerminationNoticeDays())
            .lateFeePercentage(contract.getLateFeePercentage())
            .status(contract.getStatus())
            .termsAndConditions(contract.getTermsAndConditions())
            .notes(contract.getNotes())
            .countryCode(contract.getCountryCode())
            .countryMetadata(contract.getCountryMetadata())
            .renewalMode(contract.getRenewalMode())
            .renewalTermMonths(contract.getRenewalTermMonths())
            .maxRenewals(contract.getMaxRenewals())
            .landlordNoticeDays(contract.getLandlordNoticeDays())
            .tenantNoticeDays(contract.getTenantNoticeDays())
            .requiresTenantConfirmation(contract.getRequiresTenantConfirmation())
            .rentAdjustmentType(contract.getRentAdjustmentType())
            .rentAdjustmentValue(contract.getRentAdjustmentValue())
            .landlordType(contract.getLandlordType())
            .regionCode(contract.getRegionCode())
            .createdAt(contract.getCreatedAt())
            .updatedAt(contract.getUpdatedAt())
            .createdBy(contract.getCreatedBy())
            .updatedBy(contract.getUpdatedBy())
            .deletedAt(contract.getDeletedAt())
            .build();

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
      String countryCode = CountryMetadataRegistry.normalizeCountryCode(property.getCountryCode());
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
    validateCurrencyRequired(contract.getRentAmount().currency(), contract.getRentAmount().value());
    validateCurrencyRequired(
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::currency).orElse(null),
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::value).orElse(null));
    validateCurrencyRequired(
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::currency).orElse(null),
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::value).orElse(null));

    // Enforce team currency
    currencyEnforcement.validateCurrency(contract.getRentAmount().currency(), teamId);
    contract
        .getDepositAmount()
        .ifPresent(d -> currencyEnforcement.validateCurrency(d.currency(), teamId));
    contract
        .getSecurityDeposit()
        .ifPresent(d -> currencyEnforcement.validateCurrency(d.currency(), teamId));

    // Override rent amount if components provided
    request
        .rentComponents()
        .filter(list -> !list.isEmpty())
        .ifPresent(
            components -> {
              validateRentComponents(components);
              contract.setRentAmount(
                  computeRentFromComponents(components, contract.getRentAmount().currency()));
            });

    Contract updatedContract = contractRepository.save(contract);

    // Update initial rent period if rent or start date changed on DRAFT
    if (!oldContract.getRentAmount().equals(updatedContract.getRentAmount())
        || !oldContract.getStartDate().equals(updatedContract.getStartDate())) {
      contractRentPeriodService.updateInitialRentPeriod(updatedContract, principal);
    }

    // Handle rent components — link to current rent period
    request
        .rentComponents()
        .ifPresent(
            components -> {
              var currentPeriod =
                  contractRentPeriodService.getCurrentRent(updatedContract.getId(), teamId);
              currentPeriod.ifPresent(
                  period -> {
                    if (components.isEmpty()) {
                      rentComponentRepository.softDeleteByRentPeriodIdAndTeamId(
                          period.getId(), teamId);
                    } else {
                      saveRentComponents(updatedContract, period.getId(), components, principal);
                    }
                  });
            });

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
    if (!oldContract.getRentAmount().equals(updatedContract.getRentAmount())) {
      changedFields.put("rentAmount", updatedContract.getRentAmount().value());
      changedFields.put("rentAmountCurrency", updatedContract.getRentAmount().currency());
    }
    if (!oldContract.getDepositAmount().equals(updatedContract.getDepositAmount())) {
      changedFields.put(
          "depositAmount",
          updatedContract.getDepositAmount().map(com.buurman.util.MoneyAmount::value).orElse(null));
      changedFields.put(
          "depositAmountCurrency",
          updatedContract
              .getDepositAmount()
              .map(com.buurman.util.MoneyAmount::currency)
              .orElse(null));
    }
    if (!oldContract.getSecurityDeposit().equals(updatedContract.getSecurityDeposit())) {
      changedFields.put(
          "securityDeposit",
          updatedContract
              .getSecurityDeposit()
              .map(com.buurman.util.MoneyAmount::value)
              .orElse(null));
      changedFields.put(
          "securityDepositCurrency",
          updatedContract
              .getSecurityDeposit()
              .map(com.buurman.util.MoneyAmount::currency)
              .orElse(null));
    }
    if (!oldContract.getPaymentFrequency().equals(updatedContract.getPaymentFrequency())) {
      changedFields.put("paymentFrequency", updatedContract.getPaymentFrequency());
    }
    if (!oldContract.getPaymentDueDay().equals(updatedContract.getPaymentDueDay())) {
      changedFields.put("paymentDueDay", updatedContract.getPaymentDueDay().orElse(null));
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
  public void deleteContract(ContractIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Prevent deletion of ACTIVE contracts
    if (contract.getStatus() == ACTIVE) {
      throw new IllegalArgumentException(
          "Cannot delete ACTIVE contracts. Please terminate the contract first.");
    }

    contractPartyService.softDeletePartiesForContract(contract.getId(), teamId);
    rentComponentRepository.softDeleteByContractIdAndTeamId(contract.getId(), teamId);
    contractRepository.softDeleteByIdAndTeamId(contract.getId(), teamId);
    log.info("Contract soft deleted: {} in team {}", identifier, teamId);

    // Log to audit trail
    auditService.logDelete(teamId, "CONTRACT", contract.getId(), principal.getUserId(), contract);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse changeContractStatus(
      ContractIdentifier identifier, ChangeContractStatusRequest request, UserPrincipal principal) {
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
        Contract.builder()
            .id(contract.getId())
            .identifier(contract.getIdentifier())
            .teamId(contract.getTeamId())
            .propertyId(contract.getPropertyId())
            .contractType(contract.getContractType())
            .startDate(contract.getStartDate())
            .endDate(contract.getEndDate())
            .signedDate(contract.getSignedDate())
            .rentAmount(contract.getRentAmount())
            .depositAmount(contract.getDepositAmount())
            .securityDeposit(contract.getSecurityDeposit())
            .paymentFrequency(contract.getPaymentFrequency())
            .paymentDueDay(contract.getPaymentDueDay())
            .terminationNoticeDays(contract.getTerminationNoticeDays())
            .lateFeePercentage(contract.getLateFeePercentage())
            .status(contract.getStatus())
            .termsAndConditions(contract.getTermsAndConditions())
            .notes(contract.getNotes())
            .countryCode(contract.getCountryCode())
            .countryMetadata(contract.getCountryMetadata())
            .renewalMode(contract.getRenewalMode())
            .renewalTermMonths(contract.getRenewalTermMonths())
            .maxRenewals(contract.getMaxRenewals())
            .landlordNoticeDays(contract.getLandlordNoticeDays())
            .tenantNoticeDays(contract.getTenantNoticeDays())
            .requiresTenantConfirmation(contract.getRequiresTenantConfirmation())
            .rentAdjustmentType(contract.getRentAdjustmentType())
            .rentAdjustmentValue(contract.getRentAdjustmentValue())
            .landlordType(contract.getLandlordType())
            .regionCode(contract.getRegionCode())
            .createdAt(contract.getCreatedAt())
            .updatedAt(contract.getUpdatedAt())
            .createdBy(contract.getCreatedBy())
            .updatedBy(contract.getUpdatedBy())
            .deletedAt(contract.getDeletedAt())
            .build();

    contract.setStatus(newStatus);
    contract.setUpdatedBy(principal.getUserId());
    contract.setUpdatedAt(clock.instant());

    Contract updatedContract = contractRepository.save(contract);

    // E-06: Auto-cancel pending extensions when contract is terminated/expired
    if (newStatus == TERMINATED || newStatus == EXPIRED) {
      extensionRepository
          .findDraftByContractId(contract.getId(), teamId)
          .ifPresent(
              draft -> {
                extensionRepository.cancelByIdAndTeamId(
                    draft.getId(), teamId, principal.getUserId());
              });
    }

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
    Contact primaryContact =
        contractPartyService.getPrimaryContactForContract(contract.getId(), teamId);
    String scPropertyName =
        statusChangeProperty != null
            ? statusChangeProperty.getStreet() + ", " + statusChangeProperty.getCity()
            : identifier.value();
    String scContactName = primaryContact.getDisplayName();
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_STATUS_CHANGED)
            .templateName("contract-status-changed")
            .templateVariables(
                Map.of(
                    "propertyName", scPropertyName,
                    "contactName", scContactName,
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name(),
                    "baseUrl", appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return toResponse(updatedContract, teamId);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse reopenContract(ContractIdentifier identifier, UserPrincipal principal) {
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
        Contract.builder()
            .id(contract.getId())
            .identifier(contract.getIdentifier())
            .teamId(contract.getTeamId())
            .propertyId(contract.getPropertyId())
            .contractType(contract.getContractType())
            .startDate(contract.getStartDate())
            .endDate(contract.getEndDate())
            .signedDate(contract.getSignedDate())
            .rentAmount(contract.getRentAmount())
            .depositAmount(contract.getDepositAmount())
            .securityDeposit(contract.getSecurityDeposit())
            .paymentFrequency(contract.getPaymentFrequency())
            .paymentDueDay(contract.getPaymentDueDay())
            .terminationNoticeDays(contract.getTerminationNoticeDays())
            .lateFeePercentage(contract.getLateFeePercentage())
            .status(contract.getStatus())
            .termsAndConditions(contract.getTermsAndConditions())
            .notes(contract.getNotes())
            .countryCode(contract.getCountryCode())
            .countryMetadata(contract.getCountryMetadata())
            .renewalMode(contract.getRenewalMode())
            .renewalTermMonths(contract.getRenewalTermMonths())
            .maxRenewals(contract.getMaxRenewals())
            .landlordNoticeDays(contract.getLandlordNoticeDays())
            .tenantNoticeDays(contract.getTenantNoticeDays())
            .requiresTenantConfirmation(contract.getRequiresTenantConfirmation())
            .rentAdjustmentType(contract.getRentAdjustmentType())
            .rentAdjustmentValue(contract.getRentAdjustmentValue())
            .landlordType(contract.getLandlordType())
            .regionCode(contract.getRegionCode())
            .createdAt(contract.getCreatedAt())
            .updatedAt(contract.getUpdatedAt())
            .createdBy(contract.getCreatedBy())
            .updatedBy(contract.getUpdatedBy())
            .deletedAt(contract.getDeletedAt())
            .build();

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
    Contact reopenPrimaryContact =
        contractPartyService.getPrimaryContactForContract(contract.getId(), teamId);
    String reopenPropertyName =
        reopenProperty != null
            ? reopenProperty.getStreet() + ", " + reopenProperty.getCity()
            : identifier.value();
    String reopenContactName = reopenPrimaryContact.getDisplayName();
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_REOPENED)
            .templateName("contract-reopened")
            .templateVariables(
                Map.of(
                    "propertyName",
                    reopenPropertyName,
                    "contactName",
                    reopenContactName,
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
  public ContractResponse duplicateContract(
      ContractIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract sourceContract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Create new contract with same data
    Contract newContract =
        Contract.builder()
            .identifier(Optional.of(newContractId()))
            .teamId(teamId)
            .propertyId(sourceContract.getPropertyId())
            .contractType(sourceContract.getContractType())
            .startDate(sourceContract.getStartDate())
            .endDate(sourceContract.getEndDate())
            .signedDate(sourceContract.getSignedDate())
            .rentAmount(sourceContract.getRentAmount())
            .depositAmount(sourceContract.getDepositAmount())
            .securityDeposit(sourceContract.getSecurityDeposit())
            .paymentFrequency(sourceContract.getPaymentFrequency())
            .paymentDueDay(sourceContract.getPaymentDueDay())
            .terminationNoticeDays(sourceContract.getTerminationNoticeDays())
            .lateFeePercentage(sourceContract.getLateFeePercentage())
            .status(DRAFT)
            .termsAndConditions(sourceContract.getTermsAndConditions())
            .notes(sourceContract.getNotes())
            .countryCode(sourceContract.getCountryCode())
            .countryMetadata(sourceContract.getCountryMetadata())
            .createdAt(clock.instant())
            .updatedAt(clock.instant())
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();

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
      ContractIdentifier contractIdentifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file,
        "CONTRACT",
        contract.getId(),
        contract.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getDocuments(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return documentService.getDocuments("CONTRACT", contract.getId(), principal);
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
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(contractIdentifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "CONTRACT", contract.getId());
  }

  public Map<String, Object> generatePayments(
      ContractIdentifier contractIdentifier,
      GeneratePaymentsRequest request,
      UserPrincipal principal) {
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

    Optional<ContactSummary> primaryContact =
        partyResponses.stream()
            .filter(p -> p.role() == ContractPartyRole.PRIMARY_TENANT)
            .map(ContractPartyResponse::contact)
            .flatMap(Optional::stream)
            .findFirst();

    PropertySummary propertySummary = propertyMapper.toSummary(property);

    // Compute effective end date and extension statistics
    List<ContractExtension> extensions =
        contractExtensionService.getExtensionsForContract(contract.getId(), teamId);
    Optional<LocalDate> effectiveEndDate =
        EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);
    int extensionCount = contractExtensionService.getExtensionCount(contract.getId(), teamId);
    Optional<Integer> extensionsRemaining =
        contract.getMaxRenewals().map(max -> max - extensionCount);

    List<RentComponentResponse> componentResponses =
        rentComponentMapper.toResponses(
            contractRentPeriodService.getCurrentRentComponents(contract.getId(), teamId));

    return new ContractResponse(
        contract.getIdentifier().orElseThrow(),
        Optional.of(propertySummary),
        partyResponses,
        primaryContact,
        contract.getContractType(),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getSignedDate(),
        contract.getRentAmount().value(),
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::value),
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::value),
        contract.getRentAmount().currency(),
        contract.getDepositAmount().map(com.buurman.util.MoneyAmount::currency),
        contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::currency),
        contract.getPaymentFrequency(),
        contract.getPaymentDueDay(),
        contract.getTerminationNoticeDays(),
        contract.getLateFeePercentage(),
        contract.getStatus(),
        contract.getTermsAndConditions(),
        contract.getNotes(),
        contract.getCountryCode(),
        contract.getCountryMetadata(),
        contract.getRenewalMode(),
        contract.getRenewalTermMonths(),
        contract.getMaxRenewals(),
        contract.getLandlordNoticeDays(),
        contract.getTenantNoticeDays(),
        contract.getRequiresTenantConfirmation(),
        contract.getRentAdjustmentType(),
        contract.getRentAdjustmentValue(),
        contract.getLandlordType(),
        contract.getRegionCode(),
        contract.getDocumentLanguages(),
        effectiveEndDate,
        extensionCount,
        extensionsRemaining,
        componentResponses,
        contract.getCreatedAt(),
        Optional.of(contract.getUpdatedAt()));
  }

  /** Batch build responses for a list of contracts (avoids N+1 for parties and contacts). */
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

    // Batch load contacts for all parties
    List<UUID> allContactIds =
        partiesByContract.values().stream()
            .flatMap(List::stream)
            .flatMap(p -> p.getContactId().stream())
            .distinct()
            .toList();
    Map<UUID, Contact> contactMap =
        contactRepository.findByIdsAndTeamId(allContactIds, teamId).stream()
            .collect(Collectors.toMap(Contact::getId, c -> c));

    // Batch load extensions for all contracts
    List<ContractExtension> allExtensions =
        extensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    // Batch load current rent period components
    Map<UUID, List<ContractRentComponent>> componentsByContract =
        contractRentPeriodService.getCurrentRentComponentsBatch(contractIds, teamId);

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
                            Contact contact =
                                party.getContactId().map(contactMap::get).orElse(null);
                            Optional<ContactSummary> summary =
                                Optional.ofNullable(contact).map(contactMapper::toSummary);
                            return new ContractPartyResponse(
                                party.getIdentifier().orElseThrow(), summary, party.getRole());
                          })
                      .toList();

              Optional<ContactSummary> primaryContact =
                  partyResponses.stream()
                      .filter(p -> p.role() == ContractPartyRole.PRIMARY_TENANT)
                      .map(ContractPartyResponse::contact)
                      .flatMap(Optional::stream)
                      .findFirst();

              // Compute effective end date and extension statistics
              List<ContractExtension> extensions =
                  extensionsByContract.getOrDefault(contract.getId(), List.of());
              Optional<LocalDate> effectiveEndDate =
                  EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);
              int extensionCount =
                  (int)
                      extensions.stream()
                          .filter(
                              e ->
                                  e.getStatus() == ContractExtension.ExtensionStatus.ACTIVE
                                      || e.getStatus()
                                          == ContractExtension.ExtensionStatus.SUPERSEDED)
                          .filter(e -> e.getDeletedAt().isEmpty())
                          .count();
              Optional<Integer> extensionsRemaining =
                  contract.getMaxRenewals().map(max -> max - extensionCount);

              List<RentComponentResponse> componentResponses =
                  rentComponentMapper.toResponses(
                      componentsByContract.getOrDefault(contract.getId(), List.of()));

              return new ContractResponse(
                  contract.getIdentifier().orElseThrow(),
                  propertySummary,
                  partyResponses,
                  primaryContact,
                  contract.getContractType(),
                  contract.getStartDate(),
                  contract.getEndDate(),
                  contract.getSignedDate(),
                  contract.getRentAmount().value(),
                  contract.getDepositAmount().map(com.buurman.util.MoneyAmount::value),
                  contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::value),
                  contract.getRentAmount().currency(),
                  contract.getDepositAmount().map(com.buurman.util.MoneyAmount::currency),
                  contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::currency),
                  contract.getPaymentFrequency(),
                  contract.getPaymentDueDay(),
                  contract.getTerminationNoticeDays(),
                  contract.getLateFeePercentage(),
                  contract.getStatus(),
                  contract.getTermsAndConditions(),
                  contract.getNotes(),
                  contract.getCountryCode(),
                  contract.getCountryMetadata(),
                  contract.getRenewalMode(),
                  contract.getRenewalTermMonths(),
                  contract.getMaxRenewals(),
                  contract.getLandlordNoticeDays(),
                  contract.getTenantNoticeDays(),
                  contract.getRequiresTenantConfirmation(),
                  contract.getRentAdjustmentType(),
                  contract.getRentAdjustmentValue(),
                  contract.getLandlordType(),
                  contract.getRegionCode(),
                  contract.getDocumentLanguages(),
                  effectiveEndDate,
                  extensionCount,
                  extensionsRemaining,
                  componentResponses,
                  contract.getCreatedAt(),
                  Optional.of(contract.getUpdatedAt()));
            })
        .toList();
  }

  private void saveRentComponents(
      Contract contract,
      UUID rentPeriodId,
      List<RentComponentRequest> components,
      UserPrincipal principal) {
    String currency = contract.getRentAmount().currency();
    UUID teamId = contract.getTeamId();
    UUID userId = principal.getUserId();

    List<ContractRentComponent> domainComponents = new ArrayList<>();
    for (int i = 0; i < components.size(); i++) {
      RentComponentRequest req = components.get(i);
      ContractRentComponent comp =
          ContractRentComponent.builder()
              .identifier(Optional.of(newRentComponentId()))
              .teamId(teamId)
              .contractId(contract.getId())
              .rentPeriodId(rentPeriodId)
              .componentType(req.componentType())
              .amount(com.buurman.util.MoneyAmount.of(req.amount(), currency))
              .description(req.description())
              .sortOrder(i)
              .createdAt(clock.instant())
              .updatedAt(clock.instant())
              .createdBy(userId)
              .updatedBy(userId)
              .build();
      domainComponents.add(comp);
    }

    rentComponentRepository.replaceForRentPeriod(
        rentPeriodId, contract.getId(), teamId, domainComponents);
  }

  private void validateRentComponents(List<RentComponentRequest> components) {
    boolean hasBaseRent =
        components.stream().anyMatch(c -> c.componentType() == RentComponentType.BASE_RENT);
    if (!hasBaseRent) {
      throw new BadRequestException("Rent components must include BASE_RENT");
    }

    // Check for duplicate non-OTHER types
    Map<RentComponentType, Long> typeCounts =
        components.stream()
            .filter(c -> c.componentType() != RentComponentType.OTHER)
            .collect(
                Collectors.groupingBy(RentComponentRequest::componentType, Collectors.counting()));

    typeCounts.forEach(
        (type, count) -> {
          if (count > 1) {
            throw new BadRequestException("Duplicate rent component type: " + type);
          }
        });

    // Validate OTHER components have descriptions
    components.stream()
        .filter(c -> c.componentType() == RentComponentType.OTHER)
        .forEach(
            c -> {
              if (c.description().isEmpty() || c.description().get().isBlank()) {
                throw new BadRequestException("OTHER rent components must have a description");
              }
            });
  }

  private com.buurman.util.MoneyAmount computeRentFromComponents(
      List<RentComponentRequest> components, String currency) {
    BigDecimal total =
        components.stream()
            .map(RentComponentRequest::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return com.buurman.util.MoneyAmount.of(total, currency);
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
