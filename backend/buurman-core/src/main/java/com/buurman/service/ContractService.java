package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.DRAFT;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.NOTICE_GIVEN;
import static com.buurman.domain.Contract.ContractStatus.PENDING_SIGNATURE;
import static com.buurman.domain.Contract.ContractStatus.TERMINATED;
import static com.buurman.domain.Contract.ContractType.FIXED_TERM;
import static com.buurman.domain.NotificationType.CONTRACT_CREATED;
import static com.buurman.domain.NotificationType.CONTRACT_REOPENED;
import static com.buurman.domain.NotificationType.CONTRACT_STATUS_CHANGED;
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
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
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
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.DocumentLanguages;
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
  private final UnitRepository unitRepository;
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
  private final RentRegulationRepository rentRegulationRepository;
  private final AppProperties appProperties;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractResponse createContract(CreateContractRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    // Resolve property by identifier
    Property property =
        propertyRepository.getByIdentifierAndTeamId(request.propertyIdentifier(), teamId);

    UUID unitId = resolveUnitId(property, request.unitIdentifier(), principal);

    // Check no active contract exists for the unit
    assertUnitHasNoActiveContract(unitId, teamId, null);

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
    contract.setUnitId(unitId);
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
    validateLateFee(contract);
    validateDocumentLanguages(contract);

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
    String createdBase = appProperties.email().baseUrl();
    contractVars.put("baseUrl", createdBase);
    contractVars.put(
        "primaryUrl",
        createdBase + "/contracts/" + savedContract.getIdentifier().map(s -> s.value()).orElse(""));
    contractVars.put(
        "secondaryUrl",
        createdBase + "/properties/" + property.getIdentifier().map(s -> s.value()).orElse(""));
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_CREATED)
            .relatedContractId(Optional.of(savedContract.getId()))
            .templateName("contract-created")
            .templateVariables(contractVars)
            .createdBy(principal.getUserId())
            .build());

    return toResponse(savedContract, property, teamId);
  }

  /**
   * Resolves which unit of {@code property} a new contract is for. An explicit {@code
   * unitIdentifier} must belong to this property (never a silent cross-property contract) and to
   * this team ({@link UnitRepository#getByIdentifierAndTeamId} throws {@link
   * com.buurman.exception.NotFoundException} for a wrong-team lookup, which maps to 404 without
   * leaking existence). Omitting it only works when the property has exactly one unit.
   */
  private UUID resolveUnitId(
      Property property, @Nullable String unitIdentifier, UserPrincipal principal) {
    if (unitIdentifier != null) {
      Unit unit =
          unitRepository.getByIdentifierAndTeamId(
              Sid.of(unitIdentifier), principal.requireTeamId());
      if (!unit.getPropertyId().equals(property.getId())) {
        throw new BadRequestException("The chosen unit does not belong to this property.");
      }
      return unit.getId();
    }
    List<Unit> units =
        unitRepository.findAllByPropertyIdAndTeamId(property.getId(), principal.requireTeamId());
    if (units.size() != 1) {
      throw new BadRequestException(
          "Property "
              + property.getStreet()
              + " has "
              + units.size()
              + " units. Specify which unit the contract is for.");
    }
    return units.get(0).getId();
  }

  /**
   * Guards against two in-force contracts ({@code ACTIVE} or {@code NOTICE_GIVEN}, via {@link
   * ContractRepository#findActiveByUnitId}) on the same unit: a tenant under notice still occupies
   * it until the termination's effective end date. Scoped by <em>unit</em>, not property — BUUR-106
   * made it valid for two different units of the same property to each have their own active
   * contract, so this must never widen back out to property scope (that was Critical 1 of the final
   * review: the old property-scoped guard made a second unit of an already-let building
   * un-lettable, and its {@code fetchOptional()} would 500 once two units really could both be
   * active). {@code excludeContractId} lets {@link #changeContractStatus} re-activate a contract
   * that is itself the one found "active" (e.g. a no-op transition) without rejecting itself;
   * {@link #createContract} passes {@code null} since a brand-new contract can never be the one
   * found.
   */
  private void assertUnitHasNoActiveContract(
      UUID unitId, UUID teamId, @Nullable UUID excludeContractId) {
    contractRepository
        .findActiveByUnitId(unitId, teamId)
        .ifPresent(
            existing -> {
              if (excludeContractId == null || !existing.getId().equals(excludeContractId)) {
                throw new IllegalArgumentException(
                    "Unit already has an active contract. Please terminate the existing contract"
                        + " first.");
              }
            });
  }

  public List<ContractResponse> getAllContracts(UserPrincipal principal) {
    List<Contract> contracts = contractRepository.findAllByTeamId(principal.requireTeamId());
    return toResponses(contracts, principal.requireTeamId());
  }

  /** Beyond this, any realistic lease filter is already covered; larger values risk overflow. */
  private static final int MAX_ENDING_WITHIN_DAYS = 3650;

  public PageResponse<ContractResponse> getContractsPaginated(
      UserPrincipal principal,
      @Nullable String status,
      @Nullable String search,
      @Nullable Integer endingWithinDays,
      PageRequest pageRequest) {
    if (endingWithinDays != null
        && (endingWithinDays < 0 || endingWithinDays > MAX_ENDING_WITHIN_DAYS)) {
      throw new BadRequestException(
          "endingWithinDays must be between 0 and " + MAX_ENDING_WITHIN_DAYS);
    }
    PaginatedResult<Contract> result =
        contractRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(), status, null, null, search, endingWithinDays, pageRequest);
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

    // Prevent updates to ACTIVE, NOTICE_GIVEN, TERMINATED, or EXPIRED contracts (except via status
    // change). NOTICE_GIVEN is locked like ACTIVE: its termination record and notice letter were
    // computed from the contract's current terms.
    if (contract.getStatus() == ACTIVE) {
      throw new IllegalArgumentException(
          "Cannot update ACTIVE contracts. Please change status first.");
    }
    if (contract.getStatus() == NOTICE_GIVEN) {
      throw new IllegalArgumentException(
          "Cannot update NOTICE_GIVEN contracts. Notice has already been given on this contract.");
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
            .unitId(contract.getUnitId())
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
            .tenantRemindersEnabled(contract.getTenantRemindersEnabled())
            .remindersPausedUntil(contract.getRemindersPausedUntil())
            .lateFeeEnabled(contract.getLateFeeEnabled())
            .lateFeeGraceDays(contract.getLateFeeGraceDays())
            .formalNoticeDays(contract.getFormalNoticeDays())
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
    validateLateFee(contract);
    validateDocumentLanguages(contract);

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
    // A contract under notice has a contract_terminations row the daily sweep will still process;
    // deleting the contract would orphan it.
    if (contract.getStatus() == NOTICE_GIVEN) {
      throw new IllegalArgumentException(
          "Cannot delete NOTICE_GIVEN contracts. Notice has already been given on this contract.");
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

    // Giving notice is not a bare status flip: it needs a termination record (notice period,
    // grounds, effective end date), a notice letter and a deposit deadline, all created by
    // ContractTerminationService.terminate(), which calls transitionStatus directly.
    if (request.status() == NOTICE_GIVEN) {
      throw new BadRequestException(
          "Use POST /contracts/{identifier}/terminate to give notice on a contract");
    }

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    Contract updatedContract =
        transitionStatus(contract, request.status(), principal.getUserId(), request.reason());

    return toResponse(updatedContract, teamId);
  }

  /**
   * The actual status-transition logic behind {@link #changeContractStatus}: validation,
   * persistence, extension auto-cancel, metrics, payment scheduling, unit-status sync, audit log,
   * and the status-changed notification. Extracted so the system-triggered contract termination
   * sweep ({@code ContractTerminationService.sweepDueTerminations()}) can drive the same transition
   * — it runs with no authenticated {@link UserPrincipal} (see {@code
   * ContractTerminationRepository#findDueForTransition}'s javadoc), so it cannot satisfy {@link
   * #changeContractStatus}'s {@code @PreAuthorize} gate or its {@code UserPrincipal} parameter.
   *
   * <p>Package-private and unguarded: this method performs no authorization check of its own.
   * Callers outside this package must go through the role-gated {@link #changeContractStatus}
   * instead of calling this directly. The one exception to that is {@code NOTICE_GIVEN}, which
   * {@link #changeContractStatus} refuses: {@code ContractTerminationService.terminate()} (itself
   * role-gated) is the only path to it and calls this method directly.
   */
  Contract transitionStatus(
      Contract contract,
      Contract.ContractStatus newStatus,
      UUID actingUserId,
      Optional<String> reason) {
    UUID teamId = contract.getTeamId();
    UUID contractId = contract.getId();
    Contract.ContractStatus oldStatus = contract.getStatus();
    Sid contractIdentifier = contract.getIdentifier().orElseThrow();

    // Validate status transitions
    validateStatusTransition(oldStatus, newStatus);

    // If changing to ACTIVE, ensure no other active contract on the unit
    if (newStatus == ACTIVE) {
      assertUnitHasNoActiveContract(contract.getUnitId(), teamId, contractId);
    }

    // Store old values for audit
    Contract oldContract =
        Contract.builder()
            .id(contract.getId())
            .identifier(contract.getIdentifier())
            .teamId(contract.getTeamId())
            .propertyId(contract.getPropertyId())
            .unitId(contract.getUnitId())
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
            .tenantRemindersEnabled(contract.getTenantRemindersEnabled())
            .remindersPausedUntil(contract.getRemindersPausedUntil())
            .lateFeeEnabled(contract.getLateFeeEnabled())
            .lateFeeGraceDays(contract.getLateFeeGraceDays())
            .formalNoticeDays(contract.getFormalNoticeDays())
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
    contract.setUpdatedBy(actingUserId);
    contract.setUpdatedAt(clock.instant());

    Contract updatedContract = contractRepository.save(contract);

    // E-06: Auto-cancel pending extensions when contract is terminated/expired
    if (newStatus == TERMINATED || newStatus == EXPIRED) {
      extensionRepository
          .findDraftByContractId(contract.getId(), teamId)
          .ifPresent(
              draft -> {
                extensionRepository.cancelByIdAndTeamId(draft.getId(), teamId, actingUserId);
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
        contractIdentifier,
        oldStatus,
        newStatus,
        teamId);

    // Trigger payment scheduling on status change
    paymentSchedulingService.handleContractStatusChange(
        contractId, newStatus, teamId, actingUserId);

    // Update the unit's status based on the contract's status
    updateUnitStatusBasedOnContract(
        contract.getUnitId(), teamId, newStatus, oldStatus, actingUserId);

    // Log to audit trail
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("status", newStatus);
    reason.ifPresent(r -> changedFields.put("statusChangeReason", r));

    auditService.logUpdate(
        teamId,
        "CONTRACT",
        updatedContract.getId(),
        actingUserId,
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
            : contractIdentifier.value();
    String scContactName = primaryContact.getDisplayName();
    String scBase = appProperties.email().baseUrl();
    String scPropertySid =
        statusChangeProperty != null
            ? statusChangeProperty.getIdentifier().map(s -> s.value()).orElse("")
            : "";
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_STATUS_CHANGED)
            .relatedContractId(Optional.of(contract.getId()))
            .templateName("contract-status-changed")
            .templateVariables(
                Map.of(
                    "propertyName",
                    scPropertyName,
                    "contactName",
                    scContactName,
                    "oldStatus",
                    oldStatus.name(),
                    "newStatus",
                    newStatus.name(),
                    "baseUrl",
                    scBase,
                    "primaryUrl",
                    scBase + "/contracts/" + contractIdentifier.value(),
                    "secondaryUrl",
                    scBase + "/properties/" + scPropertySid))
            .createdBy(actingUserId)
            .build());

    return updatedContract;
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
            .unitId(contract.getUnitId())
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
            .tenantRemindersEnabled(contract.getTenantRemindersEnabled())
            .remindersPausedUntil(contract.getRemindersPausedUntil())
            .lateFeeEnabled(contract.getLateFeeEnabled())
            .lateFeeGraceDays(contract.getLateFeeGraceDays())
            .formalNoticeDays(contract.getFormalNoticeDays())
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
    String reopenBase = appProperties.email().baseUrl();
    String reopenPropertySid =
        reopenProperty != null ? reopenProperty.getIdentifier().map(s -> s.value()).orElse("") : "";
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(CONTRACT_REOPENED)
            .relatedContractId(Optional.of(contract.getId()))
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
                    reopenBase,
                    "primaryUrl",
                    reopenBase + "/contracts/" + identifier.value(),
                    "secondaryUrl",
                    reopenBase + "/properties/" + reopenPropertySid))
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
            .unitId(sourceContract.getUnitId())
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
          case ACTIVE -> to == TERMINATED || to == EXPIRED || to == NOTICE_GIVEN;
          case NOTICE_GIVEN -> to == TERMINATED;
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

    Unit unit = unitRepository.getByIdAndTeamId(contract.getUnitId(), teamId);
    String unitIdentifier = unit.getIdentifier().orElseThrow().value();
    String unitNumber = unit.getUnitNumber();

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
        unitIdentifier,
        unitNumber,
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
        Optional.of(contract.getUpdatedAt()),
        contract.getTenantRemindersEnabled(),
        contract.getRemindersPausedUntil(),
        contract.getLateFeeEnabled(),
        contract.getLateFeeGraceDays(),
        contract.getFormalNoticeDays());
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

    // Batch load units
    List<UUID> unitIds = contracts.stream().map(Contract::getUnitId).distinct().toList();
    Map<UUID, Unit> unitMap =
        unitRepository.findByIdsAndTeamId(unitIds, teamId).stream()
            .collect(Collectors.toMap(Unit::getId, u -> u));

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

              Optional<Unit> unit = Optional.ofNullable(unitMap.get(contract.getUnitId()));
              String unitIdentifier =
                  unit.map(u -> u.getIdentifier().orElseThrow().value()).orElse("");
              String unitNumber = unit.map(Unit::getUnitNumber).orElse("");

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
                  unitIdentifier,
                  unitNumber,
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
                  Optional.of(contract.getUpdatedAt()),
                  contract.getTenantRemindersEnabled(),
                  contract.getRemindersPausedUntil(),
                  contract.getLateFeeEnabled(),
                  contract.getLateFeeGraceDays(),
                  contract.getFormalNoticeDays());
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

  private static void validateDocumentLanguages(Contract contract) {
    List<String> unsupported =
        contract.getDocumentLanguages().stream()
            .filter(lang -> !DocumentLanguages.isSupported(lang))
            .toList();
    if (!unsupported.isEmpty()) {
      throw new BadRequestException(
          "Unsupported document language(s): "
              + String.join(", ", unsupported)
              + ". Supported: "
              + String.join(", ", DocumentLanguages.SUPPORTED.stream().sorted().toList()));
    }
  }

  private void validateLateFee(Contract contract) {
    LateFeeService.validateAgainstRegulation(
        contract, contract.getCountryCode().flatMap(rentRegulationRepository::findCountryByCode));
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

  /**
   * Reinstates the business rule that was deleted when {@code properties.status} was dropped in
   * V070, retargeted from the property to its unit:
   *
   * <ul>
   *   <li>{@code newStatus == ACTIVE && oldStatus != ACTIVE}: the unit becomes {@code OCCUPIED}.
   *   <li>{@code oldStatus} in force ({@code ACTIVE} or {@code NOTICE_GIVEN}) {@code && (newStatus
   *       == EXPIRED || newStatus == TERMINATED)}: the unit becomes {@code VACANT}, but ONLY when
   *       no OTHER in-force contract still references it. The termination sweep moves a contract
   *       {@code NOTICE_GIVEN -> TERMINATED}, so {@code NOTICE_GIVEN} must count here. A unit can
   *       carry two overlapping tenancies (legacy data predating the V072 index), and must stay
   *       {@code OCCUPIED} while any of them is still in force.
   * </ul>
   *
   * <p>By the time this runs, the caller has already persisted {@code newStatus} on {@code
   * contract} ({@link #transitionStatus}), so {@link ContractRepository#countActiveByUnitId}
   * naturally excludes the contract that just expired or was terminated — no explicit
   * self-exclusion is needed.
   *
   * <p>Takes a team id and an acting user id rather than a {@link UserPrincipal} because {@link
   * #transitionStatus} — whose callers include the system-triggered contract termination sweep,
   * which has no authenticated {@link UserPrincipal} — is the sole caller.
   */
  private void updateUnitStatusBasedOnContract(
      UUID unitId,
      UUID teamId,
      Contract.ContractStatus newStatus,
      Contract.ContractStatus oldStatus,
      UUID actingUserId) {
    if (newStatus == ACTIVE && oldStatus != ACTIVE) {
      setUnitStatus(unitId, teamId, UnitStatus.OCCUPIED, actingUserId);
      return;
    }
    if (oldStatus.isInForce() && (newStatus == EXPIRED || newStatus == TERMINATED)) {
      if (contractRepository.countActiveByUnitId(unitId, teamId) == 0) {
        setUnitStatus(unitId, teamId, UnitStatus.VACANT, actingUserId);
      }
    }
  }

  private void setUnitStatus(UUID unitId, UUID teamId, UnitStatus status, UUID actingUserId) {
    Unit unit = unitRepository.getByIdAndTeamId(unitId, teamId);
    unit.setStatus(status);
    unit.setUpdatedBy(Optional.of(actingUserId));
    unitRepository.save(unit);
  }
}
