package com.buurman.repository;

import com.buurman.domain.CalendarFeed;
import com.buurman.mapper.CalendarFeedRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;

@Repository
public class CalendarFeedRepository {

    private final DSLContext dsl;
    private final CalendarFeedRecordMapper mapper;
    private final Clock clock;

    public CalendarFeedRepository(DSLContext dsl, CalendarFeedRecordMapper mapper, Clock clock) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Optional<CalendarFeed> findByFeedToken(String feedToken) {
        return dsl.selectFrom(CALENDAR_FEEDS)
                .where(CALENDAR_FEEDS.FEED_TOKEN.eq(feedToken)
                        .and(CALENDAR_FEEDS.ENABLED.isTrue())
                        .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<CalendarFeed> findByUserIdAndTeamId(UUID userId, UUID teamId) {
        return dsl.selectFrom(CALENDAR_FEEDS)
                .where(CALENDAR_FEEDS.USER_ID.eq(userId)
                        .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
                        .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
                .orderBy(CALENDAR_FEEDS.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Optional<CalendarFeed> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(CALENDAR_FEEDS)
                .where(CALENDAR_FEEDS.IDENTIFIER.eq(identifier)
                        .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
                        .and(CALENDAR_FEEDS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<CalendarFeed> findExistingFeed(
            CalendarFeed.FeedType feedType, UUID contractId, UUID propertyId,
            UUID tenantId, UUID userId, UUID teamId) {
        var condition = CALENDAR_FEEDS.FEED_TYPE.eq(feedType.name())
                .and(CALENDAR_FEEDS.USER_ID.eq(userId))
                .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId))
                .and(CALENDAR_FEEDS.DELETED_AT.isNull());

        condition = contractId != null
                ? condition.and(CALENDAR_FEEDS.CONTRACT_ID.eq(contractId))
                : condition.and(CALENDAR_FEEDS.CONTRACT_ID.isNull());
        condition = propertyId != null
                ? condition.and(CALENDAR_FEEDS.PROPERTY_ID.eq(propertyId))
                : condition.and(CALENDAR_FEEDS.PROPERTY_ID.isNull());
        condition = tenantId != null
                ? condition.and(CALENDAR_FEEDS.TENANT_ID.eq(tenantId))
                : condition.and(CALENDAR_FEEDS.TENANT_ID.isNull());

        return dsl.selectFrom(CALENDAR_FEEDS)
                .where(condition)
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public CalendarFeed save(CalendarFeed feed) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (feed.getId() == null) {
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = feed.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(feed.getCreatedAt(), ZoneOffset.UTC)
                    : now;
            LocalDateTime updatedAt = feed.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(feed.getUpdatedAt(), ZoneOffset.UTC)
                    : now;

            dsl.insertInto(CALENDAR_FEEDS)
                    .set(CALENDAR_FEEDS.ID, id)
                    .set(CALENDAR_FEEDS.IDENTIFIER, feed.getIdentifier())
                    .set(CALENDAR_FEEDS.TEAM_ID, feed.getTeamId())
                    .set(CALENDAR_FEEDS.USER_ID, feed.getUserId())
                    .set(CALENDAR_FEEDS.FEED_TOKEN, feed.getFeedToken())
                    .set(CALENDAR_FEEDS.FEED_TYPE, feed.getFeedType().name())
                    .set(CALENDAR_FEEDS.CONTRACT_ID, feed.getContractId())
                    .set(CALENDAR_FEEDS.PROPERTY_ID, feed.getPropertyId())
                    .set(CALENDAR_FEEDS.TENANT_ID, feed.getTenantId())
                    .set(CALENDAR_FEEDS.ENABLED, feed.getEnabled())
                    .set(CALENDAR_FEEDS.CREATED_AT, createdAt)
                    .set(CALENDAR_FEEDS.UPDATED_AT, updatedAt)
                    .set(CALENDAR_FEEDS.CREATED_BY, feed.getCreatedBy())
                    .set(CALENDAR_FEEDS.UPDATED_BY, feed.getUpdatedBy())
                    .execute();

            feed.setId(id);
            feed.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
            feed.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        } else {
            LocalDateTime updatedAt = feed.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(feed.getUpdatedAt(), ZoneOffset.UTC)
                    : now;

            dsl.update(CALENDAR_FEEDS)
                    .set(CALENDAR_FEEDS.FEED_TOKEN, feed.getFeedToken())
                    .set(CALENDAR_FEEDS.ENABLED, feed.getEnabled())
                    .set(CALENDAR_FEEDS.UPDATED_AT, updatedAt)
                    .set(CALENDAR_FEEDS.UPDATED_BY, feed.getUpdatedBy())
                    .where(CALENDAR_FEEDS.ID.eq(feed.getId())
                            .and(CALENDAR_FEEDS.TEAM_ID.eq(feed.getTeamId())))
                    .execute();

            feed.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        }

        return feed;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(CALENDAR_FEEDS)
                .set(CALENDAR_FEEDS.DELETED_AT, now)
                .where(CALENDAR_FEEDS.ID.eq(id)
                        .and(CALENDAR_FEEDS.TEAM_ID.eq(teamId)))
                .execute();
    }
}
