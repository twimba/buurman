package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.CalendarFeed;
import com.buurman.domain.CalendarFeed.FeedType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.CalendarFeedsRecord;

@DisplayName("CalendarFeedRecordMapper")
class CalendarFeedRecordMapperTest {

  private final CalendarFeedRecordMapper mapper = new CalendarFeedRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("CAL01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<CalendarFeed> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      CalendarFeedsRecord record = createCompleteRecord();

      Optional<CalendarFeed> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      CalendarFeed feed = result.get();
      assertThat(feed.getId()).isEqualTo(ID);
      assertThat(feed.getIdentifier()).contains(IDENTIFIER);
      assertThat(feed.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(feed.getUserId()).isEqualTo(USER_ID);
      assertThat(feed.getFeedToken()).isEqualTo("token-abc-123");
      assertThat(feed.getFeedType()).isEqualTo(FeedType.ALL_PAYMENTS);
      assertThat(feed.getContractId()).contains(CONTRACT_ID);
      assertThat(feed.getPropertyId()).contains(PROPERTY_ID);
      assertThat(feed.getTenantId()).contains(TENANT_ID);
      assertThat(feed.getEnabled()).isTrue();
      assertThat(feed.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(feed.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(feed.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(feed.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(feed.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all FeedType enum values")
    void mapsAllFeedTypes() {
      for (FeedType type : FeedType.values()) {
        CalendarFeedsRecord record = createCompleteRecord();
        record.setFeedType(type.name());

        Optional<CalendarFeed> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getFeedType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("wraps nullable UUID fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      CalendarFeedsRecord record = createCompleteRecord();
      record.setContractId(null);
      record.setPropertyId(null);
      record.setTenantId(null);

      Optional<CalendarFeed> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      CalendarFeed feed = result.get();
      assertThat(feed.getContractId()).isEmpty();
      assertThat(feed.getPropertyId()).isEmpty();
      assertThat(feed.getTenantId()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      CalendarFeedsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<CalendarFeed> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt())
          .contains(deletedAt.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("maps enabled false correctly")
    void mapsEnabledFalse() {
      CalendarFeedsRecord record = createCompleteRecord();
      record.setEnabled(false);

      Optional<CalendarFeed> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getEnabled()).isFalse();
    }
  }

  private CalendarFeedsRecord createCompleteRecord() {
    CalendarFeedsRecord record = new CalendarFeedsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setUserId(USER_ID);
    record.setFeedToken("token-abc-123");
    record.setFeedType("ALL_PAYMENTS");
    record.setContractId(CONTRACT_ID);
    record.setPropertyId(PROPERTY_ID);
    record.setTenantId(TENANT_ID);
    record.setEnabled(true);
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
