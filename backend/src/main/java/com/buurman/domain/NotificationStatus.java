package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Delivery status of a notification")
public enum NotificationStatus {
  PENDING,
  QUEUED,
  SENT,
  DELIVERED,
  FAILED,
  BOUNCED,
  REJECTED,
  DEMO_BLOCKED
}
