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

import com.buurman.domain.PropertyFee;
import com.buurman.domain.PropertyFee.FeeStatus;
import com.buurman.domain.PropertyFee.FeeType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyFeesRecord;

@DisplayName("PropertyFeeRecordMapper")
class PropertyFeeRecordMapperTest {

  private final PropertyFeeRecordMapper mapper = new PropertyFeeRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PFE01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyFee> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyFeesRecord record = createCompleteRecord();

      Optional<PropertyFee> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyFee fee = result.get();
      assertThat(fee.getId()).isEqualTo(ID);
      assertThat(fee.getIdentifier()).contains(IDENTIFIER);
      assertThat(fee.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(fee.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(fee.getFeeType()).isEqualTo(FeeType.HOA);
      assertThat(fee.getName()).contains("Monthly HOA");
      assertThat(fee.getAnnualAmount().value()).isEqualByComparingTo(new BigDecimal("3600.00"));
      assertThat(fee.getAnnualAmount().currency()).isEqualTo("EUR");
      assertThat(fee.getPaymentFrequency()).isEqualTo("MONTHLY");
      assertThat(fee.getDueMonths()).contains("1,2,3,4,5,6,7,8,9,10,11,12");
      assertThat(fee.getStartDate()).contains(LocalDate.of(2026, 1, 1));
      assertThat(fee.getEndDate()).contains(LocalDate.of(2026, 12, 31));
      assertThat(fee.getStatus()).isEqualTo(FeeStatus.ACTIVE);
      assertThat(fee.getNotes()).contains("HOA fees");
      assertThat(fee.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all FeeType enum values")
    void mapsAllFeeTypes() {
      for (FeeType type : FeeType.values()) {
        PropertyFeesRecord record = createCompleteRecord();
        record.setFeeType(type.name());

        Optional<PropertyFee> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getFeeType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all FeeStatus enum values")
    void mapsAllFeeStatuses() {
      for (FeeStatus status : FeeStatus.values()) {
        PropertyFeesRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<PropertyFee> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyFeesRecord record = createCompleteRecord();
      record.setName(null);
      record.setDueMonths(null);
      record.setStartDate(null);
      record.setEndDate(null);
      record.setNotes(null);

      Optional<PropertyFee> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyFee fee = result.get();
      assertThat(fee.getName()).isEmpty();
      assertThat(fee.getDueMonths()).isEmpty();
      assertThat(fee.getStartDate()).isEmpty();
      assertThat(fee.getEndDate()).isEmpty();
      assertThat(fee.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyFeesRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyFee> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyFeesRecord createCompleteRecord() {
    PropertyFeesRecord record = new PropertyFeesRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setFeeType("HOA");
    record.setName("Monthly HOA");
    record.setAnnualAmount(new BigDecimal("3600.00"));
    record.setCurrency("EUR");
    record.setPaymentFrequency("MONTHLY");
    record.setDueMonths("1,2,3,4,5,6,7,8,9,10,11,12");
    record.setStartDate(LocalDate.of(2026, 1, 1));
    record.setEndDate(LocalDate.of(2026, 12, 31));
    record.setStatus("ACTIVE");
    record.setNotes("HOA fees");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
