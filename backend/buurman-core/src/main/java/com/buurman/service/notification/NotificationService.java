package com.buurman.service.notification;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.identifier.NotificationIdentifier;

public interface NotificationService {

  /**
   * Renders and queues the notification on every channel that has a sender; returns the created
   * notification rows (one per channel; blocked ones included) so callers can track delivery.
   */
  List<Notification> send(SendNotificationRequest request);

  /** Current delivery state of the given notifications, keyed by notification id. */
  Map<UUID, DeliveryState> deliveryStates(Collection<UUID> notificationIds, UUID teamId);

  record DeliveryState(NotificationStatus status, Optional<String> error) {}

  void sendToTeam(SendNotificationRequest request);

  Notification resend(
      @Nullable UUID teamId, NotificationIdentifier notificationIdentifier, @Nullable UUID userId);
}
