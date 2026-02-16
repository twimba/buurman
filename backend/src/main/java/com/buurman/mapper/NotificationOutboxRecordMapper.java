package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import org.springframework.stereotype.Component;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.OutboxStatus;
import com.buurman.jooq.generated.tables.records.NotificationOutboxRecord;

@Component
public class NotificationOutboxRecordMapper {

  public NotificationOutbox toDomain(NotificationOutboxRecord record) {
    if (record == null) {
      return null;
    }

    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setId(record.getId());
    outbox.setNotificationId(record.getNotificationId());
    outbox.setChannel(NotificationChannel.valueOf(record.getChannel()));
    outbox.setPayload(record.getPayload() != null ? record.getPayload().data() : null);
    outbox.setStatus(OutboxStatus.valueOf(record.getStatus()));
    outbox.setRetryCount(record.getRetryCount());
    outbox.setMaxRetries(record.getMaxRetries());
    outbox.setNextRetryAt(
        record.getNextRetryAt() != null ? record.getNextRetryAt().toInstant(UTC) : null);
    outbox.setLastError(record.getLastError());
    outbox.setCreatedAt(
        record.getCreatedAt() != null ? record.getCreatedAt().toInstant(UTC) : null);
    outbox.setProcessedAt(
        record.getProcessedAt() != null ? record.getProcessedAt().toInstant(UTC) : null);

    return outbox;
  }
}
