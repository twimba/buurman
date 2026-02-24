package com.buurman.service.notification;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.NotificationType;

public record SendNotificationRequest(
    Optional<UUID> teamId,
    NotificationType notificationType,
    Optional<UUID> recipientUserId,
    Optional<UUID> recipientTenantId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    String templateName,
    Map<String, Object> templateVariables,
    UUID createdBy) {
  public SendNotificationRequest {
    teamId = Objects.requireNonNullElse(teamId, Optional.empty());
    recipientUserId = Objects.requireNonNullElse(recipientUserId, Optional.empty());
    recipientTenantId = Objects.requireNonNullElse(recipientTenantId, Optional.empty());
    recipientEmail = Objects.requireNonNullElse(recipientEmail, Optional.empty());
    recipientPhone = Objects.requireNonNullElse(recipientPhone, Optional.empty());
  }

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private @Nullable UUID teamId;
    private NotificationType notificationType;
    private @Nullable UUID recipientUserId;
    private @Nullable UUID recipientTenantId;
    private @Nullable String recipientEmail;
    private @Nullable String recipientPhone;
    private String templateName;
    private Map<String, Object> templateVariables;
    private UUID createdBy;

    public Builder teamId(@Nullable UUID teamId) {
      this.teamId = teamId;
      return this;
    }

    public Builder notificationType(NotificationType notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public Builder recipientUserId(@Nullable UUID recipientUserId) {
      this.recipientUserId = recipientUserId;
      return this;
    }

    public Builder recipientTenantId(@Nullable UUID recipientTenantId) {
      this.recipientTenantId = recipientTenantId;
      return this;
    }

    public Builder recipientEmail(@Nullable String recipientEmail) {
      this.recipientEmail = recipientEmail;
      return this;
    }

    public Builder recipientPhone(@Nullable String recipientPhone) {
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

    public Builder createdBy(UUID createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public SendNotificationRequest build() {
      return new SendNotificationRequest(
          Optional.ofNullable(teamId),
          notificationType,
          Optional.ofNullable(recipientUserId),
          Optional.ofNullable(recipientTenantId),
          Optional.ofNullable(recipientEmail),
          Optional.ofNullable(recipientPhone),
          templateName,
          templateVariables,
          createdBy);
    }
  }
}
