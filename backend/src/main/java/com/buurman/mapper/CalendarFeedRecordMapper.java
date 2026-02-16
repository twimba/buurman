package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import org.springframework.stereotype.Component;

import com.buurman.domain.CalendarFeed;
import com.buurman.jooq.generated.tables.records.CalendarFeedsRecord;

@Component
public class CalendarFeedRecordMapper {

  public CalendarFeed toDomain(CalendarFeedsRecord record) {
    if (record == null) {
      return null;
    }

    CalendarFeed feed = new CalendarFeed();
    feed.setId(record.getId());
    feed.setIdentifier(record.getIdentifier());
    feed.setTeamId(record.getTeamId());
    feed.setUserId(record.getUserId());
    feed.setFeedToken(record.getFeedToken());
    feed.setFeedType(CalendarFeed.FeedType.valueOf(record.getFeedType()));
    feed.setContractId(record.getContractId());
    feed.setPropertyId(record.getPropertyId());
    feed.setTenantId(record.getTenantId());
    feed.setEnabled(record.getEnabled());
    feed.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().toInstant(UTC) : null);
    feed.setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(UTC) : null);
    feed.setCreatedBy(record.getCreatedBy());
    feed.setUpdatedBy(record.getUpdatedBy());
    feed.setDeletedAt(record.getDeletedAt() != null ? record.getDeletedAt().toInstant(UTC) : null);

    return feed;
  }
}
