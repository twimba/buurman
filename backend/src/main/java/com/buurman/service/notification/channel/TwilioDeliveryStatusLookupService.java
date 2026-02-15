package com.buurman.service.notification.channel;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.repository.NotificationRepository;

import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.DELIVERED;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.SENT;
import com.buurman.service.notification.DeliveryStatusLookupService;
import com.twilio.rest.api.v2010.account.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class TwilioDeliveryStatusLookupService implements DeliveryStatusLookupService {

    private static final Logger log = LoggerFactory.getLogger(TwilioDeliveryStatusLookupService.class);

    private final NotificationRepository notificationRepository;

    public TwilioDeliveryStatusLookupService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification refreshStatus(Notification notification) {
        if (notification.getProviderMessageId() == null || notification.getProviderMessageId().isBlank()) {
            log.debug("No provider message ID for notification {}, skipping refresh", notification.getIdentifier());
            return notification;
        }

        if (notification.getChannel() == SMS) {
            return refreshTwilioStatus(notification);
        }

        log.debug("Status refresh not available for channel {} (notification {})",
                notification.getChannel(), notification.getIdentifier());

        return notification;
    }

    private Notification refreshTwilioStatus(Notification notification) {
        try {
            Message message = Message.fetcher(notification.getProviderMessageId()).fetch();

            NotificationStatus newStatus = mapTwilioStatus(message.getStatus());
            String providerStatus = message.getStatus().toString();
            String providerError = message.getErrorCode() != null
                    ? message.getErrorCode() + ": " + message.getErrorMessage()
                    : null;

            notificationRepository.updateStatus(
                    notification.getId(), newStatus, notification.getProviderMessageId(),
                    providerStatus, providerError);

            notification.setStatus(newStatus);
            notification.setProviderStatus(providerStatus);
            notification.setProviderError(providerError);

            log.info("Refreshed Twilio status for notification {}: {}",
                    notification.getIdentifier(), providerStatus);

            return notification;
        } catch (Exception e) {
            log.warn("Failed to refresh Twilio status for notification {}: {}",
                    notification.getIdentifier(), e.getMessage());
            return notification;
        }
    }

    private NotificationStatus mapTwilioStatus(Message.Status status) {
        return switch (status) {
            case QUEUED -> QUEUED;
            case SENDING -> QUEUED;
            case SENT -> SENT;
            case FAILED -> DELIVERED;
            case DELIVERED -> DELIVERED;
            case UNDELIVERED -> FAILED;
            case RECEIVING -> QUEUED;
            case RECEIVED -> DELIVERED;
            case ACCEPTED -> QUEUED;
            case SCHEDULED -> QUEUED;
            case READ -> DELIVERED;
            case PARTIALLY_DELIVERED -> DELIVERED;
            case CANCELED -> FAILED;
        };
    }
}
