package com.buurman.repository;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.OutboxStatus;
import com.buurman.mapper.NotificationOutboxRecordMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.NOTIFICATION_OUTBOX;

@Repository
public class NotificationOutboxRepository {

    private final DSLContext dsl;
    private final NotificationOutboxRecordMapper mapper;
    private final Clock clock;

    public NotificationOutboxRepository(DSLContext dsl, NotificationOutboxRecordMapper mapper, Clock clock) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.clock = clock;
    }

    public NotificationOutbox save(NotificationOutbox outbox) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = outbox.getCreatedAt() != null
                ? LocalDateTime.ofInstant(outbox.getCreatedAt(), ZoneOffset.UTC)
                : now;

        dsl.insertInto(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.ID, id)
                .set(NOTIFICATION_OUTBOX.NOTIFICATION_ID, outbox.getNotificationId())
                .set(NOTIFICATION_OUTBOX.CHANNEL, outbox.getChannel().name())
                .set(NOTIFICATION_OUTBOX.PAYLOAD, JSONB.valueOf(outbox.getPayload()))
                .set(NOTIFICATION_OUTBOX.STATUS, OutboxStatus.PENDING.name())
                .set(NOTIFICATION_OUTBOX.RETRY_COUNT, 0)
                .set(NOTIFICATION_OUTBOX.MAX_RETRIES, outbox.getMaxRetries() > 0 ? outbox.getMaxRetries() : 3)
                .set(NOTIFICATION_OUTBOX.CREATED_AT, createdAt)
                .execute();

        outbox.setId(id);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));

        return outbox;
    }

    public List<NotificationOutbox> findPendingBatch(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        return dsl.selectFrom(NOTIFICATION_OUTBOX)
                .where(NOTIFICATION_OUTBOX.STATUS.in(OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name())
                        .and(NOTIFICATION_OUTBOX.RETRY_COUNT.lt(NOTIFICATION_OUTBOX.MAX_RETRIES))
                        .and(NOTIFICATION_OUTBOX.NEXT_RETRY_AT.isNull()
                                .or(NOTIFICATION_OUTBOX.NEXT_RETRY_AT.le(now))))
                .orderBy(NOTIFICATION_OUTBOX.CREATED_AT.asc())
                .limit(batchSize)
                .fetch()
                .map(mapper::toDomain);
    }

    public void markProcessing(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.STATUS, OutboxStatus.PROCESSING.name())
                .set(NOTIFICATION_OUTBOX.PROCESSED_AT, now)
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }

    public void markSent(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.STATUS, OutboxStatus.SENT.name())
                .set(NOTIFICATION_OUTBOX.PROCESSED_AT, now)
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }

    public void markFailed(UUID id, String error, int currentRetryCount) {
        LocalDateTime now = LocalDateTime.now(clock);
        int newRetryCount = currentRetryCount + 1;

        // Exponential backoff: 30s, 2min, 10min
        long[] backoffSeconds = {30, 120, 600};
        int backoffIndex = Math.min(currentRetryCount, backoffSeconds.length - 1);
        LocalDateTime nextRetry = now.plusSeconds(backoffSeconds[backoffIndex]);

        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.STATUS, OutboxStatus.FAILED.name())
                .set(NOTIFICATION_OUTBOX.RETRY_COUNT, newRetryCount)
                .set(NOTIFICATION_OUTBOX.LAST_ERROR, error)
                .set(NOTIFICATION_OUTBOX.NEXT_RETRY_AT, nextRetry)
                .set(NOTIFICATION_OUTBOX.PROCESSED_AT, now)
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }
}
