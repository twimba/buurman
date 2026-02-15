package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Payment;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.config.models.AppProperties;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AppProperties appProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void checkContractExpiry() {
        log.info("Running contract expiry check...");

        LocalDate thirtyDaysFromNow = LocalDate.now(clock).plusDays(30);

        teamRepository.findAllWithAutoGenerationEnabled().forEach(team -> {
            try {
                List<Contract> expiringContracts = contractRepository.findExpiringContracts(
                        team.getId(), thirtyDaysFromNow);

                for (Contract contract : expiringContracts) {
                    int daysUntilExpiry = (int) DAYS.between(LocalDate.now(clock), contract.getEndDate());

                    if (daysUntilExpiry != 30 && daysUntilExpiry != 14 &&
                            daysUntilExpiry != 7 && daysUntilExpiry != 3 && daysUntilExpiry != 1) {
                        continue;
                    }

                    String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

                    notifyTeamMembers(team.getId(), user ->
                            notificationService.send(SendNotificationRequest.builder()
                                    .teamId(team.getId())
                                    .notificationType(NotificationType.CONTRACT_EXPIRY)
                                    .recipientUserId(user.getId())
                                    .recipientEmail(user.getEmail())
                                    .recipientPhone(user.getPhone())
                                    .templateName("contract-expiry")
                                    .templateVariables(Map.of(
                                            "userName", user.getFirstName(),
                                            "propertyName", propertyName,
                                            "daysUntilExpiry", daysUntilExpiry,
                                            "expiryDate", formatDate(contract.getEndDate()),
                                            "baseUrl", appProperties.email().baseUrl()
                                    ))
                                    .build()));
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

    @Transactional(readOnly = true)
    public void checkPaymentReminders() {
        log.info("Running payment reminder check...");

        teamRepository.findAllWithAutoGenerationEnabled().forEach(team -> {
            try {
                List<Payment> overduePayments = paymentRepository.findOverduePayments(team.getId());

                for (Payment payment : overduePayments) {
                    Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), team.getId())
                            .orElse(null);
                    if (contract == null) {
                        continue;
                    }

                    String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

                    notifyTeamMembers(team.getId(), user ->
                            notificationService.send(SendNotificationRequest.builder()
                                    .teamId(team.getId())
                                    .notificationType(NotificationType.PAYMENT_REMINDER)
                                    .recipientUserId(user.getId())
                                    .recipientEmail(user.getEmail())
                                    .recipientPhone(user.getPhone())
                                    .templateName("payment-reminder")
                                    .templateVariables(Map.of(
                                            "userName", user.getFirstName(),
                                            "propertyName", propertyName,
                                            "amount", formatCurrency(payment.getAmount()),
                                            "dueDate", formatDate(payment.getDueDate()),
                                            "baseUrl", appProperties.email().baseUrl()
                                    ))
                                    .build()));
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
            if ("TEAM_ADMIN".equals(member.getRole()) || "TEAM_EDITOR".equals(member.getRole())) {
                userRepository.findById(member.getUserId()).ifPresent(notifier);
            }
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "";
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? String.format("€%.2f", amount) : "";
    }
}
