package com.buurman.service;

import static com.buurman.domain.ContractExtension.ExtensionStatus.ACTIVE;
import static com.buurman.domain.ContractExtension.ExtensionStatus.DECLINED;
import static com.buurman.domain.ContractExtension.ExtensionStatus.DRAFT;
import static com.buurman.domain.ContractExtension.ExtensionStatus.SUPERSEDED;
import static com.buurman.domain.NotificationType.CONTRACT_EXTENDED;
import static com.buurman.domain.NotificationType.CONTRACT_EXTENSION_PENDING;
import static com.buurman.domain.NotificationType.CONTRACT_RENEWAL_REMINDER;
import static com.buurman.domain.NotificationType.CONTRACT_ROLLED_OVER_TO_INDEFINITE;
import static com.buurman.util.Constants.SYSTEM_USER_ID;
import static com.buurman.util.SidGenerator.newContractExtensionId;
import static com.buurman.util.SidGenerator.newContractRentPeriodId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractExtension.RentAdjustmentType;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.CreateContractExtensionRequest;
import com.buurman.dto.request.DeclineContractExtensionRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.ContractExtensionResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.domain.Document;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.MoneyAmount;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractExtensionService {

  private static final String ENTITY_TYPE_CONTRACT = "CONTRACT";

  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final DocumentRepository documentRepository;
  private final PropertyRepository propertyRepository;
  private final TeamRepository teamRepository;
  private final NotificationService notificationService;
  private final S3StorageService s3StorageService;
  private final AuditService auditService;
  private final TransactionTemplate transactionTemplate;
  private final AppProperties appProperties;
  private final Clock clock;

  // ── CRUD Operations ──────────────────────────────────────────────────

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractExtensionResponse createExtension(
      ContractIdentifier contractIdentifier,
      CreateContractExtensionRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    // BR-12: Extensions only for ACTIVE contracts
    if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
      throw new BusinessRuleException("Extensions can only be created for active contracts");
    }

    // BR-13: Extensions only for FIXED_TERM contracts
    if (contract.getContractType() != Contract.ContractType.FIXED_TERM) {
      throw new BusinessRuleException("Extensions can only be created for fixed-term contracts");
    }

    // BR-11: At most one DRAFT or ACTIVE extension per contract
    extensionRepository
        .findDraftByContractId(contract.getId(), teamId)
        .ifPresent(
            existing -> {
              throw new BusinessRuleException("Contract already has a pending draft extension");
            });

    extensionRepository
        .findActiveByContractId(contract.getId(), teamId)
        .ifPresent(
            existing -> {
              throw new BusinessRuleException("Contract already has an active extension");
            });

    // BR-14: Max renewals check
    if (contract.getMaxRenewals().isPresent()) {
      int used = extensionRepository.countActiveAndSuperseded(contract.getId(), teamId);
      if (used >= contract.getMaxRenewals().get()) {
        throw new BusinessRuleException("Maximum number of renewals reached");
      }
    }

    // Compute previous values
    List<ContractExtension> existingExtensions =
        extensionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Optional<LocalDate> effectiveEndDate =
        EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), existingExtensions);
    LocalDate previousEndDate =
        effectiveEndDate.orElseThrow(
            () -> new BusinessRuleException("Cannot extend indefinite contract"));
    MoneyAmount previousRent = getCurrentRent(contract, existingExtensions);

    // Determine rent adjustment
    RentAdjustmentType adjType =
        request.rentAdjustmentType().orElse(contract.getRentAdjustmentType());
    Optional<BigDecimal> adjValue =
        request.rentAdjustmentValue().or(contract::getRentAdjustmentValue);

    MoneyAmount newRent;
    if (request.newRentAmount().isPresent()) {
      // BR-22: MANUAL — user provides new rent directly
      newRent = MoneyAmount.of(request.newRentAmount().get(), previousRent.currency());
      adjType = RentAdjustmentType.MANUAL;
    } else {
      newRent = computeNewRent(previousRent, adjType, adjValue);
    }

    // Compute new end date
    LocalDate newEndDate;
    if (request.newEndDate().isPresent()) {
      newEndDate = request.newEndDate().get();
      // BR-08: new_end_date > previous_end_date
      if (!newEndDate.isAfter(previousEndDate)) {
        throw new BadRequestException("New end date must be after the current end date");
      }
    } else {
      Integer termMonths =
          contract
              .getRenewalTermMonths()
              .orElseThrow(
                  () ->
                      new BadRequestException(
                          "Renewal term months required when no end date provided"));
      newEndDate = previousEndDate.plusMonths(termMonths);
    }

    int extensionNumber = extensionRepository.getNextExtensionNumber(contract.getId(), teamId);

    ContractExtension extension =
        ContractExtension.builder()
            .identifier(Optional.of(newContractExtensionId()))
            .teamId(teamId)
            .contractId(contract.getId())
            .extensionNumber(extensionNumber)
            .previousEndDate(previousEndDate)
            .newEndDate(Optional.of(newEndDate))
            .previousRentAmount(previousRent)
            .newRentAmount(newRent)
            .rentAdjustmentType(adjType)
            .rentAdjustmentValue(adjValue)
            .status(DRAFT)
            .triggerType(ContractExtension.TriggerType.MANUAL)
            .notes(request.notes())
            .createdBy(userId)
            .updatedBy(userId)
            .build();

    try {
      extension = extensionRepository.save(extension);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessRuleException("Contract already has a pending or active extension");
    }

    auditService.logCreate(teamId, "CONTRACT_EXTENSION", extension.getId(), userId, extension);

    return toResponse(extension, contract.getIdentifier().orElseThrow());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PageResponse<ContractExtensionResponse> listExtensions(
      ContractIdentifier contractIdentifier, PageRequest pageRequest, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Sid contractSid = contract.getIdentifier().orElseThrow();

    PaginatedResult<ContractExtension> result =
        extensionRepository.findByContractIdPaginated(contract.getId(), teamId, pageRequest);

    List<ContractExtensionResponse> content =
        result.items().stream().map(e -> toResponse(e, contractSid)).toList();

    return PageResponse.of(content, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public ContractExtensionResponse getExtension(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);

    validateExtensionBelongsToContract(extension, contract);

    return toResponse(extension, contract.getIdentifier().orElseThrow());
  }

  // ── State Transitions ────────────────────────────────────────────────

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractExtensionResponse activateExtension(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);

    validateExtensionBelongsToContract(extension, contract);
    validateStatus(extension, DRAFT, "activate");

    // Check contact confirmation if required
    if (contract.getRequiresTenantConfirmation() && extension.getConfirmedAt().isEmpty()) {
      throw new BusinessRuleException("Contact confirmation required before activation");
    }

    return doActivate(extension, contract, teamId, userId);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractExtensionResponse confirmExtension(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);

    validateExtensionBelongsToContract(extension, contract);
    validateStatus(extension, DRAFT, "confirm");

    Instant now = Instant.now(clock);
    extension.setConfirmedAt(Optional.of(now));
    extension.setConfirmedBy(Optional.of(userId));
    extension.setUpdatedBy(userId);
    extension = extensionRepository.save(extension);

    return toResponse(extension, contract.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractExtensionResponse declineExtension(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      DeclineContractExtensionRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);

    validateExtensionBelongsToContract(extension, contract);
    validateStatus(extension, DRAFT, "decline");

    extension.setStatus(DECLINED);
    extension.setDeclinedReason(request.reason());
    extension.setUpdatedBy(userId);
    extension = extensionRepository.save(extension);

    auditService.logUpdate(
        teamId,
        "CONTRACT_EXTENSION",
        extension.getId(),
        userId,
        null,
        extension,
        Map.of("status", DECLINED.name()));

    return toResponse(extension, contract.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void cancelExtension(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      boolean deleteDocuments,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);

    validateExtensionBelongsToContract(extension, contract);
    validateStatus(extension, DRAFT, "cancel");

    if (deleteDocuments) {
      deleteExtensionDocuments(contract, extension, teamId);
    }

    extensionRepository.cancelByIdAndTeamId(extension.getId(), teamId, userId);
    auditService.logDelete(teamId, "CONTRACT_EXTENSION", extension.getId(), userId, extension);
  }

  private void deleteExtensionDocuments(
      Contract contract, ContractExtension extension, UUID teamId) {
    String filenamePattern = "%-" + extension.getExtensionNumber() + "-%.pdf";
    List<Document> documents = documentRepository.findByEntityAndFileNamePatternAndTeamId(
        ENTITY_TYPE_CONTRACT, contract.getId(), filenamePattern, teamId);

    for (Document doc : documents) {
      documentRepository.softDeleteByIdAndTeamId(doc.getId(), teamId);
      s3StorageService.deleteFile(doc.getFileKey());
      log.info("Deleted extension document: {}", doc.getFileName());
    }

    if (!documents.isEmpty()) {
      log.info(
          "Deleted {} documents for extension #{} on contract {}",
          documents.size(),
          extension.getExtensionNumber(),
          contract.getIdentifier().orElseThrow().value());
    }
  }

  // ── Auto-Extension Job ───────────────────────────────────────────────

  public void processAutoExtensions() {
    log.info("Starting auto-extension processing");

    List<UUID> teamIds = teamRepository.findAllActiveTeamIds();
    int totalCreated = 0;

    for (UUID teamId : teamIds) {
      try {
        totalCreated += processAutoExtensionsForTeam(teamId);
      } catch (Exception e) {
        log.error("Auto-extension failed for team {}", teamId, e);
      }
    }

    log.info(
        "Auto-extension completed. Created {} extensions across {} teams",
        totalCreated,
        teamIds.size());
  }

  int processAutoExtensionsForTeam(UUID teamId) {
    LocalDate today = LocalDate.now(clock);

    // Batch-load all extensions for candidates (done outside transaction — read-only data for
    // computation)
    List<Contract> allFiltered =
        contractRepository.findActiveByTeamId(teamId).stream()
            .filter(c -> c.getRenewalMode() == Contract.RenewalMode.AUTOMATIC)
            .filter(c -> c.getContractType() == Contract.ContractType.FIXED_TERM)
            .toList();

    if (allFiltered.isEmpty()) {
      return 0;
    }

    List<UUID> contractIds = allFiltered.stream().map(Contract::getId).toList();
    Map<UUID, List<ContractExtension>> extensionsByContract =
        extensionRepository.findByContractIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.groupingBy(ContractExtension::getContractId));

    List<Contract> candidates =
        allFiltered.stream()
            .filter(
                c -> {
                  List<ContractExtension> extensions =
                      extensionsByContract.getOrDefault(c.getId(), List.of());
                  Optional<LocalDate> effectiveEnd =
                      EffectiveEndDateHelper.computeEffectiveEndDate(c.getEndDate(), extensions);
                  if (effectiveEnd.isEmpty()) {
                    return false;
                  }
                  LocalDate noticeDate = effectiveEnd.get().minusDays(c.getLandlordNoticeDays());
                  return !today.isBefore(noticeDate);
                })
            .filter(
                c -> {
                  List<ContractExtension> extensions =
                      extensionsByContract.getOrDefault(c.getId(), List.of());
                  return extensions.stream().noneMatch(e -> e.getStatus() == DRAFT)
                      && extensions.stream().noneMatch(e -> e.getStatus() == ACTIVE);
                })
            .toList();

    int created = 0;

    for (Contract contract : candidates) {
      try {
        List<ContractExtension> extensions =
            extensionsByContract.getOrDefault(contract.getId(), List.of());
        transactionTemplate.executeWithoutResult(
            status -> processAutoExtensionForContract(contract, extensions, teamId, today));
        created++;
      } catch (Exception e) {
        log.error(
            "Auto-extension failed for contract {} in team {}",
            contract.getIdentifier().orElse(Sid.of("unknown")),
            teamId,
            e);
      }
    }

    return created;
  }

  private void processAutoExtensionForContract(
      Contract contract, List<ContractExtension> existingExtensions, UUID teamId, LocalDate today) {
    Optional<LocalDate> effectiveEndDate =
        EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), existingExtensions);
    LocalDate previousEndDate = effectiveEndDate.orElseThrow();
    MoneyAmount previousRent = getCurrentRent(contract, existingExtensions);

    // BR-14: Check max renewals
    boolean isRollover = false;
    if (contract.getMaxRenewals().isPresent()) {
      int used = extensionRepository.countActiveAndSuperseded(contract.getId(), teamId);
      if (used >= contract.getMaxRenewals().get()) {
        // BR-35: Rollover to indefinite
        isRollover = true;
      }
    }

    MoneyAmount newRent =
        isRollover
            ? previousRent
            : computeNewRent(
                previousRent, contract.getRentAdjustmentType(), contract.getRentAdjustmentValue());

    Optional<LocalDate> newEndDate;
    if (isRollover) {
      // BR-35: new_end_date = NULL for rollover to indefinite
      newEndDate = Optional.empty();
    } else {
      Integer termMonths = contract.getRenewalTermMonths().orElse(12);
      newEndDate = Optional.of(previousEndDate.plusMonths(termMonths));
    }

    int extensionNumber = extensionRepository.getNextExtensionNumber(contract.getId(), teamId);

    ContractExtension extension =
        ContractExtension.builder()
            .identifier(Optional.of(newContractExtensionId()))
            .teamId(teamId)
            .contractId(contract.getId())
            .extensionNumber(extensionNumber)
            .previousEndDate(previousEndDate)
            .newEndDate(newEndDate)
            .previousRentAmount(previousRent)
            .newRentAmount(newRent)
            .rentAdjustmentType(
                isRollover ? RentAdjustmentType.NONE : contract.getRentAdjustmentType())
            .rentAdjustmentValue(isRollover ? Optional.empty() : contract.getRentAdjustmentValue())
            .status(DRAFT)
            .triggerType(ContractExtension.TriggerType.AUTO)
            .createdBy(SYSTEM_USER_ID)
            .updatedBy(SYSTEM_USER_ID)
            .build();

    extension = extensionRepository.save(extension);

    // BR-31: If !requires_tenant_confirmation, immediately activate (no contact confirmation
    // needed)
    if (!contract.getRequiresTenantConfirmation()) {
      doActivate(extension, contract, teamId, SYSTEM_USER_ID);
    } else {
      // BR-32: Send pending notification
      sendExtensionPendingNotification(contract, extension, teamId);
    }

    // BR-36: Set renewal_mode to NONE after rollover
    if (isRollover) {
      contract.setRenewalMode(Contract.RenewalMode.NONE);
      contract.setUpdatedBy(SYSTEM_USER_ID);
      contractRepository.save(contract);

      sendRolloverNotification(contract, teamId);
    }
  }

  // ── Renewal Reminder Job ─────────────────────────────────────────────

  public void processRenewalReminders() {
    log.info("Starting renewal reminder processing");

    List<UUID> teamIds = teamRepository.findAllActiveTeamIds();
    int totalSent = 0;

    for (UUID teamId : teamIds) {
      try {
        totalSent += processRenewalRemindersForTeam(teamId);
      } catch (Exception e) {
        log.error("Renewal reminder failed for team {}", teamId, e);
      }
    }

    log.info("Renewal reminders completed. Sent {} reminders", totalSent);
  }

  int processRenewalRemindersForTeam(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    int sent = 0;

    List<Contract> contracts =
        contractRepository.findActiveByTeamId(teamId).stream()
            .filter(c -> c.getRenewalMode() != Contract.RenewalMode.NONE)
            .filter(c -> c.getContractType() == Contract.ContractType.FIXED_TERM)
            .toList();

    if (contracts.isEmpty()) {
      return 0;
    }

    // Batch-load all extensions for filtered contracts (eliminates N+1)
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    Map<UUID, List<ContractExtension>> extensionsByContract =
        extensionRepository.findByContractIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.groupingBy(ContractExtension::getContractId));

    for (Contract contract : contracts) {
      List<ContractExtension> extensions =
          extensionsByContract.getOrDefault(contract.getId(), List.of());

      // Skip if an extension already exists (notification already sent when created)
      boolean hasRecentExtension =
          extensions.stream().anyMatch(e -> e.getStatus() == DRAFT || e.getStatus() == ACTIVE);
      if (hasRecentExtension) {
        continue;
      }

      Optional<LocalDate> effectiveEnd =
          EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);

      if (effectiveEnd.isEmpty()) {
        continue;
      }

      LocalDate noticeDate = effectiveEnd.get().minusDays(contract.getLandlordNoticeDays());
      if (!today.isBefore(noticeDate) && !today.isAfter(noticeDate.plusDays(7))) {
        transactionTemplate.executeWithoutResult(
            status -> sendRenewalReminderNotification(contract, effectiveEnd.get(), teamId));
        sent++;
      }
    }

    return sent;
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  public List<ContractExtension> getExtensionsForContract(UUID contractId, UUID teamId) {
    return extensionRepository.findByContractIdAndTeamId(contractId, teamId);
  }

  public int getExtensionCount(UUID contractId, UUID teamId) {
    return extensionRepository.countActiveAndSuperseded(contractId, teamId);
  }

  private ContractExtensionResponse doActivate(
      ContractExtension extension, Contract contract, UUID teamId, UUID activatedBy) {
    Instant now = Instant.now(clock);

    // Supersede previous ACTIVE extension
    extensionRepository
        .findActiveByContractId(contract.getId(), teamId)
        .ifPresent(
            prev -> {
              prev.setStatus(SUPERSEDED);
              prev.setSupersededAt(Optional.of(now));
              prev.setUpdatedBy(activatedBy);
              extensionRepository.save(prev);
            });

    // Set ACTIVE status
    extension.setStatus(ACTIVE);
    extension.setActivatedAt(Optional.of(now));
    extension.setActivatedBy(Optional.of(activatedBy));
    extension.setUpdatedBy(activatedBy);

    // BR-27: Close previous rent period's effective_to
    LocalDate previousEndDate = extension.getPreviousEndDate();
    List<ContractRentPeriod> existingPeriods =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    existingPeriods.stream()
        .filter(p -> p.getDeletedAt().isEmpty())
        .max(java.util.Comparator.comparing(ContractRentPeriod::getEffectiveFrom))
        .ifPresent(
            prev -> {
              prev.setEffectiveTo(Optional.of(previousEndDate));
              prev.setUpdatedBy(activatedBy);
              rentPeriodRepository.save(prev);
            });

    // BR-23: Create DRAFT rent period
    UUID rentPeriodId = createDraftRentPeriod(extension, contract, teamId, activatedBy);
    extension.setRentPeriodId(Optional.of(rentPeriodId));

    extension = extensionRepository.save(extension);

    // Send notification
    sendExtensionActivatedNotification(contract, extension, teamId);

    auditService.logUpdate(
        teamId,
        "CONTRACT_EXTENSION",
        extension.getId(),
        activatedBy,
        null,
        extension,
        Map.of("status", ACTIVE.name()));

    return toResponse(extension, contract.getIdentifier().orElseThrow());
  }

  private UUID createDraftRentPeriod(
      ContractExtension extension, Contract contract, UUID teamId, UUID userId) {
    // BR-24: effective_from = previous_end_date + 1 day
    LocalDate effectiveFrom = extension.getPreviousEndDate().plusDays(1);

    Instant now = Instant.now(clock);
    ContractRentPeriod rentPeriod =
        ContractRentPeriod.builder()
            .identifier(Optional.of(newContractRentPeriodId()))
            .teamId(teamId)
            .contractId(contract.getId())
            .rentAmount(extension.getNewRentAmount())
            .effectiveFrom(effectiveFrom)
            .effectiveTo(extension.getNewEndDate())
            .notes(Optional.of("Auto-created from extension #" + extension.getExtensionNumber()))
            .createdAt(now)
            .updatedAt(now)
            .createdBy(userId)
            .updatedBy(userId)
            .build();

    rentPeriod = rentPeriodRepository.save(rentPeriod);
    return rentPeriod.getId();
  }

  private MoneyAmount getCurrentRent(Contract contract, List<ContractExtension> extensions) {
    return extensions.stream()
        .filter(e -> e.getStatus() == ACTIVE)
        .filter(e -> e.getDeletedAt().isEmpty())
        .max(java.util.Comparator.comparingInt(ContractExtension::getExtensionNumber))
        .map(ContractExtension::getNewRentAmount)
        .orElse(contract.getRentAmount());
  }

  private MoneyAmount computeNewRent(
      MoneyAmount previousRent, RentAdjustmentType type, Optional<BigDecimal> value) {
    return switch (type) {
      case NONE -> previousRent;
      case FIXED_AMOUNT -> {
        BigDecimal v =
            value.orElseThrow(
                () -> new BadRequestException("Rent adjustment value required for FIXED_AMOUNT"));
        BigDecimal newAmount = previousRent.value().add(v);
        yield MoneyAmount.of(newAmount, previousRent.currency());
      }
      case FIXED_PERCENTAGE -> {
        BigDecimal v =
            value.orElseThrow(
                () ->
                    new BadRequestException("Rent adjustment value required for FIXED_PERCENTAGE"));
        BigDecimal multiplier =
            BigDecimal.ONE.add(v.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal newAmount =
            previousRent.value().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        yield MoneyAmount.of(newAmount, previousRent.currency());
      }
      case MANUAL -> {
        throw new BadRequestException("MANUAL adjustment requires explicit new rent amount");
      }
    };
  }

  private void validateExtensionBelongsToContract(ContractExtension extension, Contract contract) {
    if (!extension.getContractId().equals(contract.getId())) {
      throw new BadRequestException("Extension does not belong to this contract");
    }
  }

  private void validateStatus(
      ContractExtension extension, ContractExtension.ExtensionStatus expected, String action) {
    if (extension.getStatus() != expected) {
      throw new BusinessRuleException(
          String.format("Cannot %s extension in %s status", action, extension.getStatus()));
    }
  }

  private ContractExtensionResponse toResponse(ContractExtension ext, Sid contractIdentifier) {
    return new ContractExtensionResponse(
        ext.getIdentifier().orElseThrow(),
        contractIdentifier,
        ext.getExtensionNumber(),
        ext.getPreviousEndDate(),
        ext.getNewEndDate(),
        ext.getPreviousRentAmount().value(),
        ext.getPreviousRentAmount().currency(),
        ext.getNewRentAmount().value(),
        ext.getNewRentAmount().currency(),
        ext.getRentAdjustmentType(),
        ext.getRentAdjustmentValue(),
        ext.getStatus(),
        ext.getTriggerType(),
        ext.getNotes(),
        ext.getDeclinedReason(),
        ext.getActivatedAt(),
        ext.getConfirmedAt(),
        ext.getSupersededAt(),
        ext.getCreatedAt());
  }

  // ── Notifications ────────────────────────────────────────────────────

  private void sendExtensionActivatedNotification(
      Contract contract, ContractExtension extension, UUID teamId) {
    try {
      Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
      Map<String, Object> vars = buildNotificationVars(contract, extension, property);
      notificationService.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(teamId))
              .notificationType(CONTRACT_EXTENDED)
              .templateName("contract-extended")
              .templateVariables(vars)
              .createdBy(SYSTEM_USER_ID)
              .build());
    } catch (Exception e) {
      log.error(
          "Failed to send extension activated notification for contract {}", contract.getId(), e);
    }
  }

  private void sendExtensionPendingNotification(
      Contract contract, ContractExtension extension, UUID teamId) {
    try {
      Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
      Map<String, Object> vars = buildNotificationVars(contract, extension, property);
      notificationService.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(teamId))
              .notificationType(CONTRACT_EXTENSION_PENDING)
              .templateName("contract-extension-pending")
              .templateVariables(vars)
              .createdBy(SYSTEM_USER_ID)
              .build());
    } catch (Exception e) {
      log.error(
          "Failed to send extension pending notification for contract {}", contract.getId(), e);
    }
  }

  private void sendRenewalReminderNotification(
      Contract contract, LocalDate effectiveEndDate, UUID teamId) {
    try {
      Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
      long daysRemaining =
          java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(clock), effectiveEndDate);
      Map<String, Object> vars = new HashMap<>();
      vars.put("propertyName", property.getStreet() + ", " + property.getCity());
      vars.put("endDate", effectiveEndDate.toString());
      vars.put("daysRemaining", daysRemaining);
      vars.put("renewalMode", contract.getRenewalMode().name());
      vars.put("renewalTermMonths", contract.getRenewalTermMonths().orElse(12));
      vars.put("contactName", "");
      vars.put(
          "contractUrl",
          appProperties.email().baseUrl()
              + "/contracts/"
              + contract.getIdentifier().map(Sid::value).orElse(""));

      notificationService.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(teamId))
              .notificationType(CONTRACT_RENEWAL_REMINDER)
              .templateName("contract-renewal-reminder")
              .templateVariables(vars)
              .createdBy(SYSTEM_USER_ID)
              .build());
    } catch (Exception e) {
      log.error("Failed to send renewal reminder for contract {}", contract.getId(), e);
    }
  }

  private void sendRolloverNotification(Contract contract, UUID teamId) {
    try {
      Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
      Map<String, Object> vars = new HashMap<>();
      vars.put("propertyName", property.getStreet() + ", " + property.getCity());
      vars.put("contractIdentifier", contract.getIdentifier().map(Sid::value).orElse(""));

      notificationService.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(teamId))
              .notificationType(CONTRACT_ROLLED_OVER_TO_INDEFINITE)
              .templateName("contract-rolled-over")
              .templateVariables(vars)
              .createdBy(SYSTEM_USER_ID)
              .build());
    } catch (Exception e) {
      log.error("Failed to send rollover notification for contract {}", contract.getId(), e);
    }
  }

  private Map<String, Object> buildNotificationVars(
      Contract contract, ContractExtension extension, Property property) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("propertyName", property.getStreet() + ", " + property.getCity());
    vars.put("extensionNumber", extension.getExtensionNumber());
    vars.put("previousEndDate", extension.getPreviousEndDate().toString());
    vars.put("newEndDate", extension.getNewEndDate().map(LocalDate::toString).orElse("Indefinite"));
    vars.put(
        "previousRentFormatted",
        extension.getPreviousRentAmount().currency()
            + " "
            + extension.getPreviousRentAmount().value());
    vars.put(
        "newRentFormatted",
        extension.getNewRentAmount().currency() + " " + extension.getNewRentAmount().value());
    vars.put("triggerType", extension.getTriggerType().name());
    return vars;
  }
}
