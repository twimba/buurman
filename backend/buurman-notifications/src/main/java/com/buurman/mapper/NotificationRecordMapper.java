package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;
import com.buurman.jooq.generated.tables.records.NotificationsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@SuppressWarnings("NullAway.Init")
public class NotificationRecordMapper {

  private final ObjectMapper objectMapper;

  public Optional<Notification> toDomain(@Nullable NotificationsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    Notification notification = new Notification();
    notification.setId(record.getId());
    notification.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    notification.setTeamId(Optional.ofNullable(record.getTeamId()));
    notification.setNotificationType(NotificationType.valueOf(record.getNotificationType()));
    notification.setSubject(Optional.ofNullable(record.getSubject()));
    notification.setBody(record.getBody());
    notification.setRecipientEmail(Optional.ofNullable(record.getRecipientEmail()));
    notification.setRecipientPhone(Optional.ofNullable(record.getRecipientPhone()));
    notification.setRecipientUserId(Optional.ofNullable(record.getRecipientUserId()));
    notification.setRecipientTenantId(Optional.ofNullable(record.getRecipientTenantId()));
    notification.setChannel(NotificationChannel.valueOf(record.getChannel()));
    notification.setContentTemplate(Optional.ofNullable(record.getContentTemplate()));

    if (record.getContentVariables() != null) {
      try {
        Map<String, Object> variables =
            objectMapper.readValue(record.getContentVariables().data(), new TypeReference<>() {});
        notification.setContentVariables(Optional.of(variables));
      } catch (Exception e) {
        // Log but don't fail - content variables are informational
      }
    }

    notification.setStatus(NotificationStatus.valueOf(record.getStatus()));
    notification.setProviderMessageId(Optional.ofNullable(record.getProviderMessageId()));
    notification.setProviderStatus(Optional.ofNullable(record.getProviderStatus()));
    notification.setProviderError(Optional.ofNullable(record.getProviderError()));
    notification.setStatusUpdatedAt(
        Optional.ofNullable(record.getStatusUpdatedAt()).map(dt -> dt.toInstant(UTC)));
    notification.setOpenCount(record.getOpenCount());
    notification.setClickCount(record.getClickCount());
    notification.setFirstOpenedAt(
        Optional.ofNullable(record.getFirstOpenedAt()).map(dt -> dt.toInstant(UTC)));
    notification.setFirstClickedAt(
        Optional.ofNullable(record.getFirstClickedAt()).map(dt -> dt.toInstant(UTC)));
    notification.setResentFromId(Optional.ofNullable(record.getResentFromId()));
    notification.setResendReason(Optional.ofNullable(record.getResendReason()));

    String urgencyStr = record.get("urgency", String.class);
    notification.setUrgency(
        urgencyStr != null ? NotificationUrgency.valueOf(urgencyStr) : NotificationUrgency.NORMAL);

    notification.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    notification.setCreatedBy(Optional.ofNullable(record.getCreatedBy()));

    return Optional.of(notification);
  }
}
