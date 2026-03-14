package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.OutboxStatus;
import com.buurman.jooq.generated.tables.records.NotificationOutboxRecord;

@Component
public class NotificationOutboxRecordMapper {

  public Optional<NotificationOutbox> toDomain(@Nullable NotificationOutboxRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setId(record.getId());
    outbox.setNotificationId(record.getNotificationId());
    outbox.setChannel(NotificationChannel.valueOf(record.getChannel()));
    if (record.getPayload() != null) {
      outbox.setPayload(record.getPayload().data());
    }
    outbox.setStatus(OutboxStatus.valueOf(record.getStatus()));
    outbox.setRetryCount(record.getRetryCount());
    outbox.setMaxRetries(record.getMaxRetries());
    if (record.getNextRetryAt() != null) {
      outbox.setNextRetryAt(record.getNextRetryAt().toInstant(UTC));
    }
    outbox.setLastError(Optional.ofNullable(record.getLastError()));
    outbox.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    outbox.setProcessedAt(
        Optional.ofNullable(record.getProcessedAt()).map(dt -> dt.toInstant(UTC)));

    outbox.setNotificationType(
        Optional.ofNullable(record.get("notification_type", String.class))
            .map(NotificationType::valueOf));
    outbox.setRecipientUserId(Optional.ofNullable(record.get("recipient_user_id", UUID.class)));
    outbox.setRecipientEmail(Optional.ofNullable(record.get("recipient_email", String.class)));
    outbox.setTeamId(Optional.ofNullable(record.get("team_id", UUID.class)));

    String urgencyStr = record.get("urgency", String.class);
    outbox.setUrgency(
        urgencyStr != null ? NotificationUrgency.valueOf(urgencyStr) : NotificationUrgency.NORMAL);

    outbox.setConsolidationGroupId(
        Optional.ofNullable(record.get("consolidation_group_id", UUID.class)));
    Boolean consolidated = record.get("is_consolidated", Boolean.class);
    outbox.setConsolidated(consolidated != null && consolidated);

    return Optional.of(outbox);
  }
}
