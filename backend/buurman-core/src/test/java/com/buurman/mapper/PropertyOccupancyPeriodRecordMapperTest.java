package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyEndReason;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyOccupancyPeriodsRecord;

@DisplayName("PropertyOccupancyPeriodRecordMapper")
class PropertyOccupancyPeriodRecordMapperTest {

  private final PropertyOccupancyPeriodRecordMapper mapper =
      new PropertyOccupancyPeriodRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID UNIT_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("POC01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyOccupancyPeriod> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyOccupancyPeriodsRecord record = createCompleteRecord();

      Optional<PropertyOccupancyPeriod> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyOccupancyPeriod period = result.get();
      assertThat(period.getId()).isEqualTo(ID);
      assertThat(period.getIdentifier()).contains(IDENTIFIER);
      assertThat(period.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(period.getPropertyId()).isEqualTo(PROPERTY_ID);
      // Distinct from PROPERTY_ID: catches a setUnitId(record.getPropertyId()) copy-paste bug
      // (BUUR-106 follow-up register, section F, item 8).
      assertThat(period.getUnitId()).isEqualTo(UNIT_ID);
      assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
      assertThat(period.getEndDate()).contains(LocalDate.of(2025, 12, 31));
      assertThat(period.getType()).isEqualTo(OccupancyType.PERSONAL);
      assertThat(period.getOccupantName()).contains("John Doe");
      assertThat(period.getMonthlyImputedRent()).contains(new BigDecimal("800.00"));
      assertThat(period.getEndReason()).contains(OccupancyEndReason.CONVERTING_TO_RENTAL);
      assertThat(period.getNotes()).contains("Personal use");
      assertThat(period.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(period.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(period.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(period.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(period.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all OccupancyType enum values")
    void mapsAllOccupancyTypes() {
      for (OccupancyType type : OccupancyType.values()) {
        PropertyOccupancyPeriodsRecord record = createCompleteRecord();
        record.setType(type.name());

        Optional<PropertyOccupancyPeriod> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all OccupancyEndReason enum values")
    void mapsAllEndReasons() {
      for (OccupancyEndReason reason : OccupancyEndReason.values()) {
        PropertyOccupancyPeriodsRecord record = createCompleteRecord();
        record.setEndReason(reason.name());

        Optional<PropertyOccupancyPeriod> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getEndReason()).contains(reason);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyOccupancyPeriodsRecord record = createCompleteRecord();
      record.setEndDate(null);
      record.setOccupantName(null);
      record.setMonthlyImputedRent(null);
      record.setEndReason(null);
      record.setNotes(null);

      Optional<PropertyOccupancyPeriod> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyOccupancyPeriod period = result.get();
      assertThat(period.getEndDate()).isEmpty();
      assertThat(period.getOccupantName()).isEmpty();
      assertThat(period.getMonthlyImputedRent()).isEmpty();
      assertThat(period.getEndReason()).isEmpty();
      assertThat(period.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyOccupancyPeriodsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyOccupancyPeriod> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyOccupancyPeriodsRecord createCompleteRecord() {
    PropertyOccupancyPeriodsRecord record = new PropertyOccupancyPeriodsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setPropertyId(PROPERTY_ID);
    record.setUnitId(UNIT_ID);
    record.setStartDate(LocalDate.of(2024, 1, 1));
    record.setEndDate(LocalDate.of(2025, 12, 31));
    record.setType("PERSONAL");
    record.setOccupantName("John Doe");
    record.setMonthlyImputedRent(new BigDecimal("800.00"));
    record.setEndReason("CONVERTING_TO_RENTAL");
    record.setNotes("Personal use");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
