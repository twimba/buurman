package com.buurman.service.notification;

import static com.buurman.domain.NotificationStatus.BOUNCED;
import static com.buurman.domain.NotificationStatus.DELIVERED;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.PENDING;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.REJECTED;
import static com.buurman.domain.NotificationStatus.SENT;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

  private static final Map<NotificationStatus, Integer> STATUS_RANK =
      Map.of(
          PENDING, 0,
          QUEUED, 1,
          SENT, 2,
          DELIVERED, 3,
          FAILED, 10,
          BOUNCED, 10,
          REJECTED, 10);

  private final NotificationRepository notificationRepository;
  private final ObjectMapper objectMapper;

  public void processMailgunEvents(String rawPayload) {
    try {
      JsonNode root = objectMapper.readTree(rawPayload);
      JsonNode eventData = root.path("event-data");

      String eventType = eventData.path("event").asText();
      String messageId = eventData.path("message").path("headers").path("message-id").asText();

      if (messageId.isEmpty() || eventType.isEmpty()) {
        log.warn("Mailgun webhook missing event type or message-id");
        return;
      }

      if ("opened".equals(eventType)) {
        notificationRepository.incrementOpenCount(messageId, NotificationChannel.EMAIL);
        log.debug("Mailgun open event for message {}", messageId);
        return;
      }
      if ("clicked".equals(eventType)) {
        notificationRepository.incrementClickCount(messageId, NotificationChannel.EMAIL);
        log.debug("Mailgun click event for message {}", messageId);
        return;
      }

      String severity = eventData.path("severity").asText(null);
      NotificationStatus status = mapMailgunStatus(eventType, severity);
      String reason = eventData.path("reason").asText(null);

      if (shouldUpdateStatus(messageId, status, NotificationChannel.EMAIL)) {
        notificationRepository.updateStatusByProviderMessageId(
            messageId, NotificationChannel.EMAIL, status, eventType, reason);
        log.debug("Mailgun event: {} -> {} for message {}", eventType, status, messageId);
      } else {
        log.debug(
            "Mailgun event skipped (status regression): {} for message {}", eventType, messageId);
      }
    } catch (Exception e) {
      log.error("Failed to parse Mailgun event: {}", e.getMessage(), e);
    }
  }

  public void processTwilioStatus(Map<String, String> params) {
    String messageSid = params.get("MessageSid");
    String messageStatus = params.get("MessageStatus");

    if (messageSid == null || messageStatus == null) {
      log.warn("Twilio webhook missing MessageSid or MessageStatus");
      return;
    }

    NotificationStatus status = mapTwilioStatus(messageStatus);
    String errorCode = params.get("ErrorCode");
    String errorMessage = params.get("ErrorMessage");

    String providerError = errorCode != null ? errorCode + ": " + errorMessage : null;

    if (shouldUpdateStatus(messageSid, status, NotificationChannel.SMS)) {
      notificationRepository.updateStatusByProviderMessageId(
          messageSid, NotificationChannel.SMS, status, messageStatus, providerError);
      log.debug("Twilio status: {} -> {} for SID {}", messageStatus, status, messageSid);
    } else {
      log.debug(
          "Twilio status skipped (status regression): {} for SID {}", messageStatus, messageSid);
    }
  }

  private boolean shouldUpdateStatus(
      String providerMessageId, NotificationStatus newStatus, NotificationChannel channel) {
    return notificationRepository
        .findByProviderMessageId(providerMessageId, channel)
        .map(
            notification -> {
              int currentRank = STATUS_RANK.getOrDefault(notification.getStatus(), 0);
              int newRank = STATUS_RANK.getOrDefault(newStatus, 0);
              return newRank > currentRank;
            })
        .orElse(true);
  }

  private NotificationStatus mapMailgunStatus(
      String eventType, @org.jspecify.annotations.Nullable String severity) {
    return switch (eventType) {
      case "accepted" -> QUEUED;
      case "delivered" -> DELIVERED;
      case "failed" -> "permanent".equals(severity) ? BOUNCED : QUEUED;
      case "rejected" -> REJECTED;
      case "complained", "unsubscribed" -> FAILED;
      default -> SENT;
    };
  }

  private NotificationStatus mapTwilioStatus(String status) {
    return switch (status) {
      case "queued", "accepted" -> QUEUED;
      case "sending", "sent" -> SENT;
      case "delivered" -> DELIVERED;
      case "failed", "undelivered" -> FAILED;
      default -> SENT;
    };
  }
}
