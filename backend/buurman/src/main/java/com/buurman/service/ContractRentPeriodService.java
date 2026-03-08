package com.buurman.service;

import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.SidGenerator.newContractRentPeriodId;
import static com.buurman.util.SidGenerator.newPaymentId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.dto.request.CreateRentPeriodRequest;
import com.buurman.dto.request.UpdateRentPeriodRequest;
import com.buurman.dto.response.AddRentPeriodResult;
import com.buurman.dto.response.RentPeriodResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.ContractRentPeriodMapper;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractRentPeriodService {

  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRentPeriodMapper rentPeriodMapper;
  private final AuditService auditService;
  private final NotificationService notificationService;
  private final ContractPartyService contractPartyService;
  private final AppProperties appProperties;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public AddRentPeriodResult addRentPeriod(
      ContractIdentifier contractIdentifier,
      CreateRentPeriodRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    validateEffectiveFrom(contract, request.effectiveFrom());
    List<Payment> conflictingPayments =
        findConflictingPayments(contract.getId(), teamId, request.effectiveFrom());

    // Close the previous period
    var previousPeriod =
        rentPeriodRepository.findPreviousPeriod(
            contract.getId(), teamId, request.effectiveFrom().plusDays(1));
    BigDecimal previousRentAmount = previousPeriod.map(p -> p.getRentAmount().value()).orElse(null);
    previousPeriod.ifPresent(
        prev ->
            rentPeriodRepository.setEffectiveTo(
                prev.getId(), teamId, request.effectiveFrom().minusDays(1)));

    // Create new period
    ContractRentPeriod period = new ContractRentPeriod();
    period.setIdentifier(Optional.of(newContractRentPeriodId()));
    period.setTeamId(teamId);
    period.setContractId(contract.getId());
    period.setRentAmount(
        com.buurman.util.MoneyAmount.of(request.rentAmount(), contract.getRentAmount().currency()));
    period.setEffectiveFrom(request.effectiveFrom());
    period.setEffectiveTo(Optional.empty());
    period.setNotes(request.notes());
    period.setCreatedBy(principal.getUserId());
    period.setUpdatedBy(principal.getUserId());
    period.setCreatedAt(clock.instant());
    period.setUpdatedAt(clock.instant());

    ContractRentPeriod saved = rentPeriodRepository.save(period);

    // Sync denormalized rent_amount on contract if this is the current period
    syncContractRentAmount(contract, teamId, principal.getUserId());

    // Update PENDING payments from effectiveFrom onwards
    updatePendingPayments(
        contract.getId(),
        teamId,
        request.effectiveFrom(),
        request.rentAmount(),
        principal.getUserId());

    // Create adjustment payments for non-PENDING payments affected by the retroactive change
    int adjustmentPaymentsCreated =
        createAdjustmentPayments(
            conflictingPayments,
            request.rentAmount(),
            contract.getId(),
            teamId,
            contract.getRentAmount().currency(),
            principal.getUserId());

    // Audit trail — log on the CONTRACT entity so it shows in contract history
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("rentPeriodAction", "ADDED");
    changedFields.put("newRentAmount", request.rentAmount());
    changedFields.put("effectiveFrom", request.effectiveFrom());
    if (previousRentAmount != null) {
      changedFields.put("previousRentAmount", previousRentAmount);
    }
    if (adjustmentPaymentsCreated > 0) {
      changedFields.put("adjustmentPaymentsCreated", adjustmentPaymentsCreated);
    }
    request.notes().ifPresent(n -> changedFields.put("notes", n));
    auditService.logCreate(teamId, "CONTRACT", contract.getId(), principal.getUserId(), saved);

    // Notification
    sendRentAdjustedNotification(
        contract,
        teamId,
        previousRentAmount,
        request.rentAmount(),
        request.effectiveFrom(),
        principal.getUserId());

    log.info(
        "Rent period added for contract {} in team {}: {} from {} (adjustment payments: {})",
        contractIdentifier,
        teamId,
        request.rentAmount(),
        request.effectiveFrom(),
        adjustmentPaymentsCreated);

    RentPeriodResponse response =
        rentPeriodMapper.toResponse(saved, Optional.ofNullable(previousRentAmount));
    return new AddRentPeriodResult(response, adjustmentPaymentsCreated);
  }

  public List<RentPeriodResponse> getRentTimeline(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    List<ContractRentPeriod> periods =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    return rentPeriodMapper.toResponses(periods);
  }

  public Optional<ContractRentPeriod> getCurrentRent(UUID contractId, UUID teamId) {
    return rentPeriodRepository.findCurrentByContractIdAndTeamId(contractId, teamId);
  }

  public Optional<ContractRentPeriod> getRentAtDate(UUID contractId, UUID teamId, LocalDate date) {
    return rentPeriodRepository.findAtDateByContractIdAndTeamId(contractId, teamId, date);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public RentPeriodResponse updateRentPeriod(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      UpdateRentPeriodRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractRentPeriod period =
        rentPeriodRepository.getByIdentifierAndTeamId(periodIdentifier, teamId);

    // Only allow editing future periods
    LocalDate today = LocalDate.now(clock);
    if (!period.getEffectiveFrom().isAfter(today)) {
      throw new BusinessRuleException("Can only edit rent periods that have not yet taken effect");
    }

    validateEffectiveFrom(contract, request.effectiveFrom());

    // Store old values for audit
    BigDecimal oldRentAmount = period.getRentAmount().value();
    LocalDate oldEffectiveFrom = period.getEffectiveFrom();

    period.setRentAmount(
        com.buurman.util.MoneyAmount.of(request.rentAmount(), period.getRentAmount().currency()));
    period.setEffectiveFrom(request.effectiveFrom());
    period.setNotes(request.notes());
    period.setUpdatedBy(principal.getUserId());
    period.setUpdatedAt(clock.instant());

    rentPeriodRepository.save(period);

    // Recalculate previous period's effective_to
    var previousPeriod =
        rentPeriodRepository.findPreviousPeriod(contract.getId(), teamId, request.effectiveFrom());
    previousPeriod.ifPresent(
        contractRentPeriod ->
            rentPeriodRepository.setEffectiveTo(
                contractRentPeriod.getId(), teamId, request.effectiveFrom().minusDays(1)));

    syncContractRentAmount(contract, teamId, principal.getUserId());

    // Update PENDING payments from effectiveFrom onwards
    updatePendingPayments(
        contract.getId(),
        teamId,
        request.effectiveFrom(),
        request.rentAmount(),
        principal.getUserId());

    // Audit
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("rentPeriodAction", "UPDATED");
    if (oldRentAmount.compareTo(request.rentAmount()) != 0) {
      changedFields.put("previousRentAmount", oldRentAmount);
      changedFields.put("newRentAmount", request.rentAmount());
    }
    if (!oldEffectiveFrom.equals(request.effectiveFrom())) {
      changedFields.put("previousEffectiveFrom", oldEffectiveFrom);
      changedFields.put("effectiveFrom", request.effectiveFrom());
    }
    auditService.logUpdate(
        teamId, "CONTRACT", contract.getId(), principal.getUserId(), null, period, changedFields);

    log.info("Rent period updated: {} in contract {}", periodIdentifier, contractIdentifier);

    Optional<BigDecimal> prevAmount = previousPeriod.map(p -> p.getRentAmount().value());
    return rentPeriodMapper.toResponse(period, prevAmount);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteRentPeriod(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    ContractRentPeriod period =
        rentPeriodRepository.getByIdentifierAndTeamId(periodIdentifier, teamId);

    // Only allow deleting future periods
    LocalDate today = LocalDate.now(clock);
    if (!period.getEffectiveFrom().isAfter(today)) {
      throw new BusinessRuleException(
          "Can only delete rent periods that have not yet taken effect");
    }

    // Restore effective_to = NULL on the preceding period
    var previousPeriod =
        rentPeriodRepository.findPreviousPeriod(
            contract.getId(), teamId, period.getEffectiveFrom());
    previousPeriod.ifPresent(
        contractRentPeriod ->
            rentPeriodRepository.setEffectiveTo(contractRentPeriod.getId(), teamId, null));

    rentPeriodRepository.softDeleteByIdAndTeamId(period.getId(), teamId);

    syncContractRentAmount(contract, teamId, principal.getUserId());

    // Update PENDING payments: revert to previous period's amount
    previousPeriod.ifPresent(
        contractRentPeriod ->
            updatePendingPayments(
                contract.getId(),
                teamId,
                period.getEffectiveFrom(),
                contractRentPeriod.getRentAmount().value(),
                principal.getUserId()));

    // Audit
    auditService.logDelete(teamId, "CONTRACT", contract.getId(), principal.getUserId(), period);

    log.info("Rent period deleted: {} in contract {}", periodIdentifier, contractIdentifier);
  }

  /** Creates the initial rent period when a contract is created. */
  @Transactional
  public void createInitialRentPeriod(Contract contract, UserPrincipal principal) {
    ContractRentPeriod period = new ContractRentPeriod();
    period.setIdentifier(Optional.of(newContractRentPeriodId()));
    period.setTeamId(contract.getTeamId());
    period.setContractId(contract.getId());
    period.setRentAmount(contract.getRentAmount());
    period.setEffectiveFrom(contract.getStartDate());
    period.setEffectiveTo(Optional.empty());
    period.setCreatedBy(principal.getUserId());
    period.setUpdatedBy(principal.getUserId());
    period.setCreatedAt(clock.instant());
    period.setUpdatedAt(clock.instant());

    rentPeriodRepository.save(period);
    log.debug(
        "Initial rent period created for contract {}", contract.getIdentifier().orElseThrow());
  }

  /** Updates the initial rent period when a DRAFT contract's rent is edited. */
  @Transactional
  public void updateInitialRentPeriod(Contract contract, UserPrincipal principal) {
    List<ContractRentPeriod> periods =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), contract.getTeamId());
    if (periods.size() == 1) {
      ContractRentPeriod initial = periods.getFirst();
      initial.setRentAmount(contract.getRentAmount());
      initial.setEffectiveFrom(contract.getStartDate());
      initial.setUpdatedBy(principal.getUserId());
      initial.setUpdatedAt(clock.instant());
      rentPeriodRepository.save(initial);
    }
  }

  // --- Private helpers ---

  private void validateEffectiveFrom(Contract contract, LocalDate effectiveFrom) {
    if (effectiveFrom.isBefore(contract.getStartDate())) {
      throw new BusinessRuleException("Effective date cannot be before the contract start date");
    }
    if (contract.getEndDate().filter(effectiveFrom::isAfter).isPresent()) {
      throw new BusinessRuleException("Effective date cannot be after the contract end date");
    }
  }

  private List<Payment> findConflictingPayments(
      UUID contractId, UUID teamId, LocalDate effectiveFrom) {
    LocalDate today = LocalDate.now(clock);
    if (!effectiveFrom.isBefore(today)) {
      return List.of();
    }
    // Retroactive — find non-PENDING, non-CANCELLED payments in the affected range
    List<Payment> existingPayments = paymentRepository.findByContractId(contractId, teamId);
    return existingPayments.stream()
        .filter(
            p ->
                !p.getDueDate().isBefore(effectiveFrom)
                    && p.getStatus() != PENDING
                    && p.getStatus() != CANCELLED
                    && p.getDeletedAt().isEmpty())
        .toList();
  }

  private int createAdjustmentPayments(
      List<Payment> conflictingPayments,
      BigDecimal newRentAmount,
      UUID contractId,
      UUID teamId,
      String currency,
      UUID userId) {
    int created = 0;
    for (Payment existing : conflictingPayments) {
      BigDecimal oldAmount = existing.getAmount().value();
      BigDecimal difference = newRentAmount.subtract(oldAmount);

      if (difference.compareTo(BigDecimal.ZERO) > 0) {
        // Rent increase: create adjustment payment for the positive difference
        Payment adjustment = new Payment();
        adjustment.setIdentifier(Optional.of(newPaymentId()));
        adjustment.setTeamId(teamId);
        adjustment.setContractId(contractId);
        adjustment.setAmount(com.buurman.util.MoneyAmount.of(difference, currency));
        adjustment.setDueDate(existing.getDueDate());
        adjustment.setStatus(PENDING);
        adjustment.setNotes(
            Optional.of(
                String.format(
                    "Rent adjustment: difference from %s to %s (original payment %s)",
                    oldAmount, newRentAmount, existing.getIdentifier().orElseThrow())));
        adjustment.setAutoGenerated(true);
        adjustment.setCreatedBy(userId);
        adjustment.setUpdatedBy(userId);
        adjustment.setCreatedAt(clock.instant());
        adjustment.setUpdatedAt(clock.instant());

        paymentRepository.save(adjustment);

        auditService.logCreate(teamId, "PAYMENT", adjustment.getId(), userId, adjustment);
        log.info(
            "Created adjustment payment {} for difference {} on due date {}",
            adjustment.getIdentifier().orElseThrow(),
            difference,
            existing.getDueDate());
        created++;
      } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
        log.warn(
            "Skipped adjustment for payment {} — rent decrease of {} not supported as payment",
            existing.getIdentifier().orElseThrow(),
            difference.abs());
      }
    }
    return created;
  }

  private void syncContractRentAmount(Contract contract, UUID teamId, UUID userId) {
    var currentPeriod =
        rentPeriodRepository.findCurrentByContractIdAndTeamId(contract.getId(), teamId);
    currentPeriod.ifPresent(
        period -> {
          BigDecimal currentRent = period.getRentAmount().value();
          if (contract.getRentAmount().value().compareTo(currentRent) != 0) {
            contract.setRentAmount(
                com.buurman.util.MoneyAmount.of(currentRent, contract.getRentAmount().currency()));
            contract.setUpdatedBy(userId);
            contract.setUpdatedAt(clock.instant());
            contractRepository.save(contract);
            log.debug(
                "Synced contract {} rent_amount to {}",
                contract.getIdentifier().orElseThrow(),
                currentRent);
          }
        });
  }

  private void updatePendingPayments(
      UUID contractId, UUID teamId, LocalDate fromDate, BigDecimal newAmount, UUID userId) {
    List<Payment> pendingPayments =
        paymentRepository.findPendingByContractIdFromDate(contractId, teamId, fromDate);
    for (Payment payment : pendingPayments) {
      if (payment.getAmount().value().compareTo(newAmount) != 0) {
        BigDecimal oldAmount = payment.getAmount().value();
        payment.setAmount(
            com.buurman.util.MoneyAmount.of(newAmount, payment.getAmount().currency()));
        payment.setUpdatedBy(userId);
        paymentRepository.save(payment);

        Map<String, Object> changedFields = new HashMap<>();
        changedFields.put("amount", newAmount);
        changedFields.put("previousAmount", oldAmount);
        changedFields.put("reason", "Rent period adjustment");
        auditService.logUpdate(
            teamId, "payment", payment.getId(), userId, null, payment, changedFields);

        log.debug(
            "Updated payment {} amount from {} to {}",
            payment.getIdentifier().orElseThrow(),
            oldAmount,
            newAmount);
      }
    }
  }

  private void sendRentAdjustedNotification(
      Contract contract,
      UUID teamId,
      @Nullable BigDecimal oldRentAmount,
      BigDecimal newRentAmount,
      LocalDate effectiveFrom,
      UUID userId) {
    try {
      Property property =
          propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
      Tenant primaryTenant =
          contractPartyService.getPrimaryTenantForContract(contract.getId(), teamId);

      String propertyName =
          property != null
              ? property.getStreet() + ", " + property.getCity()
              : contract.getIdentifier().orElseThrow().toString();
      String tenantName =
          primaryTenant.getFirstName() + primaryTenant.getLastName().map(n -> " " + n).orElse("");

      Map<String, Object> vars = new HashMap<>();
      vars.put("propertyName", propertyName);
      vars.put("tenantName", tenantName);
      vars.put(
          "oldRentAmount",
          oldRentAmount != null
              ? contract.getRentAmount().currency() + " " + oldRentAmount
              : "N/A");
      vars.put("newRentAmount", contract.getRentAmount().currency() + " " + newRentAmount);
      vars.put("effectiveFrom", effectiveFrom.toString());
      vars.put("baseUrl", appProperties.email().baseUrl());

      notificationService.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(teamId)
              .notificationType(NotificationType.CONTRACT_RENT_ADJUSTED)
              .templateName("contract-rent-adjusted")
              .templateVariables(vars)
              .createdBy(userId)
              .build());
    } catch (Exception e) {
      log.error("Failed to send rent adjustment notification for contract {}", contract.getId(), e);
    }
  }
}
