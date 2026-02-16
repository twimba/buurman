package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Notification {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private NotificationType notificationType;
  private String subject;
  private String body;
  private String recipientEmail;
  private String recipientPhone;
  private UUID recipientUserId;
  private UUID recipientTenantId;
  private NotificationChannel channel;
  private String contentTemplate;
  private Map<String, Object> contentVariables;
  private NotificationStatus status;
  private String providerMessageId;
  private String providerStatus;
  private String providerError;
  private Instant statusUpdatedAt;
  private UUID resentFromId;
  private String resendReason;
  private Instant createdAt;
  private UUID createdBy;
}
