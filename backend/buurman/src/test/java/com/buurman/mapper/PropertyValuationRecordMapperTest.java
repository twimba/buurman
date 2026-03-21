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

import com.buurman.domain.PropertyValuation;
import com.buurman.domain.PropertyValuation.ValuationType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyValuationsRecord;

@DisplayName("PropertyValuationRecordMapper")
class PropertyValuationRecordMapperTest {

  private final PropertyValuationRecordMapper mapper = new PropertyValuationRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PVA01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyValuation> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyValuationsRecord record = createCompleteRecord();

      Optional<PropertyValuation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyValuation val = result.get();
      assertThat(val.getId()).isEqualTo(ID);
      assertThat(val.getIdentifier()).contains(IDENTIFIER);
      assertThat(val.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(val.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(val.getValuationType()).isEqualTo(ValuationType.MARKET);
      assertThat(val.getValuationDate()).isEqualTo(LocalDate.of(2026, 1, 15));
      assertThat(val.getAmount().value()).isEqualByComparingTo(new BigDecimal("350000.00"));
      assertThat(val.getAmount().currency()).isEqualTo("EUR");
      assertThat(val.getSource()).contains("Independent appraiser");
      assertThat(val.getNotes()).contains("Annual valuation");
      assertThat(val.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(val.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(val.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(val.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(val.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all ValuationType enum values")
    void mapsAllValuationTypes() {
      for (ValuationType type : ValuationType.values()) {
        PropertyValuationsRecord record = createCompleteRecord();
        record.setValuationType(type.name());

        Optional<PropertyValuation> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getValuationType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyValuationsRecord record = createCompleteRecord();
      record.setSource(null);
      record.setNotes(null);

      Optional<PropertyValuation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getSource()).isEmpty();
      assertThat(result.get().getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyValuationsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyValuation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyValuationsRecord createCompleteRecord() {
    PropertyValuationsRecord record = new PropertyValuationsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setValuationType("MARKET");
    record.setValuationDate(LocalDate.of(2026, 1, 15));
    record.setAmount(new BigDecimal("350000.00"));
    record.setCurrency("EUR");
    record.setSource("Independent appraiser");
    record.setNotes("Annual valuation");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
