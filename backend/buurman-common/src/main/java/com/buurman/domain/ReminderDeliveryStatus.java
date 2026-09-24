package com.buurman.domain;

/** Delivery state of the email behind a payment reminder, derived from its notification. */
public enum ReminderDeliveryStatus {
  /** Accepted into the outbox, not handed to the provider yet. */
  QUEUED,
  /** Handed to the email provider. */
  SENT,
  /** Provider confirmed delivery to the tenant's mailbox. */
  DELIVERED,
  /** Provider refused it or the outbox gave up after its retries. */
  FAILED,
  /** Not sent by policy (e.g. demo team). */
  BLOCKED,
  /** No delivery information (legacy row or no email channel). */
  UNKNOWN;

  public static ReminderDeliveryStatus from(NotificationStatus status) {
    return switch (status) {
      case PENDING, QUEUED -> QUEUED;
      case SENT -> SENT;
      case DELIVERED -> DELIVERED;
      case FAILED, BOUNCED, REJECTED -> FAILED;
      case DEMO_BLOCKED -> BLOCKED;
    };
  }
}
