package com.buurman.service.notification;

import com.buurman.domain.NotificationStatus;
import com.buurman.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    public void processSendGridEvents(String rawPayload) {
        try {
            List<Map<String, Object>> events = objectMapper.readValue(
                    rawPayload, new TypeReference<>() {});

            for (Map<String, Object> event : events) {
                String sgMessageId = (String) event.get("sg_message_id");
                String eventType = (String) event.get("event");

                if (sgMessageId == null || eventType == null) continue;

                // SendGrid message IDs may have a filter suffix
                String messageId = sgMessageId.contains(".")
                        ? sgMessageId.substring(0, sgMessageId.indexOf("."))
                        : sgMessageId;

                NotificationStatus status = mapSendGridStatus(eventType);
                String reason = (String) event.get("reason");

                notificationRepository.updateStatusByProviderMessageId(
                        messageId, status, eventType, reason);

                log.debug("SendGrid event: {} -> {} for message {}", eventType, status, messageId);
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

        notificationRepository.updateStatusByProviderMessageId(
                messageSid, status, messageStatus, providerError);

        log.debug("Twilio status: {} -> {} for SID {}", messageStatus, status, messageSid);
    }

    private NotificationStatus mapSendGridStatus(String eventType) {
        return switch (eventType) {
            case "processed" -> NotificationStatus.QUEUED;
            case "delivered" -> NotificationStatus.DELIVERED;
            case "bounce", "blocked" -> NotificationStatus.BOUNCED;
            case "dropped" -> NotificationStatus.REJECTED;
            case "deferred" -> NotificationStatus.QUEUED;
            default -> NotificationStatus.SENT;
        };
    }

    private NotificationStatus mapTwilioStatus(String status) {
        return switch (status) {
            case "queued", "accepted" -> NotificationStatus.QUEUED;
            case "sending", "sent" -> NotificationStatus.SENT;
            case "delivered" -> NotificationStatus.DELIVERED;
            case "failed", "undelivered" -> NotificationStatus.FAILED;
            default -> NotificationStatus.SENT;
        };
    }
}
