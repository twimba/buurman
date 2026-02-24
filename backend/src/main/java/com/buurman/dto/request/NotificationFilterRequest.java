package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

public record NotificationFilterRequest(
    Optional<String> type,
    Optional<String> channel,
    Optional<String> status,
    Optional<String> recipientEmail,
    Optional<String> dateFrom,
    Optional<String> dateTo) {
  public NotificationFilterRequest {
    type = Objects.requireNonNullElse(type, Optional.empty());
    channel = Objects.requireNonNullElse(channel, Optional.empty());
    status = Objects.requireNonNullElse(status, Optional.empty());
    recipientEmail = Objects.requireNonNullElse(recipientEmail, Optional.empty());
    dateFrom = Objects.requireNonNullElse(dateFrom, Optional.empty());
    dateTo = Objects.requireNonNullElse(dateTo, Optional.empty());
  }
}
