package com.buurman.repository;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.NotificationRecordMapper;
import com.buurman.util.EntityPrefix;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.util.UlidGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;

@Repository
public class NotificationRepository {

    private final DSLContext dsl;
    private final NotificationRecordMapper mapper;
    private final ObjectMapper objectMapper;

    public NotificationRepository(DSLContext dsl, NotificationRecordMapper mapper, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public Notification save(Notification notification) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        UUID id = UUID.randomUUID();
        String identifier = UlidGenerator.generate(EntityPrefix.NTF);

        JSONB contentVariablesJson = null;
        if (notification.getContentVariables() != null) {
            try {
                contentVariablesJson = JSONB.valueOf(objectMapper.writeValueAsString(notification.getContentVariables()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize content variables", e);
            }
        }

        dsl.insertInto(NOTIFICATIONS)
                .set(NOTIFICATIONS.ID, id)
                .set(NOTIFICATIONS.IDENTIFIER, identifier)
                .set(NOTIFICATIONS.TEAM_ID, notification.getTeamId())
                .set(NOTIFICATIONS.NOTIFICATION_TYPE, notification.getNotificationType().name())
                .set(NOTIFICATIONS.SUBJECT, notification.getSubject())
                .set(NOTIFICATIONS.BODY, notification.getBody())
                .set(NOTIFICATIONS.RECIPIENT_EMAIL, notification.getRecipientEmail())
                .set(NOTIFICATIONS.RECIPIENT_PHONE, notification.getRecipientPhone())
                .set(NOTIFICATIONS.RECIPIENT_USER_ID, notification.getRecipientUserId())
                .set(NOTIFICATIONS.RECIPIENT_TENANT_ID, notification.getRecipientTenantId())
                .set(NOTIFICATIONS.CHANNEL, notification.getChannel().name())
                .set(NOTIFICATIONS.CONTENT_TEMPLATE, notification.getContentTemplate())
                .set(NOTIFICATIONS.CONTENT_VARIABLES, contentVariablesJson)
                .set(NOTIFICATIONS.STATUS, notification.getStatus().name())
                .set(NOTIFICATIONS.RESENT_FROM_ID, notification.getResentFromId())
                .set(NOTIFICATIONS.RESEND_REASON, notification.getResendReason())
                .set(NOTIFICATIONS.CREATED_AT, now)
                .set(NOTIFICATIONS.CREATED_BY, notification.getCreatedBy())
                .execute();

        notification.setId(id);
        notification.setIdentifier(identifier);
        notification.setCreatedAt(now.toInstant(ZoneOffset.UTC));

        return notification;
    }

    public Optional<Notification> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(NOTIFICATIONS)
                .where(NOTIFICATIONS.IDENTIFIER.eq(identifier)
                        .and(NOTIFICATIONS.TEAM_ID.eq(teamId)))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<Notification> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(NOTIFICATIONS)
                .where(NOTIFICATIONS.ID.eq(id)
                        .and(NOTIFICATIONS.TEAM_ID.eq(teamId)))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<Notification> findByProviderMessageId(String providerMessageId) {
        return dsl.selectFrom(NOTIFICATIONS)
                .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public PaginatedResult<Notification> findAllByTeamIdPaginated(
            UUID teamId, String type, String channel, String status,
            String recipientEmail, LocalDateTime dateFrom, LocalDateTime dateTo,
            PageRequest pageRequest) {

        Condition condition = NOTIFICATIONS.TEAM_ID.eq(teamId);

        if (type != null && !type.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.NOTIFICATION_TYPE.eq(type));
        }
        if (channel != null && !channel.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.CHANNEL.eq(channel));
        }
        if (status != null && !status.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.STATUS.eq(status));
        }
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.RECIPIENT_EMAIL.likeIgnoreCase("%" + recipientEmail + "%"));
        }
        if (dateFrom != null) {
            condition = condition.and(NOTIFICATIONS.CREATED_AT.ge(dateFrom));
        }
        if (dateTo != null) {
            condition = condition.and(NOTIFICATIONS.CREATED_AT.le(dateTo));
        }

        Map<String, Field<?>> sortableFields = Map.of(
                "createdAt", NOTIFICATIONS.CREATED_AT,
                "notificationType", NOTIFICATIONS.NOTIFICATION_TYPE,
                "channel", NOTIFICATIONS.CHANNEL,
                "status", NOTIFICATIONS.STATUS,
                "recipientEmail", NOTIFICATIONS.RECIPIENT_EMAIL
        );

        return PaginationHelper.paginate(dsl, NOTIFICATIONS, condition, sortableFields,
                NOTIFICATIONS.CREATED_AT, pageRequest, r -> mapper.toDomain(
                        (com.buurman.jooq.generated.tables.records.NotificationsRecord) r));
    }

    public void updateStatus(UUID id, NotificationStatus status, String providerMessageId,
                             String providerStatus, String providerError) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.STATUS, status.name())
                .set(NOTIFICATIONS.PROVIDER_MESSAGE_ID, providerMessageId)
                .set(NOTIFICATIONS.PROVIDER_STATUS, providerStatus)
                .set(NOTIFICATIONS.PROVIDER_ERROR, providerError)
                .set(NOTIFICATIONS.STATUS_UPDATED_AT, now)
                .where(NOTIFICATIONS.ID.eq(id))
                .execute();
    }

    public void updateStatusByProviderMessageId(String providerMessageId, NotificationStatus status,
                                                 String providerStatus, String providerError) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.STATUS, status.name())
                .set(NOTIFICATIONS.PROVIDER_STATUS, providerStatus)
                .set(NOTIFICATIONS.PROVIDER_ERROR, providerError)
                .set(NOTIFICATIONS.STATUS_UPDATED_AT, now)
                .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
                .execute();
    }

    public List<Record2<String, Integer>> countByTeamIdGroupedByStatus(UUID teamId) {
        return dsl.select(NOTIFICATIONS.STATUS, count().as("count"))
                .from(NOTIFICATIONS)
                .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
                .groupBy(NOTIFICATIONS.STATUS)
                .fetch();
    }

    public List<Record2<String, Integer>> countByTeamIdGroupedByChannel(UUID teamId) {
        return dsl.select(NOTIFICATIONS.CHANNEL, count().as("count"))
                .from(NOTIFICATIONS)
                .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
                .groupBy(NOTIFICATIONS.CHANNEL)
                .fetch();
    }

    public long countByTeamId(UUID teamId) {
        return dsl.selectCount()
                .from(NOTIFICATIONS)
                .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
                .fetchOne(0, long.class);
    }

    public PaginatedResult<Notification> findAllPaginatedUnscoped(
            UUID teamId, String type, String channel, String status,
            String recipientEmail, LocalDateTime dateFrom, LocalDateTime dateTo,
            PageRequest pageRequest) {

        Condition condition = trueCondition();

        if (teamId != null) {
            condition = condition.and(NOTIFICATIONS.TEAM_ID.eq(teamId));
        }
        if (type != null && !type.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.NOTIFICATION_TYPE.eq(type));
        }
        if (channel != null && !channel.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.CHANNEL.eq(channel));
        }
        if (status != null && !status.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.STATUS.eq(status));
        }
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            condition = condition.and(NOTIFICATIONS.RECIPIENT_EMAIL.likeIgnoreCase("%" + recipientEmail + "%"));
        }
        if (dateFrom != null) {
            condition = condition.and(NOTIFICATIONS.CREATED_AT.ge(dateFrom));
        }
        if (dateTo != null) {
            condition = condition.and(NOTIFICATIONS.CREATED_AT.le(dateTo));
        }

        Map<String, Field<?>> sortableFields = Map.of(
            "createdAt", NOTIFICATIONS.CREATED_AT,
            "notificationType", NOTIFICATIONS.NOTIFICATION_TYPE,
            "channel", NOTIFICATIONS.CHANNEL,
            "status", NOTIFICATIONS.STATUS,
            "recipientEmail", NOTIFICATIONS.RECIPIENT_EMAIL
        );

        return PaginationHelper.paginate(dsl, NOTIFICATIONS, condition, sortableFields,
            NOTIFICATIONS.CREATED_AT, pageRequest, r -> mapper.toDomain(
                (com.buurman.jooq.generated.tables.records.NotificationsRecord) r));
    }

    public Optional<Notification> findByIdentifierUnscoped(String identifier) {
        return dsl.selectFrom(NOTIFICATIONS)
            .where(NOTIFICATIONS.IDENTIFIER.eq(identifier))
            .fetchOptional()
            .map(r -> mapper.toDomain(
                (com.buurman.jooq.generated.tables.records.NotificationsRecord) r));
    }

    public long countAll() {
        return dsl.selectCount().from(NOTIFICATIONS).fetchOne(0, long.class);
    }

    public List<Record2<String, Integer>> countGroupedByStatus() {
        return dsl.select(NOTIFICATIONS.STATUS, count().as("count"))
            .from(NOTIFICATIONS)
            .groupBy(NOTIFICATIONS.STATUS)
            .fetch();
    }

    public List<Record2<String, Integer>> countGroupedByChannel() {
        return dsl.select(NOTIFICATIONS.CHANNEL, count().as("count"))
            .from(NOTIFICATIONS)
            .groupBy(NOTIFICATIONS.CHANNEL)
            .fetch();
    }
}
