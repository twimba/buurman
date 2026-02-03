package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Team;
import com.buurman.domain.TeamSettings;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.buurman.util.Constants.SYSTEM_USER_ID;

@Service
public class PaymentSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchedulingService.class);

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final TeamRepository teamRepository;
    private final AuditService auditService;

    public PaymentSchedulingService(ContractRepository contractRepository,
                                   PaymentRepository paymentRepository,
                                   TeamRepository teamRepository,
                                   AuditService auditService) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.teamRepository = teamRepository;
        this.auditService = auditService;
    }

    /**
     * Scheduled job that generates future payments for all teams with auto-generation enabled.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void scheduledPaymentGeneration() {
        log.info("Starting scheduled payment generation");
        long startTime = System.currentTimeMillis();

        List<Team> teams = teamRepository.findAllWithAutoGenerationEnabled();

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
                    log.info("Generated {} payments for team {}", count, team.getIdentifier());
                }
            } catch (Exception e) {
                teamsFailed++;
                log.error("Failed to generate payments for team {}: {}",
                        team.getIdentifier(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Payment generation completed in {}ms: {} teams processed, {} failed, {} payments generated",
                duration, teamsProcessed, teamsFailed, totalPaymentsGenerated);
    }

    @Transactional
    public int generateFuturePaymentsForContract(UUID contractId, UUID teamId, UUID userId) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, teamId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            log.debug("Skipping payment generation for non-ACTIVE contract: {}", contract.getIdentifier());
            return 0;
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamSettings settings = team.getSettings();
        if (settings == null || settings.getPayments() == null) {
            settings = new TeamSettings();
        }

        TeamSettings.PaymentSettings paymentSettings = settings.getPayments();
        if (!paymentSettings.getAutoGenerationEnabled()) {
            log.debug("Auto-generation disabled for team: {}", team.getIdentifier());
            return 0;
        }

        int paymentsAheadCount = paymentSettings.getPaymentsAheadCount();
        return generatePayments(contract, teamId, userId, paymentsAheadCount, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public int generatePaymentsManually(UUID contractId, UUID teamId, UUID userId, int count) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, teamId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw new IllegalStateException("Can only generate payments for ACTIVE contracts");
        }

        return generatePayments(contract, teamId, userId, count, false);
    }

    private int generatePayments(Contract contract, UUID teamId, UUID userId, int count, boolean autoGenerated) {
        int paymentsCreated = 0;
        LocalDate currentDate = LocalDate.now();
        UUID contractId = contract.getId();
        String notes = autoGenerated ? "Auto-generated payment" : "Manually generated payment";

        for (int i = 0; i < count; i++) {
            LocalDate nextDueDate = calculateNextDueDate(currentDate, i, contract);

            if (contract.getEndDate() != null && nextDueDate.isAfter(contract.getEndDate())) {
                log.debug("Stopping payment generation: next due date {} is after contract end date {}",
                        nextDueDate, contract.getEndDate());
                break;
            }

            if (paymentRepository.existsByContractIdAndDueDate(contractId, nextDueDate)) {
                log.debug("Payment already exists for contract {} on due date {}, skipping",
                        contract.getIdentifier(), nextDueDate);
                continue;
            }

            try {
                Payment payment = new Payment();
                payment.setIdentifier(UlidGenerator.generate());
                payment.setTeamId(teamId);
                payment.setContractId(contractId);
                payment.setAmount(contract.getRentAmount());
                payment.setCurrency(contract.getCurrency() != null ? contract.getCurrency() : "EUR");
                payment.setDueDate(nextDueDate);
                payment.setStatus(Payment.PaymentStatus.PENDING);
                payment.setNotes(notes);
                payment.setAutoGenerated(autoGenerated);
                payment.setCreatedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                payment.setCreatedBy(userId);
                payment.setUpdatedBy(userId);

                paymentRepository.save(payment);
                paymentsCreated++;

                log.debug("Created {} payment for contract {} with due date {}",
                        autoGenerated ? "auto-generated" : "manual", contract.getIdentifier(), nextDueDate);

                auditService.logCreate(teamId, "payment", payment.getId(), userId, payment);

            } catch (DataIntegrityViolationException e) {
                log.info("Payment already exists for contract {} on due date {} (caught race condition)",
                        contract.getIdentifier(), nextDueDate);
            }
        }

        log.info("Generated {} {} payments for contract {}",
                paymentsCreated, autoGenerated ? "auto" : "manual", contract.getIdentifier());
        return paymentsCreated;
    }

    private LocalDate calculateNextDueDate(LocalDate startDate, int periodIndex, Contract contract) {
        LocalDate nextDueDate = startDate;

        switch (contract.getPaymentFrequency()) {
            case MONTHLY:
                nextDueDate = nextDueDate.plusMonths(periodIndex + 1);
                break;
            case QUARTERLY:
                nextDueDate = nextDueDate.plusMonths((periodIndex + 1) * 3);
                break;
            case ANNUALLY:
                nextDueDate = nextDueDate.plusYears(periodIndex + 1);
                break;
        }

        int paymentDueDay = contract.getPaymentDueDay() != null ? contract.getPaymentDueDay() : 1;
        int daysInMonth = nextDueDate.lengthOfMonth();
        int actualDay = Math.min(paymentDueDay, daysInMonth);

        return nextDueDate.withDayOfMonth(actualDay);
    }

    @Transactional
    public int generateFuturePaymentsForTeam(UUID teamId, UUID userId) {
        log.debug("Generating future payments for team {}", teamId);

        List<Contract> activeContracts = contractRepository.findActiveByTeamId(teamId);

        int totalPaymentsCreated = 0;

        for (Contract contract : activeContracts) {
            try {
                int count = generateFuturePaymentsForContract(contract.getId(), teamId, userId);
                totalPaymentsCreated += count;
            } catch (Exception e) {
                log.error("Failed to generate payments for contract {}: {}",
                        contract.getIdentifier(), e.getMessage(), e);
            }
        }

        log.info("Generated {} payments for team {} ({} active contracts)",
                totalPaymentsCreated, teamId, activeContracts.size());

        return totalPaymentsCreated;
    }

    @Transactional
    public int cancelFuturePaymentsForContract(UUID contractId, UUID teamId, UUID userId) {
        log.debug("Cancelling future payments for contract {}", contractId);

        List<Payment> futurePayments = paymentRepository.findFuturePendingByContractId(contractId, teamId);

        int cancelledCount = 0;

        for (Payment payment : futurePayments) {
            try {
                paymentRepository.softDeleteByIdAndTeamId(payment.getId(), teamId);
                cancelledCount++;

                log.debug("Soft-deleted future payment {} (due: {})",
                        payment.getIdentifier(), payment.getDueDate());

                auditService.logDelete(teamId, "payment", payment.getId(), userId, payment);

            } catch (Exception e) {
                log.error("Failed to delete payment {}: {}",
                        payment.getIdentifier(), e.getMessage(), e);
            }
        }

        log.info("Cancelled {} future payments for contract {}", cancelledCount, contractId);
        return cancelledCount;
    }

    @Transactional
    public void handleContractStatusChange(UUID contractId, Contract.ContractStatus newStatus,
                                          UUID teamId, UUID userId) {
        log.debug("Handling contract status change to {} for contract {}", newStatus, contractId);

        switch (newStatus) {
            case ACTIVE:
                int paymentsCreated = generateFuturePaymentsForContract(contractId, teamId, userId);
                log.info("Contract {} activated: generated {} future payments", contractId, paymentsCreated);
                break;

            case EXPIRED:
            case TERMINATED:
            case DRAFT:
                int paymentsCancelled = cancelFuturePaymentsForContract(contractId, teamId, userId);
                log.info("Contract {} changed to {}: cancelled {} future payments",
                        contractId, newStatus, paymentsCancelled);
                break;

            case PENDING_SIGNATURE:
                log.debug("Contract {} moved to PENDING_SIGNATURE, no payment action needed", contractId);
                break;

            default:
                log.warn("Unknown contract status: {}", newStatus);
        }
    }
}
