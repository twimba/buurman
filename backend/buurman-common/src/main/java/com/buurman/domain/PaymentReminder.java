package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reminder sent to a tenant about an outstanding payment. Snapshots the balance and days overdue
 * at send time so the per-payment communications timeline stays accurate after the payment itself
 * changes.
 */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReminder {

  public enum ReminderType {
    MANUAL,
    AUTOMATIC
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID paymentId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  private ReminderType reminderType;
  private NotificationChannel channel;
  @Builder.Default private Optional<String> recipientEmail = Optional.empty();
  @Builder.Default private int daysOverdue = 0;
  private MoneyAmount outstandingAmount;
  @Builder.Default private Optional<String> notes = Optional.empty();

  /** Ladder step (days relative to due date) that produced an AUTOMATIC reminder. */
  @Builder.Default private Optional<Integer> stepOffsetDays = Optional.empty();

  @Builder.Default private Optional<ReminderTone> tone = Optional.empty();
  private Instant sentAt;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
