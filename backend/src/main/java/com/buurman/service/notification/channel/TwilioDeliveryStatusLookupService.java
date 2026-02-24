package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.DELIVERED;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.SENT;
import static com.twilio.rest.api.v2010.account.Message.Status.ACCEPTED;
import static com.twilio.rest.api.v2010.account.Message.Status.CANCELED;
import static com.twilio.rest.api.v2010.account.Message.Status.PARTIALLY_DELIVERED;
import static com.twilio.rest.api.v2010.account.Message.Status.READ;
import static com.twilio.rest.api.v2010.account.Message.Status.RECEIVED;
import static com.twilio.rest.api.v2010.account.Message.Status.RECEIVING;
import static com.twilio.rest.api.v2010.account.Message.Status.SCHEDULED;
import static com.twilio.rest.api.v2010.account.Message.Status.SENDING;
import static com.twilio.rest.api.v2010.account.Message.Status.UNDELIVERED;

import java.util.Optional;

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
    if (notification.getProviderMessageId().map(String::isBlank).orElse(true)) {
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
      Message message = Message.fetcher(notification.getProviderMessageId().orElseThrow()).fetch();

      NotificationStatus newStatus = mapTwilioStatus(message.getStatus());
      String providerStatus = message.getStatus() != null ? message.getStatus().toString() : null;
      String providerError =
          message.getErrorCode() != null
              ? message.getErrorCode() + ": " + message.getErrorMessage()
              : null;

      notificationRepository.updateStatus(
          notification.getId(),
          newStatus,
          notification.getProviderMessageId().orElse(null),
          providerStatus,
          providerError);

      notification.setStatus(newStatus);
      notification.setProviderStatus(Optional.ofNullable(providerStatus));
      notification.setProviderError(Optional.ofNullable(providerError));

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

    return switch (status) {
      case QUEUED, SENDING, RECEIVING, ACCEPTED, SCHEDULED -> QUEUED;
      case SENT -> SENT;
      case DELIVERED, RECEIVED, READ, PARTIALLY_DELIVERED -> DELIVERED;
      case FAILED, UNDELIVERED, CANCELED -> FAILED;
    };
  }
}
