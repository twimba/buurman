package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.CalendarFeed;
import com.buurman.jooq.generated.tables.records.CalendarFeedsRecord;

@Component
public class CalendarFeedRecordMapper {

  public Optional<CalendarFeed> toDomain(@Nullable CalendarFeedsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    CalendarFeed feed = new CalendarFeed();
    feed.setId(record.getId());
    feed.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    feed.setTeamId(record.getTeamId());
    feed.setUserId(record.getUserId());
    feed.setFeedToken(record.getFeedToken());
    feed.setFeedType(CalendarFeed.FeedType.valueOf(record.getFeedType()));
    feed.setContractId(Optional.ofNullable(record.getContractId()));
    feed.setPropertyId(Optional.ofNullable(record.getPropertyId()));
    feed.setTenantId(Optional.ofNullable(record.getTenantId()));
    feed.setEnabled(record.getEnabled());
    feed.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    feed.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    feed.setCreatedBy(record.getCreatedBy());
    feed.setUpdatedBy(record.getUpdatedBy());
    feed.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(feed);
  }
}
