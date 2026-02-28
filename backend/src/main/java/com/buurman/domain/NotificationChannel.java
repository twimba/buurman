package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Delivery channel for notifications")
public enum NotificationChannel {
  EMAIL,
  SMS
}
