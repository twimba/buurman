package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.PaymentReminder.ReminderType;
import com.buurman.mapper.PaymentRecordMapper;
import com.buurman.repository.PaymentReminderRepository.ReminderSummary;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

class PaymentReminderRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private PaymentRepository paymentRepo;
  private PaymentReminderRepository repo;
  private Payment paymentA;
  private Payment paymentB;

  @BeforeEach
  void setUp() {
    paymentRepo = new PaymentRepository(dsl, new PaymentRecordMapper(), CLOCK);
    repo = new PaymentReminderRepository(dsl, CLOCK);

    UUID propertyA = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID contractA = TestDataHelper.insertContract(dsl, TEAM_A_ID, propertyA, USER_ID);
    paymentA =
        paymentRepo.save(
            TestDataHelper.buildPayment(
                TEAM_A_ID,
                contractA,
                USER_ID,
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 2, 1)));

    UUID propertyB = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    UUID contractB = TestDataHelper.insertContract(dsl, TEAM_B_ID, propertyB, USER_ID);
    paymentB =
        paymentRepo.save(
            TestDataHelper.buildPayment(
                TEAM_B_ID, contractB, USER_ID, new BigDecimal("500.00"), LocalDate.of(2026, 2, 1)));
  }

  private PaymentReminder reminder(Payment payment, UUID teamId, Instant sentAt, int daysOverdue) {
    return PaymentReminder.builder()
        .identifier(Optional.of(SidGenerator.newPaymentReminderId()))
        .teamId(teamId)
        .paymentId(payment.getId())
        .contactId(Optional.empty())
        .reminderType(ReminderType.MANUAL)
        .channel(NotificationChannel.EMAIL)
        .recipientEmail(Optional.of("tenant@example.com"))
        .daysOverdue(daysOverdue)
        .outstandingAmount(MoneyAmount.of(new BigDecimal("750.00"), "EUR"))
        .notes(Optional.of("first notice"))
        .sentAt(sentAt)
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName("save then find by payment returns the reminder with money and dates intact")
  void saveAndFind() {
    Instant sentAt = Instant.parse("2026-02-20T10:15:00Z");
    repo.save(reminder(paymentA, TEAM_A_ID, sentAt, 19));

    List<PaymentReminder> found = repo.findByPaymentIdAndTeamId(paymentA.getId(), TEAM_A_ID);

    assertThat(found).hasSize(1);
    PaymentReminder r = found.getFirst();
    assertThat(r.getId()).isNotNull();
    assertThat(r.getIdentifier()).isPresent();
    assertThat(r.getReminderType()).isEqualTo(ReminderType.MANUAL);
    assertThat(r.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    assertThat(r.getRecipientEmail()).contains("tenant@example.com");
    assertThat(r.getDaysOverdue()).isEqualTo(19);
    assertThat(r.getOutstandingAmount().value()).isEqualByComparingTo("750.00");
    assertThat(r.getOutstandingAmount().currency()).isEqualTo("EUR");
    assertThat(r.getNotes()).contains("first notice");
    assertThat(r.getSentAt()).isEqualTo(sentAt);
    assertThat(r.getCreatedBy()).isEqualTo(USER_ID);
  }

  @Test
  @DisplayName("a ladder step whose email failed does not count as sent")
  void failedDeliveryIsRetried() {
    UUID okNotification = insertNotification(TEAM_A_ID, "SENT");
    UUID failedNotification = insertNotification(TEAM_A_ID, "FAILED");
    repo.save(automatic(paymentA, TEAM_A_ID, 0, okNotification));
    repo.save(automatic(paymentA, TEAM_A_ID, 7, failedNotification));
    repo.save(automatic(paymentA, TEAM_A_ID, 14, null));

    assertThat(repo.findAutomaticStepOffsets(paymentA.getId(), TEAM_A_ID))
        .containsExactlyInAnyOrder(0, 14);
    assertThat(
            repo.findAutomaticStepOffsetsByPaymentIds(List.of(paymentA.getId()), TEAM_A_ID)
                .get(paymentA.getId()))
        .containsExactlyInAnyOrder(0, 14);
  }

  private PaymentReminder automatic(
      Payment payment, UUID teamId, int offset, @Nullable UUID notificationId) {
    PaymentReminder r = reminder(payment, teamId, Instant.parse("2026-02-20T10:00:00Z"), offset);
    r.setReminderType(ReminderType.AUTOMATIC);
    r.setStepOffsetDays(Optional.of(offset));
    r.setNotificationId(Optional.ofNullable(notificationId));
    return r;
  }

  private UUID insertNotification(UUID teamId, String status) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("notifications"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newNotificationId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("notification_type", String.class), "PAYMENT_REMINDER")
        .set(DSL.field("channel", String.class), "EMAIL")
        .set(DSL.field("content_template", String.class), "payment-reminder-tenant")
        .set(DSL.field("body", String.class), "test")
        .set(DSL.field("status", String.class), status)
        .execute();
    return id;
  }

  @Test
  @DisplayName("reminders are ordered newest first")
  void newestFirst() {
    repo.save(reminder(paymentA, TEAM_A_ID, Instant.parse("2026-02-10T10:00:00Z"), 9));
    repo.save(reminder(paymentA, TEAM_A_ID, Instant.parse("2026-02-24T10:00:00Z"), 23));

    List<PaymentReminder> found = repo.findByPaymentIdAndTeamId(paymentA.getId(), TEAM_A_ID);

    assertThat(found).extracting(PaymentReminder::getDaysOverdue).containsExactly(23, 9);
  }

  @Test
  @DisplayName("team A cannot see team B's reminders even with the right payment id")
  void teamIsolation() {
    repo.save(reminder(paymentB, TEAM_B_ID, Instant.parse("2026-02-20T10:00:00Z"), 19));

    assertThat(repo.findByPaymentIdAndTeamId(paymentB.getId(), TEAM_A_ID)).isEmpty();
    assertThat(repo.findByPaymentIdAndTeamId(paymentB.getId(), TEAM_B_ID)).hasSize(1);
    assertThat(repo.summarizeByPaymentIds(List.of(paymentB.getId()), TEAM_A_ID)).isEmpty();
  }

  @Test
  @DisplayName("summarize reports latest send and count per payment")
  void summarize() {
    repo.save(reminder(paymentA, TEAM_A_ID, Instant.parse("2026-02-10T10:00:00Z"), 9));
    repo.save(reminder(paymentA, TEAM_A_ID, Instant.parse("2026-02-24T10:00:00Z"), 23));

    Map<UUID, ReminderSummary> summary =
        repo.summarizeByPaymentIds(List.of(paymentA.getId(), UUID.randomUUID()), TEAM_A_ID);

    assertThat(summary).containsOnlyKeys(paymentA.getId());
    ReminderSummary s = Optional.ofNullable(summary.get(paymentA.getId())).orElseThrow();
    assertThat(s.count()).isEqualTo(2);
    assertThat(s.lastSentAt()).contains(Instant.parse("2026-02-24T10:00:00Z"));
  }
}
