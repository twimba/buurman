package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PAYMENT_REMINDERS;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.max;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.ReminderTone;
import com.buurman.jooq.generated.tables.records.PaymentRemindersRecord;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentReminderRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PaymentReminder> findByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
    return dsl.selectFrom(PAYMENT_REMINDERS)
        .where(
            PAYMENT_REMINDERS
                .PAYMENT_ID
                .eq(paymentId)
                .and(PAYMENT_REMINDERS.TEAM_ID.eq(teamId))
                .and(PAYMENT_REMINDERS.DELETED_AT.isNull()))
        .orderBy(PAYMENT_REMINDERS.SENT_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  /** Latest send time and total count per payment, for "last reminded" columns. */
  public Map<UUID, ReminderSummary> summarizeByPaymentIds(
      Collection<UUID> paymentIds, UUID teamId) {
    if (paymentIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, ReminderSummary> result = new HashMap<>();
    dsl.select(
            PAYMENT_REMINDERS.PAYMENT_ID,
            max(PAYMENT_REMINDERS.SENT_AT).as("last_sent_at"),
            count().as("reminder_count"))
        .from(PAYMENT_REMINDERS)
        .where(
            PAYMENT_REMINDERS
                .PAYMENT_ID
                .in(paymentIds)
                .and(PAYMENT_REMINDERS.TEAM_ID.eq(teamId))
                .and(PAYMENT_REMINDERS.DELETED_AT.isNull()))
        .groupBy(PAYMENT_REMINDERS.PAYMENT_ID)
        .fetch()
        .forEach(
            r ->
                result.put(
                    r.get(PAYMENT_REMINDERS.PAYMENT_ID),
                    new ReminderSummary(
                        Optional.ofNullable(r.get("last_sent_at", LocalDateTime.class))
                            .map(ldt -> ldt.toInstant(UTC)),
                        Optional.ofNullable(r.get("reminder_count", Integer.class)).orElse(0))));
    return result;
  }

  public record ReminderSummary(Optional<Instant> lastSentAt, int count) {}

  /** Ladder step offsets already sent automatically for a payment, for idempotent scheduling. */
  public Set<Integer> findAutomaticStepOffsets(UUID paymentId, UUID teamId) {
    return new HashSet<>(
        dsl.select(PAYMENT_REMINDERS.STEP_OFFSET_DAYS)
            .from(PAYMENT_REMINDERS)
            .where(
                PAYMENT_REMINDERS
                    .PAYMENT_ID
                    .eq(paymentId)
                    .and(PAYMENT_REMINDERS.TEAM_ID.eq(teamId))
                    .and(
                        PAYMENT_REMINDERS.REMINDER_TYPE.eq(
                            PaymentReminder.ReminderType.AUTOMATIC.name()))
                    .and(PAYMENT_REMINDERS.STEP_OFFSET_DAYS.isNotNull())
                    .and(PAYMENT_REMINDERS.DELETED_AT.isNull()))
            .fetch(PAYMENT_REMINDERS.STEP_OFFSET_DAYS));
  }

  /** {@link #findAutomaticStepOffsets} for many payments in one query. */
  public Map<UUID, Set<Integer>> findAutomaticStepOffsetsByPaymentIds(
      Collection<UUID> paymentIds, UUID teamId) {
    if (paymentIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Set<Integer>> out = new HashMap<>();
    dsl.select(PAYMENT_REMINDERS.PAYMENT_ID, PAYMENT_REMINDERS.STEP_OFFSET_DAYS)
        .from(PAYMENT_REMINDERS)
        .where(
            PAYMENT_REMINDERS
                .PAYMENT_ID
                .in(paymentIds)
                .and(PAYMENT_REMINDERS.TEAM_ID.eq(teamId))
                .and(
                    PAYMENT_REMINDERS.REMINDER_TYPE.eq(
                        PaymentReminder.ReminderType.AUTOMATIC.name()))
                .and(PAYMENT_REMINDERS.STEP_OFFSET_DAYS.isNotNull())
                .and(PAYMENT_REMINDERS.DELETED_AT.isNull()))
        .forEach(
            r ->
                out.computeIfAbsent(r.value1(), k -> new HashSet<>())
                    .add(requireNonNull(r.value2())));
    return out;
  }

  public PaymentReminder save(PaymentReminder reminder) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    LocalDateTime sentAt = LocalDateTime.ofInstant(reminder.getSentAt(), UTC);

    dsl.insertInto(PAYMENT_REMINDERS)
        .set(PAYMENT_REMINDERS.ID, id)
        .set(PAYMENT_REMINDERS.IDENTIFIER, reminder.getIdentifier().orElseThrow())
        .set(PAYMENT_REMINDERS.TEAM_ID, reminder.getTeamId())
        .set(PAYMENT_REMINDERS.PAYMENT_ID, reminder.getPaymentId())
        .set(PAYMENT_REMINDERS.CONTACT_ID, reminder.getContactId().orElse(null))
        .set(PAYMENT_REMINDERS.REMINDER_TYPE, reminder.getReminderType().name())
        .set(PAYMENT_REMINDERS.CHANNEL, reminder.getChannel().name())
        .set(PAYMENT_REMINDERS.RECIPIENT_EMAIL, reminder.getRecipientEmail().orElse(null))
        .set(PAYMENT_REMINDERS.DAYS_OVERDUE, reminder.getDaysOverdue())
        .set(PAYMENT_REMINDERS.OUTSTANDING_AMOUNT, reminder.getOutstandingAmount().toMinorUnits())
        .set(PAYMENT_REMINDERS.CURRENCY, reminder.getOutstandingAmount().currency())
        .set(PAYMENT_REMINDERS.NOTES, reminder.getNotes().orElse(null))
        .set(PAYMENT_REMINDERS.STEP_OFFSET_DAYS, reminder.getStepOffsetDays().orElse(null))
        .set(PAYMENT_REMINDERS.TONE, reminder.getTone().map(Enum::name).orElse(null))
        .set(PAYMENT_REMINDERS.SENT_AT, sentAt)
        .set(PAYMENT_REMINDERS.CREATED_AT, now)
        .set(PAYMENT_REMINDERS.UPDATED_AT, now)
        .set(PAYMENT_REMINDERS.CREATED_BY, reminder.getCreatedBy())
        .set(PAYMENT_REMINDERS.UPDATED_BY, reminder.getUpdatedBy())
        .execute();

    reminder.setId(id);
    reminder.setCreatedAt(now.toInstant(UTC));
    reminder.setUpdatedAt(now.toInstant(UTC));
    return reminder;
  }

  private PaymentReminder toDomain(PaymentRemindersRecord record) {
    String currency = record.getCurrency();
    Long minorUnits = record.getOutstandingAmount();
    int digits = CurrencyUtils.getFractionalDigits(currency);
    BigDecimal major =
        minorUnits != null ? BigDecimal.valueOf(minorUnits, digits) : BigDecimal.ZERO;

    PaymentReminder reminder = new PaymentReminder();
    reminder.setId(record.getId());
    reminder.setIdentifier(Optional.of(record.getIdentifier()));
    reminder.setTeamId(record.getTeamId());
    reminder.setPaymentId(record.getPaymentId());
    reminder.setContactId(Optional.ofNullable(record.getContactId()));
    reminder.setReminderType(PaymentReminder.ReminderType.valueOf(record.getReminderType()));
    reminder.setChannel(NotificationChannel.valueOf(record.getChannel()));
    reminder.setRecipientEmail(Optional.ofNullable(record.getRecipientEmail()));
    reminder.setDaysOverdue(Optional.ofNullable(record.getDaysOverdue()).orElse(0));
    reminder.setOutstandingAmount(MoneyAmount.of(major, currency));
    reminder.setNotes(Optional.ofNullable(record.getNotes()));
    reminder.setStepOffsetDays(Optional.ofNullable(record.getStepOffsetDays()));
    reminder.setTone(Optional.ofNullable(record.getTone()).map(ReminderTone::valueOf));
    reminder.setSentAt(record.getSentAt().toInstant(UTC));
    reminder.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    reminder.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    reminder.setCreatedBy(record.getCreatedBy());
    reminder.setUpdatedBy(record.getUpdatedBy());
    reminder.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return reminder;
  }
}
