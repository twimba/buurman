package com.buurman.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReminderStep;
import com.buurman.domain.TeamPreferences;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamPreferencesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily automatic reminder ladder. For every team that opted in, walks its open payments and sends
 * the single most advanced ladder step that has been reached and not sent yet. Eligibility
 * (contract flag, tenant opt-in, pause, email) is enforced by {@link PaymentReminderService}; the
 * candidate query already excludes contracts that are disabled or paused so ineligible payments
 * never open a transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentDunningService {

  private final TeamPreferencesRepository teamPreferencesRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReminderRepository reminderRepository;
  private final PaymentReminderService reminderService;
  private final MetricsService metricsService;
  private final Clock clock;

  /** Incident kill switch: {@code scheduling.payment-dunning.enabled=false} skips the run. */
  @Value("${scheduling.payment-dunning.enabled:true}")
  private boolean enabled = true;

  public void runDailyLadder() {
    if (!enabled) {
      log.warn("Payment dunning ladder is disabled by configuration; skipping run");
      return;
    }
    Instant start = clock.instant();
    LocalDate today = LocalDate.now(clock);
    List<UUID> teamIds = teamPreferencesRepository.findTeamIdsWithAutomaticRemindersEnabled();
    log.info("Running payment dunning ladder for {} team(s)", teamIds.size());
    int sent = 0;
    int skipped = 0;
    int failed = 0;
    for (UUID teamId : teamIds) {
      try {
        int[] counts = runForTeam(teamId, today);
        sent += counts[0];
        skipped += counts[1];
        failed += counts[2];
      } catch (Exception e) {
        failed++;
        metricsService.incrementCounter("payment.dunning.team_failed.total");
        log.error("Dunning ladder failed for team {}: {}", teamId, e.getMessage(), e);
      }
    }
    metricsService.recordTimer(
        "payment.dunning.run.duration", Duration.between(start, clock.instant()));
    log.info(
        "Payment dunning ladder completed — {} sent, {} skipped, {} failed", sent, skipped, failed);
  }

  /** Returns {sent, skipped, failed} for one team. */
  private int[] runForTeam(UUID teamId, LocalDate today) {
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);
    List<PaymentReminderStep> steps =
        prefs.getPaymentReminderSteps().stream().filter(PaymentReminderStep::enabled).toList();
    if (steps.isEmpty()) {
      return new int[] {0, 0, 0};
    }
    int minOffset = steps.stream().mapToInt(PaymentReminderStep::offsetDays).min().orElse(0);
    List<Payment> candidates =
        paymentRepository.findDunningCandidates(teamId, today.minusDays(minOffset), today);
    Map<UUID, Set<Integer>> sentOffsets =
        reminderRepository.findAutomaticStepOffsetsByPaymentIds(
            candidates.stream().map(Payment::getId).toList(), teamId);
    int sent = 0;
    int skipped = 0;
    int failed = 0;
    for (Payment payment : candidates) {
      Optional<PaymentReminderStep> step =
          selectStep(
              steps,
              payment.getDueDate(),
              today,
              sentOffsets.getOrDefault(payment.getId(), Set.of()));
      if (step.isEmpty()) {
        continue;
      }
      String paymentSid = payment.getIdentifier().map(Object::toString).orElse("?");
      try {
        reminderService.sendAutomatic(payment, step.get());
        sent++;
      } catch (BusinessRuleException e) {
        // Opt-out, missing email, already settled — expected outcomes, not errors.
        skipped++;
        metricsService.incrementCounter("payment.reminder.skipped.total", "type", "AUTOMATIC");
        log.debug("Dunning step {} skipped for payment {}", step.get().offsetDays(), paymentSid);
      } catch (Exception e) {
        failed++;
        metricsService.incrementCounter("payment.reminder.failed.total", "type", "AUTOMATIC");
        log.error(
            "Dunning step {} failed for payment {} (team {}): {}",
            step.get().offsetDays(),
            paymentSid,
            teamId,
            e.getMessage(),
            e);
      }
    }
    return new int[] {sent, skipped, failed};
  }

  /**
   * The most advanced enabled step whose offset has been reached today, that has not been sent yet
   * and that lies beyond every step already sent. Returning only that one means enabling the ladder
   * on old arrears sends a single appropriately-toned reminder instead of the whole ladder, and a
   * payment never walks back down to a softer step on later days.
   */
  static Optional<PaymentReminderStep> selectStep(
      List<PaymentReminderStep> steps,
      LocalDate dueDate,
      LocalDate today,
      Set<Integer> sentOffsets) {
    int highestSent =
        sentOffsets.stream().mapToInt(Integer::intValue).max().orElse(Integer.MIN_VALUE);
    return steps.stream()
        .filter(PaymentReminderStep::enabled)
        .filter(s -> !today.isBefore(dueDate.plusDays(s.offsetDays())))
        .filter(s -> s.offsetDays() > highestSent)
        .max(Comparator.comparingInt(PaymentReminderStep::offsetDays));
  }
}
