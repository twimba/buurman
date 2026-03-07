package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
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

    return Optional.of(outbox);
  }
}
