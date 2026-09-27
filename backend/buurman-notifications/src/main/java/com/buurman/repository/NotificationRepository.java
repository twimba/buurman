package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.util.SidGenerator.newNotificationId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.trueCondition;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.LabelCount;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.NotificationRecordMapper;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

  private final DSLContext dsl;
  private final NotificationRecordMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Newest N: a timeline is for reading, not for auditing every message ever sent. */
  private static final int TIMELINE_LIMIT = 100;

  public Notification save(Notification notification) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    Sid identifier = newNotificationId();
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
        .set(NOTIFICATIONS.RECIPIENT_CONTACT_ID, notification.getRecipientContactId().orElse(null))
        .set(NOTIFICATIONS.PAYMENT_ID, notification.getRelatedPaymentId().orElse(null))
        .set(NOTIFICATIONS.CONTRACT_ID, notification.getRelatedContractId().orElse(null))
        .set(NOTIFICATIONS.CHANNEL, notification.getChannel().name())
        .set(NOTIFICATIONS.CONTENT_TEMPLATE, notification.getContentTemplate().orElse(null))
        .set(NOTIFICATIONS.CONTENT_VARIABLES, contentVariablesJson)
        .set(NOTIFICATIONS.STATUS, notification.getStatus().name())
        .set(NOTIFICATIONS.RESENT_FROM_ID, notification.getResentFromId().orElse(null))
        .set(NOTIFICATIONS.RESEND_REASON, notification.getResendReason().orElse(null))
        .set(DSL.field("urgency", String.class), notification.getUrgency().name())
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
   * use only). For team-scoped lookups, always pass a non-null teamId.
   */
  public Optional<Notification> findByIdentifierAndTeamId(Sid identifier, @Nullable UUID teamId) {
    Condition condition = NOTIFICATIONS.IDENTIFIER.eq(identifier);
    if (teamId != null) {
      condition = condition.and(NOTIFICATIONS.TEAM_ID.eq(teamId));
    }
    return dsl.selectFrom(NOTIFICATIONS).where(condition).fetchOptional().flatMap(mapper::toDomain);
  }

  public Notification getByIdentifierAndTeamId(Sid identifier, @Nullable UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Notification not found"));
  }

  /** See {@link #findByIdentifierAndTeamId} — same null-teamId semantics. */
  /** Newest first: a timeline reads top-down from the most recent message. */
  public List<Notification> findByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
    return findByEntityAndTeamId(NOTIFICATIONS.PAYMENT_ID.eq(paymentId), teamId);
  }

  public List<Notification> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return findByEntityAndTeamId(NOTIFICATIONS.CONTRACT_ID.eq(contractId), teamId);
  }

  /**
   * The team_id predicate is not redundant with the caller's entity lookup: it is the guarantee
   * that holds even if a caller reaches this repository by another route.
   */
  private List<Notification> findByEntityAndTeamId(Condition entityMatches, UUID teamId) {
    return dsl
        .selectFrom(NOTIFICATIONS)
        .where(entityMatches.and(NOTIFICATIONS.TEAM_ID.eq(teamId)))
        .orderBy(NOTIFICATIONS.CREATED_AT.desc())
        // sendToTeam emits one notification per admin/editor per channel, so a long-lived
        // contract accumulates far more than the handful a single send suggests.
        .limit(TIMELINE_LIMIT)
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<Notification> findByIdAndTeamId(UUID id, @Nullable UUID teamId) {
    Condition condition = NOTIFICATIONS.ID.eq(id);
    if (teamId != null) {
      condition = condition.and(NOTIFICATIONS.TEAM_ID.eq(teamId));
    }
    return dsl.selectFrom(NOTIFICATIONS).where(condition).fetchOptional().flatMap(mapper::toDomain);
  }

  /** Delivery state per notification, for the reminders timeline. */
  public Map<UUID, NotificationService.DeliveryState> findDeliveryStatesByIdsAndTeamId(
      Collection<UUID> ids, UUID teamId) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, NotificationService.DeliveryState> out = new HashMap<>();
    dsl.select(NOTIFICATIONS.ID, NOTIFICATIONS.STATUS, NOTIFICATIONS.PROVIDER_ERROR)
        .from(NOTIFICATIONS)
        .where(NOTIFICATIONS.ID.in(ids).and(NOTIFICATIONS.TEAM_ID.eq(teamId)))
        .forEach(
            r ->
                out.put(
                    r.value1(),
                    new NotificationService.DeliveryState(
                        NotificationStatus.valueOf(r.value2()), Optional.ofNullable(r.value3()))));
    return out;
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

  public Optional<Notification> findByIdentifierUnscoped(Sid identifier) {
    return dsl.selectFrom(NOTIFICATIONS)
        .where(NOTIFICATIONS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Notification getByIdentifierUnscoped(Sid identifier) {
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
