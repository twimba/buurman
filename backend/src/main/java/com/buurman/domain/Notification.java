package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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

    public Notification() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(UUID recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public UUID getRecipientTenantId() {
        return recipientTenantId;
    }

    public void setRecipientTenantId(UUID recipientTenantId) {
        this.recipientTenantId = recipientTenantId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public void setContentTemplate(String contentTemplate) {
        this.contentTemplate = contentTemplate;
    }

    public Map<String, Object> getContentVariables() {
        return contentVariables;
    }

    public void setContentVariables(Map<String, Object> contentVariables) {
        this.contentVariables = contentVariables;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public void setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
    }

    public String getProviderError() {
        return providerError;
    }

    public void setProviderError(String providerError) {
        this.providerError = providerError;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(Instant statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public UUID getResentFromId() {
        return resentFromId;
    }

    public void setResentFromId(UUID resentFromId) {
        this.resentFromId = resentFromId;
    }

    public String getResendReason() {
        return resendReason;
    }

    public void setResendReason(String resendReason) {
        this.resendReason = resendReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
