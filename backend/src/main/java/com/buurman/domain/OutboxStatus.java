package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Processing status of an outbox message")
public enum OutboxStatus {
  PENDING,
  PROCESSING,
  SENT,
  FAILED
}
