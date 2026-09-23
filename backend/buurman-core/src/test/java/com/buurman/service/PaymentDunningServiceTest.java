package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.PaymentReminderStep;
import com.buurman.domain.ReminderTone;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamPreferences;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
class PaymentDunningServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);
  private static final PaymentReminderStep BEFORE =
      new PaymentReminderStep(-3, ReminderTone.FRIENDLY, true);
  private static final PaymentReminderStep DUE =
      new PaymentReminderStep(0, ReminderTone.FRIENDLY, true);
  private static final PaymentReminderStep WEEK =
      new PaymentReminderStep(7, ReminderTone.FIRM, true);
  private static final PaymentReminderStep FINAL =
      new PaymentReminderStep(21, ReminderTone.FINAL, true);
  private static final PaymentReminderStep DISABLED =
      new PaymentReminderStep(14, ReminderTone.FIRM, false);

  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReminderRepository reminderRepository;
  @Mock private PaymentReminderService reminderService;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private PaymentDunningService service;

  @BeforeEach
  void setUp() {
    service =
        new PaymentDunningService(
            teamPreferencesRepository,
            paymentRepository,
            reminderRepository,
            reminderService,
            clock);
  }

  @Nested
  @DisplayName("selectStep")
  class SelectStep {

    @Test
    @DisplayName("picks the latest reached step that was not sent yet")
    void latestReachedUnsent() {
      List<PaymentReminderStep> steps = List.of(BEFORE, DUE, WEEK, FINAL);
      LocalDate due = TODAY.minusDays(10); // day 10: -3, 0, 7 reached; 21 not

      assertThat(PaymentDunningService.selectStep(steps, due, TODAY, Set.of())).contains(WEEK);
      assertThat(PaymentDunningService.selectStep(steps, due, TODAY, Set.of(7))).contains(DUE);
      assertThat(PaymentDunningService.selectStep(steps, due, TODAY, Set.of(7, 0, -3))).isEmpty();
    }

    @Test
    @DisplayName("pre-due steps fire before the due date and nothing fires too early")
    void preDueSteps() {
      List<PaymentReminderStep> steps = List.of(BEFORE, DUE);
      assertThat(PaymentDunningService.selectStep(steps, TODAY.plusDays(3), TODAY, Set.of()))
          .contains(BEFORE);
      assertThat(PaymentDunningService.selectStep(steps, TODAY.plusDays(4), TODAY, Set.of()))
          .isEmpty();
    }

    @Test
    @DisplayName("disabled steps are never selected")
    void ignoresDisabled() {
      List<PaymentReminderStep> steps = List.of(DUE, DISABLED);
      assertThat(PaymentDunningService.selectStep(steps, TODAY.minusDays(30), TODAY, Set.of()))
          .contains(DUE);
    }
  }

  @Nested
  @DisplayName("runDailyLadder")
  class RunDailyLadder {

    private Payment payment(UUID teamId, LocalDate due) {
      return Payment.builder()
          .id(UUID.randomUUID())
          .identifier(Optional.of(Sid.of("pay_01JTEST000000000000000001")))
          .teamId(teamId)
          .contractId(UUID.randomUUID())
          .amount(MoneyAmount.of(new BigDecimal("900.00"), "EUR"))
          .dueDate(due)
          .status(PaymentStatus.OVERDUE)
          .build();
    }

    @Test
    @DisplayName("sends one step per payment and swallows opt-out rejections")
    void sendsAndSkips() {
      UUID teamId = UUID.randomUUID();
      when(teamPreferencesRepository.findTeamIdsWithAutomaticRemindersEnabled())
          .thenReturn(List.of(teamId));
      TeamPreferences prefs = new TeamPreferences();
      prefs.setTeamId(teamId);
      prefs.setAutomaticRemindersEnabled(true);
      prefs.setPaymentReminderSteps(List.of(DUE, WEEK));
      when(teamPreferencesRepository.getByTeamId(teamId)).thenReturn(prefs);

      Payment eligible = payment(teamId, TODAY.minusDays(8));
      Payment optedOut = payment(teamId, TODAY.minusDays(8));
      Payment tooEarly = payment(teamId, TODAY.plusDays(2));
      when(paymentRepository.findOpenPaymentsDueOnOrBefore(eq(teamId), any()))
          .thenReturn(List.of(eligible, optedOut, tooEarly));
      when(reminderRepository.findAutomaticStepOffsets(any(), eq(teamId))).thenReturn(Set.of());
      when(reminderService.sendAutomatic(eq(optedOut), any()))
          .thenThrow(new BusinessRuleException("Tenant reminders are not enabled"));

      service.runDailyLadder();

      verify(reminderService).sendAutomatic(eligible, WEEK);
      verify(reminderService).sendAutomatic(eq(optedOut), any());
      verify(reminderService, never()).sendAutomatic(eq(tooEarly), any());
      verify(reminderService, times(2)).sendAutomatic(any(), any());
    }

    @Test
    @DisplayName("teams without enabled steps are skipped entirely")
    void noStepsNoQueries() {
      UUID teamId = UUID.randomUUID();
      when(teamPreferencesRepository.findTeamIdsWithAutomaticRemindersEnabled())
          .thenReturn(List.of(teamId));
      TeamPreferences prefs = new TeamPreferences();
      prefs.setTeamId(teamId);
      prefs.setPaymentReminderSteps(List.of(DISABLED));
      when(teamPreferencesRepository.getByTeamId(teamId)).thenReturn(prefs);

      service.runDailyLadder();

      verify(paymentRepository, never()).findOpenPaymentsDueOnOrBefore(any(), any());
    }
  }
}
