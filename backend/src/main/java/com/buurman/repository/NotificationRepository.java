package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.util.UlidGenerator.newNotificationId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.trueCondition;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.LabelCount;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.NotificationRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import com.buurman.domain.Ulid;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

  private final DSLContext dsl;
  private final NotificationRecordMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public Notification save(Notification notification) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    Ulid identifier = newNotificationId();
    LocalDateTime createdAt =
        notification.getCreatedAt() != null
            ? LocalDateTime.ofInstant(notification.getCreatedAt(), UTC)
            : now;

    JSONB contentVariablesJson = null;
    if (notification.getContentVariables().isPresent()) {
      try {
        contentVariablesJson =
            JSONB.valueOf(
                objectMapper.writeValueAsString(notification.getContentVariables().get()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize content variables", e);
      }
    }

    dsl.insertInto(NOTIFICATIONS)
        .set(NOTIFICATIONS.ID, id)
        .set(NOTIFICATIONS.IDENTIFIER, identifier)
        .set(NOTIFICATIONS.TEAM_ID, notification.getTeamId().orElse(null))
        .set(NOTIFICATIONS.NOTIFICATION_TYPE, notification.getNotificationType().name())
        .set(NOTIFICATIONS.SUBJECT, notification.getSubject().orElse(null))
        .set(NOTIFICATIONS.BODY, notification.getBody())
        .set(NOTIFICATIONS.RECIPIENT_EMAIL, notification.getRecipientEmail().orElse(null))
        .set(NOTIFICATIONS.RECIPIENT_PHONE, notification.getRecipientPhone().orElse(null))
        .set(NOTIFICATIONS.RECIPIENT_USER_ID, notification.getRecipientUserId().orElse(null))
        .set(NOTIFICATIONS.RECIPIENT_TENANT_ID, notification.getRecipientTenantId().orElse(null))
        .set(NOTIFICATIONS.CHANNEL, notification.getChannel().name())
        .set(NOTIFICATIONS.CONTENT_TEMPLATE, notification.getContentTemplate().orElse(null))
        .set(NOTIFICATIONS.CONTENT_VARIABLES, contentVariablesJson)
        .set(NOTIFICATIONS.STATUS, notification.getStatus().name())
        .set(NOTIFICATIONS.RESENT_FROM_ID, notification.getResentFromId().orElse(null))
        .set(NOTIFICATIONS.RESEND_REASON, notification.getResendReason().orElse(null))
        .set(NOTIFICATIONS.CREATED_AT, createdAt)
        .set(NOTIFICATIONS.CREATED_BY, notification.getCreatedBy().orElse(null))
        .execute();

    notification.setId(id);
    notification.setIdentifier(java.util.Optional.of(identifier));
    notification.setCreatedAt(createdAt.toInstant(UTC));

    return notification;
  }

  /**
   * Find notification by identifier. When teamId is null, searches across all teams (admin/system
   * use only). For tenant-scoped lookups, always pass a non-null teamId.
   */
  public Optional<Notification> findByIdentifierAndTeamId(
      Ulid identifier, @Nullable UUID teamId) {
    Condition condition = NOTIFICATIONS.IDENTIFIER.eq(identifier);
    if (teamId != null) {
      condition = condition.and(NOTIFICATIONS.TEAM_ID.eq(teamId));
    }
    return dsl.selectFrom(NOTIFICATIONS).where(condition).fetchOptional().flatMap(mapper::toDomain);
  }

  public Notification getByIdentifierAndTeamId(Ulid identifier, @Nullable UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Notification not found"));
  }

  /** See {@link #findByIdentifierAndTeamId} — same null-teamId semantics. */
  public Optional<Notification> findByIdAndTeamId(UUID id, @Nullable UUID teamId) {
    Condition condition = NOTIFICATIONS.ID.eq(id);
    if (teamId != null) {
      condition = condition.and(NOTIFICATIONS.TEAM_ID.eq(teamId));
    }
    return dsl.selectFrom(NOTIFICATIONS).where(condition).fetchOptional().flatMap(mapper::toDomain);
  }

  public Optional<Notification> findByProviderMessageId(String providerMessageId) {
    return dsl.selectFrom(NOTIFICATIONS)
        .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PaginatedResult<Notification> findAllByTeamIdPaginated(
      UUID teamId,
      @Nullable String type,
      @Nullable String channel,
      @Nullable String status,
      @Nullable String recipientEmail,
      @Nullable LocalDateTime dateFrom,
      @Nullable LocalDateTime dateTo,
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
      condition =
          condition.and(NOTIFICATIONS.RECIPIENT_EMAIL.likeIgnoreCase("%" + recipientEmail + "%"));
    }
    if (dateFrom != null) {
      condition = condition.and(NOTIFICATIONS.CREATED_AT.ge(dateFrom));
    }
    if (dateTo != null) {
      condition = condition.and(NOTIFICATIONS.CREATED_AT.le(dateTo));
    }

    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", NOTIFICATIONS.CREATED_AT,
            "notificationType", NOTIFICATIONS.NOTIFICATION_TYPE,
            "channel", NOTIFICATIONS.CHANNEL,
            "status", NOTIFICATIONS.STATUS,
            "recipientEmail", NOTIFICATIONS.RECIPIENT_EMAIL);

    return PaginationHelper.paginate(
        dsl,
        NOTIFICATIONS,
        condition,
        sortableFields,
        NOTIFICATIONS.CREATED_AT,
        pageRequest,
        r ->
            mapper
                .toDomain(r)
                .orElseThrow(() -> new IllegalStateException("Failed to map notification record")));
  }

  public void updateStatus(
      UUID id,
      NotificationStatus status,
      @Nullable String providerMessageId,
      @Nullable String providerStatus,
      @Nullable String providerError) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(NOTIFICATIONS)
        .set(NOTIFICATIONS.STATUS, status.name())
        .set(NOTIFICATIONS.PROVIDER_MESSAGE_ID, providerMessageId)
        .set(NOTIFICATIONS.PROVIDER_STATUS, providerStatus)
        .set(NOTIFICATIONS.PROVIDER_ERROR, providerError)
        .set(NOTIFICATIONS.STATUS_UPDATED_AT, now)
        .where(NOTIFICATIONS.ID.eq(id))
        .execute();
  }

  public void updateStatusByProviderMessageId(
      String providerMessageId,
      NotificationStatus status,
      @Nullable String providerStatus,
      @Nullable String providerError) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(NOTIFICATIONS)
        .set(NOTIFICATIONS.STATUS, status.name())
        .set(NOTIFICATIONS.PROVIDER_STATUS, providerStatus)
        .set(NOTIFICATIONS.PROVIDER_ERROR, providerError)
        .set(NOTIFICATIONS.STATUS_UPDATED_AT, now)
        .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
        .execute();
  }

  public void incrementOpenCount(String providerMessageId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(NOTIFICATIONS)
        .set(NOTIFICATIONS.OPEN_COUNT, NOTIFICATIONS.OPEN_COUNT.plus(1))
        .set(
            NOTIFICATIONS.FIRST_OPENED_AT,
            org.jooq.impl.DSL.coalesce(NOTIFICATIONS.FIRST_OPENED_AT, now))
        .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
        .execute();
  }

  public void incrementClickCount(String providerMessageId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(NOTIFICATIONS)
        .set(NOTIFICATIONS.CLICK_COUNT, NOTIFICATIONS.CLICK_COUNT.plus(1))
        .set(
            NOTIFICATIONS.FIRST_CLICKED_AT,
            org.jooq.impl.DSL.coalesce(NOTIFICATIONS.FIRST_CLICKED_AT, now))
        .where(NOTIFICATIONS.PROVIDER_MESSAGE_ID.eq(providerMessageId))
        .execute();
  }

  public List<LabelCount> countByTeamIdGroupedByStatus(UUID teamId) {
    return dsl.select(NOTIFICATIONS.STATUS, count().as("count"))
        .from(NOTIFICATIONS)
        .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
        .groupBy(NOTIFICATIONS.STATUS)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }

  public List<LabelCount> countByTeamIdGroupedByChannel(UUID teamId) {
    return dsl.select(NOTIFICATIONS.CHANNEL, count().as("count"))
        .from(NOTIFICATIONS)
        .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
        .groupBy(NOTIFICATIONS.CHANNEL)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }

  public long countByTeamId(UUID teamId) {
    Long result =
        dsl.selectCount()
            .from(NOTIFICATIONS)
            .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
            .fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  public PaginatedResult<Notification> findAllPaginatedUnscoped(
      @Nullable UUID teamId,
      @Nullable String type,
      @Nullable String channel,
      @Nullable String status,
      @Nullable String recipientEmail,
      @Nullable LocalDateTime dateFrom,
      @Nullable LocalDateTime dateTo,
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
      condition =
          condition.and(NOTIFICATIONS.RECIPIENT_EMAIL.likeIgnoreCase("%" + recipientEmail + "%"));
    }
    if (dateFrom != null) {
      condition = condition.and(NOTIFICATIONS.CREATED_AT.ge(dateFrom));
    }
    if (dateTo != null) {
      condition = condition.and(NOTIFICATIONS.CREATED_AT.le(dateTo));
    }

    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", NOTIFICATIONS.CREATED_AT,
            "notificationType", NOTIFICATIONS.NOTIFICATION_TYPE,
            "channel", NOTIFICATIONS.CHANNEL,
            "status", NOTIFICATIONS.STATUS,
            "recipientEmail", NOTIFICATIONS.RECIPIENT_EMAIL);

    return PaginationHelper.paginate(
        dsl,
        NOTIFICATIONS,
        condition,
        sortableFields,
        NOTIFICATIONS.CREATED_AT,
        pageRequest,
        r ->
            mapper
                .toDomain(r)
                .orElseThrow(() -> new IllegalStateException("Failed to map notification record")));
  }

  public Optional<Notification> findByIdentifierUnscoped(Ulid identifier) {
    return dsl.selectFrom(NOTIFICATIONS)
        .where(NOTIFICATIONS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Notification getByIdentifierUnscoped(Ulid identifier) {
    return findByIdentifierUnscoped(identifier)
        .orElseThrow(() -> new NotFoundException("Notification not found"));
  }

  public long countAll() {
    Long result = dsl.selectCount().from(NOTIFICATIONS).fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  public List<LabelCount> countGroupedByStatus() {
    return dsl.select(NOTIFICATIONS.STATUS, count().as("count"))
        .from(NOTIFICATIONS)
        .groupBy(NOTIFICATIONS.STATUS)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }

  public List<LabelCount> countGroupedByChannel() {
    return dsl.select(NOTIFICATIONS.CHANNEL, count().as("count"))
        .from(NOTIFICATIONS)
        .groupBy(NOTIFICATIONS.CHANNEL)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }
}
