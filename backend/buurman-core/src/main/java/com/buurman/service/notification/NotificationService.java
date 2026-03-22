package com.buurman.service.notification;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Notification;
import com.buurman.domain.identifier.NotificationIdentifier;

public interface NotificationService {

  void send(SendNotificationRequest request);

  void sendToTeam(SendNotificationRequest request);

  Notification resend(
      @Nullable UUID teamId, NotificationIdentifier notificationIdentifier, @Nullable UUID userId);
}
