package com.buurman.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReminderStep;
import com.buurman.domain.TeamPreferences;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamPreferencesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily dunning ladder. For every team that switched automatic reminders on, walks its open
 * payments and sends at most one ladder step per payment per run: the latest step whose day offset
 * has been reached and that was not sent before. Sending itself enforces the per-contract and
 * per-contact opt-in, so a team-level switch alone never emails anyone.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentDunningService {

  private final TeamPreferencesRepository teamPreferencesRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReminderRepository reminderRepository;
  private final PaymentReminderService reminderService;
  private final Clock clock;

  public void runDailyLadder() {
    LocalDate today = LocalDate.now(clock);
    List<UUID> teamIds = teamPreferencesRepository.findTeamIdsWithAutomaticRemindersEnabled();
    log.info("Running payment dunning ladder for {} team(s)", teamIds.size());
    int sent = 0;
    int skipped = 0;
    for (UUID teamId : teamIds) {
      TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);
      List<PaymentReminderStep> steps =
          prefs.getPaymentReminderSteps().stream().filter(PaymentReminderStep::enabled).toList();
      if (steps.isEmpty()) {
        continue;
      }
      int minOffset = steps.stream().mapToInt(PaymentReminderStep::offsetDays).min().orElse(0);
      List<Payment> candidates =
          paymentRepository.findOpenPaymentsDueOnOrBefore(teamId, today.minusDays(minOffset));
      for (Payment payment : candidates) {
        Set<Integer> alreadySent =
            reminderRepository.findAutomaticStepOffsets(payment.getId(), teamId);
        Optional<PaymentReminderStep> step =
            selectStep(steps, payment.getDueDate(), today, alreadySent);
        if (step.isEmpty()) {
          continue;
        }
        try {
          reminderService.sendAutomatic(payment, step.get());
          sent++;
        } catch (Exception e) {
          // Opt-out, pause, missing email, or already paid — expected, not an error.
          skipped++;
          log.debug(
              "Dunning step {} skipped for payment {}: {}",
              step.get().offsetDays(),
              payment.getIdentifier().map(Object::toString).orElse("?"),
              e.getMessage());
        }
      }
    }
    log.info("Payment dunning ladder completed — {} sent, {} skipped", sent, skipped);
  }

  /**
   * The latest enabled step whose offset has been reached today and that has not been sent yet.
   * Returning only the latest one means enabling the ladder on old arrears sends a single
   * appropriately-toned reminder instead of the whole ladder at once.
   */
  static Optional<PaymentReminderStep> selectStep(
      List<PaymentReminderStep> steps,
      LocalDate dueDate,
      LocalDate today,
      Set<Integer> sentOffsets) {
    return steps.stream()
        .filter(PaymentReminderStep::enabled)
        .filter(s -> !today.isBefore(dueDate.plusDays(s.offsetDays())))
        .filter(s -> !sentOffsets.contains(s.offsetDays()))
        .max(Comparator.comparingInt(PaymentReminderStep::offsetDays));
  }
}
