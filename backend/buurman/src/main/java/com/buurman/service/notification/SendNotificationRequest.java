package com.buurman.service.notification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;

public record SendNotificationRequest(
    Optional<UUID> teamId,
    NotificationType notificationType,
    Optional<UUID> recipientUserId,
    Optional<UUID> recipientTenantId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    String templateName,
    Map<String, Object> templateVariables,
    NotificationUrgency urgency,
    UUID createdBy) {

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private Optional<UUID> teamId = Optional.empty();
    private NotificationType notificationType;
    private Optional<UUID> recipientUserId = Optional.empty();
    private Optional<UUID> recipientTenantId = Optional.empty();
    private Optional<String> recipientEmail = Optional.empty();
    private Optional<String> recipientPhone = Optional.empty();
    private String templateName;
    private Map<String, Object> templateVariables;
    private NotificationUrgency urgency = NotificationUrgency.NORMAL;
    private UUID createdBy;

    public Builder teamId(UUID teamId) {
      this.teamId = Optional.of(teamId);
      return this;
    }

    public Builder teamId(Optional<UUID> teamId) {
      this.teamId = teamId;
      return this;
    }

    public Builder notificationType(NotificationType notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public Builder recipientUserId(UUID recipientUserId) {
      this.recipientUserId = Optional.of(recipientUserId);
      return this;
    }

    public Builder recipientUserId(Optional<UUID> recipientUserId) {
      this.recipientUserId = recipientUserId;
      return this;
    }

    public Builder recipientTenantId(UUID recipientTenantId) {
      this.recipientTenantId = Optional.of(recipientTenantId);
      return this;
    }

    public Builder recipientTenantId(Optional<UUID> recipientTenantId) {
      this.recipientTenantId = recipientTenantId;
      return this;
    }

    public Builder recipientEmail(String recipientEmail) {
      this.recipientEmail = Optional.of(recipientEmail);
      return this;
    }

    public Builder recipientEmail(Optional<String> recipientEmail) {
      this.recipientEmail = recipientEmail;
      return this;
    }

    public Builder recipientPhone(String recipientPhone) {
      this.recipientPhone = Optional.of(recipientPhone);
      return this;
    }

    public Builder recipientPhone(Optional<String> recipientPhone) {
      this.recipientPhone = recipientPhone;
      return this;
    }

    public Builder templateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder templateVariables(Map<String, Object> templateVariables) {
      this.templateVariables = templateVariables;
      return this;
    }

    public Builder urgency(NotificationUrgency urgency) {
      this.urgency = urgency;
      return this;
    }

    public Builder createdBy(UUID createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public SendNotificationRequest build() {
      return new SendNotificationRequest(
          teamId,
          notificationType,
          recipientUserId,
          recipientTenantId,
          recipientEmail,
          recipientPhone,
          templateName,
          templateVariables,
          urgency,
          createdBy);
    }
  }
}
