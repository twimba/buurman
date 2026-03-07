package com.buurman.service.notification;

import static com.buurman.domain.NotificationStatus.BOUNCED;
import static com.buurman.domain.NotificationStatus.DELIVERED;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.PENDING;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.REJECTED;
import static com.buurman.domain.NotificationStatus.SENT;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.buurman.domain.NotificationStatus;
import com.buurman.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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

  public void processSendGridEvents(String rawPayload) {
    try {
      List<Map<String, Object>> events =
          objectMapper.readValue(rawPayload, new TypeReference<>() {});

      for (Map<String, Object> event : events) {
        String sgMessageId = (String) event.get("sg_message_id");
        String eventType = (String) event.get("event");

        if (sgMessageId == null || eventType == null) {
          continue;
        }

        // SendGrid message IDs may have a filter suffix
        String messageId =
            sgMessageId.contains(".")
                ? sgMessageId.substring(0, sgMessageId.indexOf("."))
                : sgMessageId;

        // Handle engagement events (open/click) separately from delivery status
        if ("open".equals(eventType)) {
          notificationRepository.incrementOpenCount(messageId);
          log.debug("SendGrid open event for message {}", messageId);
          continue;
        }
        if ("click".equals(eventType)) {
          notificationRepository.incrementClickCount(messageId);
          log.debug("SendGrid click event for message {}", messageId);
          continue;
        }

        NotificationStatus status = mapSendGridStatus(eventType);
        String reason = (String) event.get("reason");

        if (shouldUpdateStatus(messageId, status)) {
          notificationRepository.updateStatusByProviderMessageId(
              messageId, status, eventType, reason);
          log.debug("SendGrid event: {} -> {} for message {}", eventType, status, messageId);
        } else {
          log.debug(
              "SendGrid event skipped (status regression): {} for message {}",
              eventType,
              messageId);
        }
      }
    } catch (Exception e) {
      log.error("Failed to parse SendGrid events: {}", e.getMessage(), e);
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

    if (shouldUpdateStatus(messageSid, status)) {
      notificationRepository.updateStatusByProviderMessageId(
          messageSid, status, messageStatus, providerError);
      log.debug("Twilio status: {} -> {} for SID {}", messageStatus, status, messageSid);
    } else {
      log.debug(
          "Twilio status skipped (status regression): {} for SID {}", messageStatus, messageSid);
    }
  }

  private boolean shouldUpdateStatus(String providerMessageId, NotificationStatus newStatus) {
    return notificationRepository
        .findByProviderMessageId(providerMessageId)
        .map(
            notification -> {
              int currentRank = STATUS_RANK.getOrDefault(notification.getStatus(), 0);
              int newRank = STATUS_RANK.getOrDefault(newStatus, 0);
              return newRank > currentRank;
            })
        .orElse(true); // If notification not found, allow the update (it may arrive before our DB
    // write)
  }

  private NotificationStatus mapSendGridStatus(String eventType) {
    return switch (eventType) {
      case "processed" -> QUEUED;
      case "delivered" -> DELIVERED;
      case "bounce", "blocked" -> BOUNCED;
      case "dropped" -> REJECTED;
      case "deferred" -> QUEUED;
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
