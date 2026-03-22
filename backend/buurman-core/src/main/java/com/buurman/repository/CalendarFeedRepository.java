package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.CalendarFeed;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.CalendarFeedRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CalendarFeedRepository {

  private final DSLContext dsl;
  private final CalendarFeedRecordMapper mapper;
  private final Clock clock;

  public Optional<CalendarFeed> findByFeedToken(String feedToken) {
    return dsl.selectFrom(CALENDAR_FEEDS)
        .where(
            CALENDAR_FEEDS
                .FEED_TOKEN
                .eq(feedToken)
                .and(CALENDAR_FEEDS.ENABLED.isTrue())
                .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public List<CalendarFeed> findByUserIdAndTeamId(UUID userId, UUID teamId) {
    return dsl
        .selectFrom(CALENDAR_FEEDS)
        .where(
            CALENDAR_FEEDS
                .USER_ID
                .eq(userId)
                .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
                .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
        .orderBy(CALENDAR_FEEDS.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<CalendarFeed> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CALENDAR_FEEDS)
        .where(
            CALENDAR_FEEDS
                .IDENTIFIER
                .eq(identifier)
                .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
                .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public CalendarFeed getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Calendar feed not found"));
  }

  public Optional<CalendarFeed> findExistingFeed(
      CalendarFeed.FeedType feedType,
      Optional<UUID> contractId,
      Optional<UUID> propertyId,
      Optional<UUID> contactId,
      UUID userId,
      UUID teamId) {
    var condition =
        CALENDAR_FEEDS
            .FEED_TYPE
            .eq(feedType.name())
            .and(CALENDAR_FEEDS.USER_ID.eq(userId))
            .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
            .and(CALENDAR_FEEDS.DELETED_AT.isNull());

    condition =
        contractId.isPresent()
            ? condition.and(CALENDAR_FEEDS.CONTRACT_ID.eq(contractId.get()))
            : condition.and(CALENDAR_FEEDS.CONTRACT_ID.isNull());
    condition =
        propertyId.isPresent()
            ? condition.and(CALENDAR_FEEDS.PROPERTY_ID.eq(propertyId.get()))
            : condition.and(CALENDAR_FEEDS.PROPERTY_ID.isNull());
    condition =
        contactId.isPresent()
            ? condition.and(CALENDAR_FEEDS.CONTACT_ID.eq(contactId.get()))
            : condition.and(CALENDAR_FEEDS.CONTACT_ID.isNull());

    return dsl.selectFrom(CALENDAR_FEEDS)
        .where(condition)
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public CalendarFeed save(CalendarFeed feed) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (feed.getId() == null) {
      UUID id = UUID.randomUUID();

      dsl.insertInto(CALENDAR_FEEDS)
          .set(CALENDAR_FEEDS.ID, id)
          .set(CALENDAR_FEEDS.IDENTIFIER, feed.getIdentifier().orElseThrow())
          .set(CALENDAR_FEEDS.TEAM_ID, feed.getTeamId())
          .set(CALENDAR_FEEDS.USER_ID, feed.getUserId())
          .set(CALENDAR_FEEDS.FEED_TOKEN, feed.getFeedToken())
          .set(CALENDAR_FEEDS.FEED_TYPE, feed.getFeedType().name())
          .set(CALENDAR_FEEDS.CONTRACT_ID, feed.getContractId().orElse(null))
          .set(CALENDAR_FEEDS.PROPERTY_ID, feed.getPropertyId().orElse(null))
          .set(CALENDAR_FEEDS.CONTACT_ID, feed.getContactId().orElse(null))
          .set(CALENDAR_FEEDS.ENABLED, feed.getEnabled())
          .set(CALENDAR_FEEDS.CREATED_AT, now)
          .set(CALENDAR_FEEDS.UPDATED_AT, now)
          .set(CALENDAR_FEEDS.CREATED_BY, feed.getCreatedBy())
          .set(CALENDAR_FEEDS.UPDATED_BY, feed.getUpdatedBy())
          .execute();

      feed.setId(id);
      feed.setCreatedAt(now.toInstant(UTC));
      feed.setUpdatedAt(now.toInstant(UTC));
    } else {

      dsl.update(CALENDAR_FEEDS)
          .set(CALENDAR_FEEDS.FEED_TOKEN, feed.getFeedToken())
          .set(CALENDAR_FEEDS.ENABLED, feed.getEnabled())
          .set(CALENDAR_FEEDS.UPDATED_AT, now)
          .set(CALENDAR_FEEDS.UPDATED_BY, feed.getUpdatedBy())
          .where(
              CALENDAR_FEEDS.ID.eq(feed.getId()).and(CALENDAR_FEEDS.TEAM_ID.eq(feed.getTeamId())))
          .execute();

      feed.setUpdatedAt(now.toInstant(UTC));
    }

    return feed;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CALENDAR_FEEDS)
        .set(CALENDAR_FEEDS.DELETED_AT, now)
        .where(CALENDAR_FEEDS.ID.eq(id).and(CALENDAR_FEEDS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
