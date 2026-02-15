package com.buurman.mapper;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.jooq.generated.tables.records.NotificationsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

@Component
public class NotificationRecordMapper {

    private final ObjectMapper objectMapper;

    public NotificationRecordMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Notification toDomain(NotificationsRecord record) {
        if (record == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setId(record.getId());
        notification.setIdentifier(record.getIdentifier());
        notification.setTeamId(record.getTeamId());
        notification.setNotificationType(NotificationType.valueOf(record.getNotificationType()));
        notification.setSubject(record.getSubject());
        notification.setBody(record.getBody());
        notification.setRecipientEmail(record.getRecipientEmail());
        notification.setRecipientPhone(record.getRecipientPhone());
        notification.setRecipientUserId(record.getRecipientUserId());
        notification.setRecipientTenantId(record.getRecipientTenantId());
        notification.setChannel(NotificationChannel.valueOf(record.getChannel()));
        notification.setContentTemplate(record.getContentTemplate());

        if (record.getContentVariables() != null) {
            try {
                Map<String, Object> variables = objectMapper.readValue(
                        record.getContentVariables().data(),
                        new TypeReference<>() {});
                notification.setContentVariables(variables);
            } catch (Exception e) {
                // Log but don't fail - content variables are informational
            }
        }

        notification.setStatus(NotificationStatus.valueOf(record.getStatus()));
        notification.setProviderMessageId(record.getProviderMessageId());
        notification.setProviderStatus(record.getProviderStatus());
        notification.setProviderError(record.getProviderError());
        notification.setStatusUpdatedAt(record.getStatusUpdatedAt() != null
                ? record.getStatusUpdatedAt().toInstant(UTC) : null);
        notification.setResentFromId(record.getResentFromId());
        notification.setResendReason(record.getResendReason());
        notification.setCreatedAt(record.getCreatedAt() != null
                ? record.getCreatedAt().toInstant(UTC) : null);
        notification.setCreatedBy(record.getCreatedBy());

        return notification;
    }
}
