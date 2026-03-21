package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.OutboxStatus;
import com.buurman.jooq.generated.tables.records.NotificationOutboxRecord;

@DisplayName("NotificationOutboxRecordMapper")
class NotificationOutboxRecordMapperTest {

  private final NotificationOutboxRecordMapper mapper = new NotificationOutboxRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID NOTIFICATION_ID = UUID.randomUUID();
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<NotificationOutbox> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps typed fields from a complete record")
    void mapsTypedFields() {
      NotificationOutboxRecord record = createCompleteRecord();

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      NotificationOutbox outbox = result.get();
      assertThat(outbox.getId()).isEqualTo(ID);
      assertThat(outbox.getNotificationId()).isEqualTo(NOTIFICATION_ID);
      assertThat(outbox.getChannel()).isEqualTo(NotificationChannel.EMAIL);
      assertThat(outbox.getPayload()).isEqualTo("{\"to\":\"test@example.com\"}");
      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
      assertThat(outbox.getRetryCount()).isEqualTo(0);
      assertThat(outbox.getMaxRetries()).isEqualTo(3);
      assertThat(outbox.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("maps all NotificationChannel enum values")
    void mapsAllChannels() {
      for (NotificationChannel channel : NotificationChannel.values()) {
        NotificationOutboxRecord record = createCompleteRecord();
        record.setChannel(channel.name());

        Optional<NotificationOutbox> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getChannel()).isEqualTo(channel);
      }
    }

    @Test
    @DisplayName("maps all OutboxStatus enum values")
    void mapsAllStatuses() {
      for (OutboxStatus status : OutboxStatus.values()) {
        NotificationOutboxRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<NotificationOutbox> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("handles null payload")
    void handlesNullPayload() {
      NotificationOutboxRecord record = createCompleteRecord();
      record.setPayload(null);

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getPayload()).isNull();
    }

    @Test
    @DisplayName("handles null nextRetryAt")
    void handlesNullNextRetryAt() {
      NotificationOutboxRecord record = createCompleteRecord();
      record.setNextRetryAt(null);

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      // nextRetryAt is not set when null — field stays at default
    }

    @Test
    @DisplayName("maps nextRetryAt when present")
    void mapsNextRetryAt() {
      NotificationOutboxRecord record = createCompleteRecord();
      LocalDateTime nextRetry = LocalDateTime.of(2026, 3, 2, 12, 0, 0);
      record.setNextRetryAt(nextRetry);

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getNextRetryAt()).isEqualTo(nextRetry.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("wraps lastError as empty Optional when null")
    void wrapsNullLastErrorAsEmpty() {
      NotificationOutboxRecord record = createCompleteRecord();
      record.setLastError(null);

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getLastError()).isEmpty();
    }

    @Test
    @DisplayName("maps lastError when present")
    void mapsLastError() {
      NotificationOutboxRecord record = createCompleteRecord();
      record.setLastError("Connection timeout");

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getLastError()).contains("Connection timeout");
    }

    @Test
    @DisplayName("maps processedAt when present")
    void mapsProcessedAt() {
      NotificationOutboxRecord record = createCompleteRecord();
      LocalDateTime processed = LocalDateTime.of(2026, 3, 1, 13, 0, 0);
      record.setProcessedAt(processed);

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getProcessedAt()).contains(processed.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("defaults urgency to NORMAL for dynamic field access")
    void defaultsUrgencyToNormal() {
      NotificationOutboxRecord record = createCompleteRecord();

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      // Dynamic record.get("urgency", String.class) returns null on plain record
      assertThat(result).isPresent();
      assertThat(result.get().getUrgency()).isEqualTo(NotificationUrgency.NORMAL);
    }

    @Test
    @DisplayName("defaults consolidated to false for dynamic field access")
    void defaultsConsolidatedToFalse() {
      NotificationOutboxRecord record = createCompleteRecord();

      Optional<NotificationOutbox> result = mapper.toDomain(record);

      // Dynamic record.get("is_consolidated", Boolean.class) returns null on plain record
      assertThat(result).isPresent();
      assertThat(result.get().isConsolidated()).isFalse();
    }
  }

  private NotificationOutboxRecord createCompleteRecord() {
    NotificationOutboxRecord record = new NotificationOutboxRecord();
    record.setId(ID);
    record.setNotificationId(NOTIFICATION_ID);
    record.setChannel("EMAIL");
    record.setPayload(JSONB.jsonb("{\"to\":\"test@example.com\"}"));
    record.setStatus("PENDING");
    record.setRetryCount(0);
    record.setMaxRetries(3);
    record.setNextRetryAt(null);
    record.setLastError(null);
    record.setCreatedAt(NOW);
    record.setProcessedAt(null);
    return record;
  }
}
