package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerService.class);

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationSchedulerService(ContractRepository contractRepository,
                                        PaymentRepository paymentRepository,
                                        PropertyRepository propertyRepository,
                                        TeamRepository teamRepository,
                                        TeamMemberRepository teamMemberRepository,
                                        UserRepository userRepository,
                                        EmailService emailService) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.propertyRepository = propertyRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Check for contracts expiring within 30 days and send alerts.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    public void checkContractExpiry() {
        log.info("Running contract expiry check...");

        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);

        teamRepository.findAllWithAutoGenerationEnabled().forEach(team -> {
            try {
                List<Contract> expiringContracts = contractRepository.findExpiringContracts(
                        team.getId(), thirtyDaysFromNow);

                for (Contract contract : expiringContracts) {
                    int daysUntilExpiry = (int) ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());

                    // Only send at 30, 14, 7, 3, 1 day marks to avoid spamming
                    if (daysUntilExpiry != 30 && daysUntilExpiry != 14 &&
                            daysUntilExpiry != 7 && daysUntilExpiry != 3 && daysUntilExpiry != 1) {
                        continue;
                    }

                    String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

                    // Notify all team admins and editors
                    notifyTeamMembers(team.getId(), user ->
                            emailService.sendContractExpiryAlert(user, contract, propertyName, daysUntilExpiry));
                }

                if (!expiringContracts.isEmpty()) {
                    log.info("Found {} expiring contracts for team {}", expiringContracts.size(), team.getIdentifier());
                }
            } catch (Exception e) {
                log.error("Failed to check contract expiry for team {}: {}", team.getIdentifier(), e.getMessage(), e);
            }
        });

        log.info("Contract expiry check completed");
    }

    /**
     * Check for overdue/unpaid payments and send reminders.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    public void checkPaymentReminders() {
        log.info("Running payment reminder check...");

        teamRepository.findAllWithAutoGenerationEnabled().forEach(team -> {
            try {
                List<Payment> overduePayments = paymentRepository.findOverduePayments(team.getId());

                for (Payment payment : overduePayments) {
                    Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), team.getId())
                            .orElse(null);
                    if (contract == null) continue;

                    String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

                    // Notify all team admins and editors
                    notifyTeamMembers(team.getId(), user ->
                            emailService.sendPaymentReminder(user, payment, propertyName, payment.getAmount()));
                }

                if (!overduePayments.isEmpty()) {
                    log.info("Found {} overdue payments for team {}", overduePayments.size(), team.getIdentifier());
                }
            } catch (Exception e) {
                log.error("Failed to check payment reminders for team {}: {}", team.getIdentifier(), e.getMessage(), e);
            }
        });

        log.info("Payment reminder check completed");
    }

    private String getPropertyName(UUID propertyId, UUID teamId) {
        return propertyRepository.findByIdAndTeamId(propertyId, teamId)
                .map(p -> p.getStreet() + ", " + p.getCity())
                .orElse("Unknown Property");
    }

    private void notifyTeamMembers(UUID teamId, java.util.function.Consumer<User> notifier) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        for (TeamMember member : members) {
            // Only notify admins and editors
            if ("TEAM_ADMIN".equals(member.getRole()) || "TEAM_EDITOR".equals(member.getRole())) {
                userRepository.findById(member.getUserId()).ifPresent(notifier);
            }
        }
    }
}
