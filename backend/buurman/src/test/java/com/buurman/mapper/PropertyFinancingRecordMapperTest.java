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

import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.PropertyFinancing.FinancingStatus;
import com.buurman.domain.PropertyFinancing.FinancingType;
import com.buurman.domain.PropertyFinancing.RateType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyFinancingsRecord;

@DisplayName("PropertyFinancingRecordMapper")
class PropertyFinancingRecordMapperTest {

  private final PropertyFinancingRecordMapper mapper = new PropertyFinancingRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PFI01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyFinancing> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyFinancingsRecord record = createCompleteRecord();

      Optional<PropertyFinancing> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyFinancing fin = result.get();
      assertThat(fin.getId()).isEqualTo(ID);
      assertThat(fin.getIdentifier()).contains(IDENTIFIER);
      assertThat(fin.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(fin.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(fin.getFinancingType()).isEqualTo(FinancingType.MORTGAGE);
      assertThat(fin.getRateType()).isEqualTo(RateType.FIXED);
      assertThat(fin.getLenderName()).contains("ING Bank");
      assertThat(fin.getLoanNumber()).contains("LOAN-2026-001");
      assertThat(fin.getOriginalAmount().value()).isEqualByComparingTo(new BigDecimal("300000.00"));
      assertThat(fin.getOriginalAmount().currency()).isEqualTo("EUR");
      assertThat(fin.getCurrentBalance()).contains(new BigDecimal("280000.00"));
      assertThat(fin.getInterestRate()).contains(new BigDecimal("3.50"));
      assertThat(fin.getMonthlyPayment()).contains(new BigDecimal("1500.00"));
      assertThat(fin.isPaymentVariable()).isFalse();
      assertThat(fin.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
      assertThat(fin.getEndDate()).contains(LocalDate.of(2054, 1, 1));
      assertThat(fin.getTermMonths()).contains(360);
      assertThat(fin.getStatus()).isEqualTo(FinancingStatus.ACTIVE);
      assertThat(fin.getNotes()).contains("Primary mortgage");
      assertThat(fin.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all FinancingType enum values")
    void mapsAllFinancingTypes() {
      for (FinancingType type : FinancingType.values()) {
        PropertyFinancingsRecord record = createCompleteRecord();
        record.setFinancingType(type.name());

        Optional<PropertyFinancing> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getFinancingType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all RateType enum values")
    void mapsAllRateTypes() {
      for (RateType type : RateType.values()) {
        PropertyFinancingsRecord record = createCompleteRecord();
        record.setRateType(type.name());

        Optional<PropertyFinancing> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getRateType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all FinancingStatus enum values")
    void mapsAllFinancingStatuses() {
      for (FinancingStatus status : FinancingStatus.values()) {
        PropertyFinancingsRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<PropertyFinancing> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("paymentVariable defaults to false when record value is null")
    void paymentVariableDefaultsFalseWhenNull() {
      PropertyFinancingsRecord record = createCompleteRecord();
      record.setPaymentVariable(null);

      Optional<PropertyFinancing> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().isPaymentVariable()).isFalse();
    }

    @Test
    @DisplayName("paymentVariable maps true correctly")
    void paymentVariableMapsTrue() {
      PropertyFinancingsRecord record = createCompleteRecord();
      record.setPaymentVariable(true);

      Optional<PropertyFinancing> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().isPaymentVariable()).isTrue();
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyFinancingsRecord record = createCompleteRecord();
      record.setLenderName(null);
      record.setLoanNumber(null);
      record.setCurrentBalance(null);
      record.setInterestRate(null);
      record.setMonthlyPayment(null);
      record.setEndDate(null);
      record.setTermMonths(null);
      record.setNotes(null);

      Optional<PropertyFinancing> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyFinancing fin = result.get();
      assertThat(fin.getLenderName()).isEmpty();
      assertThat(fin.getLoanNumber()).isEmpty();
      assertThat(fin.getCurrentBalance()).isEmpty();
      assertThat(fin.getInterestRate()).isEmpty();
      assertThat(fin.getMonthlyPayment()).isEmpty();
      assertThat(fin.getEndDate()).isEmpty();
      assertThat(fin.getTermMonths()).isEmpty();
      assertThat(fin.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyFinancingsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyFinancing> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyFinancingsRecord createCompleteRecord() {
    PropertyFinancingsRecord record = new PropertyFinancingsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setFinancingType("MORTGAGE");
    record.setRateType("FIXED");
    record.setLenderName("ING Bank");
    record.setLoanNumber("LOAN-2026-001");
    record.setOriginalAmount(new BigDecimal("300000.00"));
    record.setOriginalAmountCurrency("EUR");
    record.setCurrentBalance(new BigDecimal("280000.00"));
    record.setInterestRate(new BigDecimal("3.50"));
    record.setMonthlyPayment(new BigDecimal("1500.00"));
    record.setPaymentVariable(false);
    record.setStartDate(LocalDate.of(2024, 1, 1));
    record.setEndDate(LocalDate.of(2054, 1, 1));
    record.setTermMonths(360);
    record.setStatus("ACTIVE");
    record.setNotes("Primary mortgage");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
