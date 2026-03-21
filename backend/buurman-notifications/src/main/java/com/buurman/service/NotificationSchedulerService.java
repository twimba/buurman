package com.buurman.service;

import static com.buurman.domain.NotificationType.CONTRACT_EXPIRY;
import static com.buurman.domain.NotificationType.PAYMENT_REMINDER;
import static com.buurman.domain.TeamRole.TEAM_EDITOR;
import static com.buurman.util.Constants.SYSTEM_USER_ID;
import static java.time.temporal.ChronoUnit.DAYS;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.Payment;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamRole;
import com.buurman.domain.User;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSchedulerService {

  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final PaymentRepository paymentRepository;
  private final PropertyRepository propertyRepository;
  private final TeamRepository teamRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final AppProperties appProperties;
  private final Clock clock;

  @Transactional
  public void checkContractExpiry() {
    log.info("Running contract expiry check...");

    LocalDate thirtyDaysFromNow = LocalDate.now(clock).plusDays(30);

    List<Team> teams =
        teamPreferencesRepository.findTeamIdsWithAutoGenerationEnabled().stream()
            .map(teamRepository::getById)
            .toList();
    teams.forEach(
        team -> {
          try {
            List<Contract> expiringContracts =
                contractRepository.findExpiringContracts(team.getId(), thirtyDaysFromNow);

            for (Contract contract : expiringContracts) {
              // Skip contracts with automatic renewal — they auto-extend
              if (contract.getRenewalMode() == Contract.RenewalMode.AUTOMATIC) {
                continue;
              }

              List<ContractExtension> extensions =
                  contractExtensionRepository.findByContractIdAndTeamId(
                      contract.getId(), team.getId());
              Optional<LocalDate> effectiveEndDate =
                  EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);

              if (effectiveEndDate.isEmpty()) {
                continue;
              }
              LocalDate endDate = effectiveEndDate.get();
              int daysUntilExpiry = (int) DAYS.between(LocalDate.now(clock), endDate);

              if (daysUntilExpiry != 30
                  && daysUntilExpiry != 14
                  && daysUntilExpiry != 7
                  && daysUntilExpiry != 3
                  && daysUntilExpiry != 1) {
                continue;
              }

              String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

              notifyTeamMembers(
                  team.getId(),
                  user ->
                      notificationService.send(
                          SendNotificationRequest.builder()
                              .teamId(Optional.of(team.getId()))
                              .notificationType(CONTRACT_EXPIRY)
                              .recipientUserId(Optional.of(user.getId()))
                              .recipientEmail(Optional.of(user.getEmail()))
                              .recipientPhone(user.getPhone())
                              .templateName("contract-expiry")
                              .templateVariables(
                                  Map.of(
                                      "userName", user.getFirstName(),
                                      "propertyName", propertyName,
                                      "daysUntilExpiry", daysUntilExpiry,
                                      "expiryDate", formatDate(endDate),
                                      "baseUrl", appProperties.email().baseUrl()))
                              .urgency(
                                  daysUntilExpiry <= 7
                                      ? NotificationUrgency.URGENT
                                      : NotificationUrgency.NORMAL)
                              .createdBy(SYSTEM_USER_ID)
                              .build()));
            }

            if (!expiringContracts.isEmpty()) {
              log.info(
                  "Found {} expiring contracts for team {}",
                  expiringContracts.size(),
                  team.getIdentifier().orElseThrow());
            }
          } catch (Exception e) {
            log.error(
                "Failed to check contract expiry for team {}: {}",
                team.getIdentifier().orElseThrow(),
                e.getMessage(),
                e);
          }
        });

    log.info("Contract expiry check completed");
  }

  @Transactional
  public void checkPaymentReminders() {
    log.info("Running payment reminder check...");

    List<Team> activeTeams =
        teamPreferencesRepository.findTeamIdsWithAutoGenerationEnabled().stream()
            .map(teamRepository::getById)
            .toList();
    activeTeams.forEach(
        team -> {
          try {
            List<Payment> overduePayments = paymentRepository.findOverduePayments(team.getId());

            for (Payment payment : overduePayments) {
              Contract contract =
                  contractRepository
                      .findByIdAndTeamId(payment.getContractId(), team.getId())
                      .orElse(null);
              if (contract == null) {
                continue;
              }

              String propertyName = getPropertyName(contract.getPropertyId(), team.getId());

              notifyTeamMembers(
                  team.getId(),
                  user ->
                      notificationService.send(
                          SendNotificationRequest.builder()
                              .teamId(Optional.of(team.getId()))
                              .notificationType(PAYMENT_REMINDER)
                              .recipientUserId(Optional.of(user.getId()))
                              .recipientEmail(Optional.of(user.getEmail()))
                              .recipientPhone(user.getPhone())
                              .templateName("payment-reminder")
                              .templateVariables(
                                  Map.of(
                                      "userName", user.getFirstName(),
                                      "propertyName", propertyName,
                                      "amount", formatCurrency(payment.getAmount().value()),
                                      "dueDate", formatDate(payment.getDueDate()),
                                      "baseUrl", appProperties.email().baseUrl()))
                              .urgency(NotificationUrgency.URGENT)
                              .createdBy(SYSTEM_USER_ID)
                              .build()));
            }

            if (!overduePayments.isEmpty()) {
              log.info(
                  "Found {} overdue payments for team {}",
                  overduePayments.size(),
                  team.getIdentifier().orElseThrow());
            }
          } catch (Exception e) {
            log.error(
                "Failed to check payment reminders for team {}: {}",
                team.getIdentifier().orElseThrow(),
                e.getMessage(),
                e);
          }
        });

    log.info("Payment reminder check completed");
  }

  private String getPropertyName(UUID propertyId, UUID teamId) {
    return propertyRepository
        .findByIdAndTeamId(propertyId, teamId)
        .map(p -> p.getStreet() + ", " + p.getCity())
        .orElse("Unknown Property");
  }

  private void notifyTeamMembers(UUID teamId, java.util.function.Consumer<User> notifier) {
    List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
    for (TeamMember member : members) {
      if (member.getRole() == TeamRole.TEAM_ADMIN || member.getRole() == TEAM_EDITOR) {
        userRepository.findById(member.getUserId()).ifPresent(notifier);
      }
    }
  }

  private String formatDate(@Nullable LocalDate date) {
    return date != null ? date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "";
  }

  private String formatCurrency(BigDecimal amount) {
    return amount != null ? String.format("€%.2f", amount) : "";
  }
}
