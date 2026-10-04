package com.buurman.service;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.Constants.SYSTEM_USER_ID;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Team;
import com.buurman.domain.TeamPreferences;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentSchedulingService {

  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository paymentReceivalRepository;
  private final TeamRepository teamRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final AuditService auditService;
  private final Clock clock;
  private final ContractTerminationRepository terminationRepository;

  /**
   * Scheduled job that generates future payments for all teams with auto-generation enabled. Runs
   * every hour.
   */
  @Transactional
  public void scheduledPaymentGeneration() {
    log.info("Starting scheduled payment generation");
    long startTime = clock.millis();

    List<UUID> teamIds = teamPreferencesRepository.findTeamIdsWithAutoGenerationEnabled();
    List<Team> teams = teamIds.stream().map(teamRepository::getById).toList();

    if (teams.isEmpty()) {
      log.info("No teams with auto-generation enabled, skipping");
      return;
    }

    int totalPaymentsGenerated = 0;
    int teamsProcessed = 0;
    int teamsFailed = 0;

    for (Team team : teams) {
      try {
        int count = generateFuturePaymentsForTeam(team.getId(), SYSTEM_USER_ID);
        totalPaymentsGenerated += count;
        teamsProcessed++;

        if (count > 0) {
          log.info("Generated {} payments for team {}", count, team.getIdentifier().orElseThrow());
        }
      } catch (Exception e) {
        teamsFailed++;
        log.error(
            "Failed to generate payments for team {}: {}",
            team.getIdentifier().orElseThrow(),
            e.getMessage(),
            e);
      }
    }

    long duration = clock.millis() - startTime;
    log.info(
        "Payment generation completed in {}ms: {} teams processed, {} failed, {} payments"
            + " generated",
        duration,
        teamsProcessed,
        teamsFailed,
        totalPaymentsGenerated);
  }

  @Transactional
  public int generateFuturePaymentsForContract(UUID contractId, UUID teamId, UUID userId) {
    Contract contract = contractRepository.getByIdAndTeamId(contractId, teamId);

    // NOTICE_GIVEN is in force too: a tenant under notice still owes rent until the effective end
    // date, which generatePayments enforces via billingEndDate.
    if (!contract.getStatus().isInForce()) {
      log.debug(
          "Skipping payment generation for contract not in force: {}",
          contract.getIdentifier().orElseThrow());
      return 0;
    }

    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);
    if (!prefs.isAutoGenerationEnabled()) {
      return 0;
    }

    int paymentsAheadCount = prefs.getPaymentsAheadCount();
    return generatePayments(contract, teamId, userId, paymentsAheadCount, true, false, null);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public int generatePaymentsManually(
      UUID contractId,
      UUID teamId,
      UUID userId,
      int count,
      boolean markAsPaid,
      @Nullable LocalDate paymentDate) {
    Contract contract = contractRepository.getByIdAndTeamId(contractId, teamId);

    if (!contract.getStatus().isInForce()) {
      throw new BusinessRuleException(
          "Can only generate payments for ACTIVE or NOTICE_GIVEN contracts");
    }

    return generatePayments(contract, teamId, userId, count, false, markAsPaid, paymentDate);
  }

  private int generatePayments(
      Contract contract,
      UUID teamId,
      UUID userId,
      int count,
      boolean autoGenerated,
      boolean markAsPaid,
      @Nullable LocalDate paymentDate) {
    int paymentsCreated = 0;
    LocalDate currentDate = LocalDate.now(clock);
    UUID contractId = contract.getId();
    String notes = autoGenerated ? "Auto-generated payment" : "Manually scheduled payment";

    Optional<LocalDate> effectiveEndDate = billingEndDate(contract, teamId);

    // Resolve primary tenant contact for auto-linking payments
    Optional<UUID> primaryContactId =
        contractPartyRepository
            .findPrimaryContactByContractIdAndTeamId(contractId, teamId)
            .flatMap(party -> party.getContactId());

    for (int i = 0; i < count; i++) {
      LocalDate nextDueDate = calculateNextDueDate(currentDate, i, contract);

      if (effectiveEndDate.isPresent() && nextDueDate.isAfter(effectiveEndDate.get())) {
        log.debug(
            "Stopping payment generation: next due date {} is after contract effective end date {}",
            nextDueDate,
            effectiveEndDate.get());
        break;
      }

      if (paymentRepository.existsByContractIdAndDueDate(contractId, nextDueDate)) {
        log.debug(
            "Payment already exists for contract {} on due date {}, skipping",
            contract.getIdentifier().orElseThrow(),
            nextDueDate);
        continue;
      }

      try {
        // Use rent period amount for the due date, falling back to contract rent amount
        java.math.BigDecimal paymentAmount = contract.getRentAmount().value();
        var rentPeriod =
            rentPeriodRepository.findAtDateByContractIdAndTeamId(contractId, teamId, nextDueDate);
        if (rentPeriod.isPresent()) {
          paymentAmount = rentPeriod.get().getRentAmount().value();
        }

        String currency = contract.getRentAmount().currency();

        Payment payment = new Payment();
        payment.setIdentifier(Optional.of(newPaymentId()));
        payment.setTeamId(teamId);
        payment.setContractId(contractId);
        payment.setContactId(primaryContactId);
        payment.setAmount(com.buurman.util.MoneyAmount.of(paymentAmount, currency));
        payment.setDueDate(nextDueDate);
        payment.setStatus(markAsPaid ? PAID : PENDING);
        payment.setNotes(Optional.of(notes));
        payment.setAutoGenerated(autoGenerated);
        payment.setCreatedAt(clock.instant());
        payment.setUpdatedAt(clock.instant());
        payment.setCreatedBy(userId);
        payment.setUpdatedBy(userId);

        if (markAsPaid) {
          payment.setPaymentDate(Optional.ofNullable(paymentDate));
        }

        paymentRepository.save(payment);
        paymentsCreated++;

        // Create receival record when marking as paid
        if (markAsPaid) {
          PaymentReceival receival = new PaymentReceival();
          receival.setIdentifier(Optional.of(newPaymentReceivalId()));
          receival.setTeamId(teamId);
          receival.setPaymentId(payment.getId());
          receival.setAmount(com.buurman.util.MoneyAmount.of(paymentAmount, currency));
          receival.setReceivalDate(paymentDate != null ? paymentDate : nextDueDate);
          receival.setCreatedBy(userId);
          receival.setUpdatedBy(userId);
          receival.setCreatedAt(clock.instant());
          receival.setUpdatedAt(clock.instant());
          paymentReceivalRepository.save(receival);
        }

        log.debug(
            "Created {} payment for contract {} with due date {}{}",
            autoGenerated ? "auto-generated" : "manual",
            contract.getIdentifier().orElseThrow(),
            nextDueDate,
            markAsPaid ? " (marked as paid)" : "");

        auditService.logCreate(teamId, "payment", payment.getId(), userId, payment);

      } catch (DataIntegrityViolationException e) {
        log.info(
            "Payment already exists for contract {} on due date {} (caught race condition)",
            contract.getIdentifier().orElseThrow(),
            nextDueDate);
      }
    }

    log.info(
        "Generated {} {} payments for contract {}{}",
        paymentsCreated,
        autoGenerated ? "auto" : "manual",
        contract.getIdentifier().orElseThrow(),
        markAsPaid ? " (all marked as paid)" : "");
    return paymentsCreated;
  }

  /**
   * The last date rent may fall due for {@code contract}: the extension-aware effective end date,
   * capped by the effective end date of a termination on record (notice given). Empty means
   * open-ended.
   */
  public Optional<LocalDate> billingEndDate(Contract contract, UUID teamId) {
    List<ContractExtension> extensions =
        contractExtensionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    return EffectiveEndDateHelper.computeEffectiveEndDate(
        contract.getEndDate(),
        extensions,
        terminationRepository.findByContractIdAndTeamId(contract.getId(), teamId));
  }

  private LocalDate calculateNextDueDate(LocalDate startDate, int periodIndex, Contract contract) {
    LocalDate nextDueDate = startDate;

    nextDueDate =
        switch (contract.getPaymentFrequency()) {
          case MONTHLY -> nextDueDate.plusMonths(periodIndex + 1);
          case QUARTERLY -> nextDueDate.plusMonths((periodIndex + 1) * 3L);
          case ANNUALLY -> nextDueDate.plusYears(periodIndex + 1);
        };

    int paymentDueDay = contract.getPaymentDueDay().orElse(1);
    int daysInMonth = nextDueDate.lengthOfMonth();
    int actualDay = Math.min(paymentDueDay, daysInMonth);

    return nextDueDate.withDayOfMonth(actualDay);
  }

  @Transactional
  public int generateFuturePaymentsForTeam(UUID teamId, UUID userId) {
    log.debug("Generating future payments for team {}", teamId);

    List<Contract> activeContracts = contractRepository.findInForceByTeamId(teamId);

    int totalPaymentsCreated = 0;

    for (Contract contract : activeContracts) {
      try {
        int count = generateFuturePaymentsForContract(contract.getId(), teamId, userId);
        totalPaymentsCreated += count;
      } catch (Exception e) {
        log.error(
            "Failed to generate payments for contract {}: {}",
            contract.getIdentifier().orElseThrow(),
            e.getMessage(),
            e);
      }
    }

    log.info(
        "Generated {} payments for team {} ({} in-force contracts)",
        totalPaymentsCreated,
        teamId,
        activeContracts.size());

    return totalPaymentsCreated;
  }

  @Transactional
  public int cancelFuturePaymentsForContract(UUID contractId, UUID teamId, UUID userId) {
    log.debug("Cancelling future payments for contract {}", contractId);

    List<Payment> futurePayments =
        paymentRepository.findFuturePendingByContractId(contractId, teamId);

    int cancelledCount = 0;

    for (Payment payment : futurePayments) {
      try {
        paymentRepository.softDeleteByIdAndTeamId(payment.getId(), teamId);
        cancelledCount++;

        log.debug(
            "Soft-deleted future payment {} (due: {})",
            payment.getIdentifier().orElseThrow(),
            payment.getDueDate());

        auditService.logDelete(teamId, "payment", payment.getId(), userId, payment);

      } catch (Exception e) {
        log.error(
            "Failed to delete payment {}: {}",
            payment.getIdentifier().orElseThrow(),
            e.getMessage(),
            e);
      }
    }

    log.info("Cancelled {} future payments for contract {}", cancelledCount, contractId);
    return cancelledCount;
  }

  @Transactional
  public void handleContractStatusChange(
      UUID contractId, Contract.ContractStatus newStatus, UUID teamId, UUID userId) {
    log.debug("Handling contract status change to {} for contract {}", newStatus, contractId);

    switch (newStatus) {
      case ACTIVE -> {
        int paymentsCreated = generateFuturePaymentsForContract(contractId, teamId, userId);
        log.info(
            "Contract {} activated: generated {} future payments", contractId, paymentsCreated);
      }
      case EXPIRED, TERMINATED, DRAFT -> {
        int paymentsCancelled = cancelFuturePaymentsForContract(contractId, teamId, userId);
        log.info(
            "Contract {} changed to {}: cancelled {} future payments",
            contractId,
            newStatus,
            paymentsCancelled);
      }
      case PENDING_SIGNATURE ->
          log.debug("Contract {} moved to PENDING_SIGNATURE, no payment action needed", contractId);
      case NOTICE_GIVEN ->
          log.debug(
              "Contract {} moved to NOTICE_GIVEN, no payment action needed (NOTICE_GIVEN is in"
                  + " force: payments continue up to the termination's effective end date)",
              contractId);
    }
  }
}
