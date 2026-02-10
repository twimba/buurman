package com.buurman.service.notification;

import com.buurman.domain.NotificationType;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
        UUID teamId,
        NotificationType notificationType,
        UUID recipientUserId,
        UUID recipientTenantId,
        String recipientEmail,
        String recipientPhone,
        String templateName,
        Map<String, Object> templateVariables,
        UUID createdBy
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID teamId;
        private NotificationType notificationType;
        private UUID recipientUserId;
        private UUID recipientTenantId;
        private String recipientEmail;
        private String recipientPhone;
        private String templateName;
        private Map<String, Object> templateVariables;
        private UUID createdBy;

        public Builder teamId(UUID teamId) { this.teamId = teamId; return this; }
        public Builder notificationType(NotificationType notificationType) { this.notificationType = notificationType; return this; }
        public Builder recipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; return this; }
        public Builder recipientTenantId(UUID recipientTenantId) { this.recipientTenantId = recipientTenantId; return this; }
        public Builder recipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; return this; }
        public Builder recipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder templateVariables(Map<String, Object> templateVariables) { this.templateVariables = templateVariables; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }

        public SendNotificationRequest build() {
            return new SendNotificationRequest(teamId, notificationType, recipientUserId,
                    recipientTenantId, recipientEmail, recipientPhone, templateName,
                    templateVariables, createdBy);
        }
    }
}
