package com.buurman.service.notification;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.NotificationChannel;

public record RenderedContent(Optional<String> subject, String body, NotificationChannel channel) {
  public RenderedContent {
    subject = Objects.requireNonNullElse(subject, Optional.empty());
  }
}
