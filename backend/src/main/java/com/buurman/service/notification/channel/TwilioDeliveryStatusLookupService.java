package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.DELIVERED;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.SENT;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.repository.NotificationRepository;
import com.buurman.service.notification.DeliveryStatusLookupService;
import com.twilio.rest.api.v2010.account.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("!local")
@Slf4j
@RequiredArgsConstructor
public class TwilioDeliveryStatusLookupService implements DeliveryStatusLookupService {

  private final NotificationRepository notificationRepository;

  @Override
  public Notification refreshStatus(Notification notification) {
    if (notification.getProviderMessageId() == null
        || notification.getProviderMessageId().isBlank()) {
      log.debug(
          "No provider message ID for notification {}, skipping refresh",
          notification.getIdentifier());
      return notification;
    }

    if (notification.getChannel() == SMS) {
      return refreshTwilioStatus(notification);
    }

    log.debug(
        "Status refresh not available for channel {} (notification {})",
        notification.getChannel(),
        notification.getIdentifier());

    return notification;
  }

  private Notification refreshTwilioStatus(Notification notification) {
    try {
      Message message = Message.fetcher(notification.getProviderMessageId()).fetch();

      NotificationStatus newStatus = mapTwilioStatus(message.getStatus());
      String providerStatus = message.getStatus() != null ? message.getStatus().toString() : null;
      String providerError =
          message.getErrorCode() != null
              ? message.getErrorCode() + ": " + message.getErrorMessage()
              : null;

      notificationRepository.updateStatus(
          notification.getId(),
          newStatus,
          notification.getProviderMessageId(),
          providerStatus,
          providerError);

      notification.setStatus(newStatus);
      notification.setProviderStatus(providerStatus);
      notification.setProviderError(providerError);

      log.info(
          "Refreshed Twilio status for notification {}: {}",
          notification.getIdentifier(),
          providerStatus);

      return notification;
    } catch (Exception e) {
      log.warn(
          "Failed to refresh Twilio status for notification {}: {}",
          notification.getIdentifier(),
          e.getMessage());
      return notification;
    }
  }

  private NotificationStatus mapTwilioStatus(Message.Status status) {
    if (status == null) {
      return QUEUED;
    }

    return switch (status.toString().toUpperCase(Locale.ROOT)) {
      case "QUEUED", "SENDING", "RECEIVING", "ACCEPTED", "SCHEDULED" -> QUEUED;
      case "SENT" -> SENT;
      case "DELIVERED", "RECEIVED", "READ", "PARTIALLY_DELIVERED" -> DELIVERED;
      case "FAILED", "UNDELIVERED", "CANCELED" -> FAILED;
      default -> QUEUED;
    };
  }
}
