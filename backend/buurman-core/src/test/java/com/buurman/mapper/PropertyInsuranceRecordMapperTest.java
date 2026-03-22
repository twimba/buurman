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

import com.buurman.domain.PropertyInsurance;
import com.buurman.domain.PropertyInsurance.InsuranceStatus;
import com.buurman.domain.PropertyInsurance.InsuranceType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyInsurancesRecord;

@DisplayName("PropertyInsuranceRecordMapper")
class PropertyInsuranceRecordMapperTest {

  private final PropertyInsuranceRecordMapper mapper = new PropertyInsuranceRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PIN01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyInsurance> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyInsurancesRecord record = createCompleteRecord();

      Optional<PropertyInsurance> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyInsurance ins = result.get();
      assertThat(ins.getId()).isEqualTo(ID);
      assertThat(ins.getIdentifier()).contains(IDENTIFIER);
      assertThat(ins.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(ins.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(ins.getInsuranceType()).isEqualTo(InsuranceType.BUILDING);
      assertThat(ins.getProvider()).contains("Allianz");
      assertThat(ins.getPolicyNumber()).contains("POL-2026-001");
      assertThat(ins.getCoverageAmount()).isPresent();
      assertThat(ins.getCoverageAmount().get().value())
          .isEqualByComparingTo(new BigDecimal("500000.00"));
      assertThat(ins.getCoverageAmount().get().currency()).isEqualTo("EUR");
      assertThat(ins.getAnnualPremium().value()).isEqualByComparingTo(new BigDecimal("2400.00"));
      assertThat(ins.getAnnualPremium().currency()).isEqualTo("EUR");
      assertThat(ins.getPaymentFrequency()).isEqualTo("MONTHLY");
      assertThat(ins.getStartDate()).contains(LocalDate.of(2026, 1, 1));
      assertThat(ins.getEndDate()).contains(LocalDate.of(2027, 1, 1));
      assertThat(ins.getStatus()).isEqualTo(InsuranceStatus.ACTIVE);
      assertThat(ins.getNotes()).contains("Building insurance");
      assertThat(ins.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all InsuranceType enum values")
    void mapsAllInsuranceTypes() {
      for (InsuranceType type : InsuranceType.values()) {
        PropertyInsurancesRecord record = createCompleteRecord();
        record.setInsuranceType(type.name());

        Optional<PropertyInsurance> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getInsuranceType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all InsuranceStatus enum values")
    void mapsAllInsuranceStatuses() {
      for (InsuranceStatus status : InsuranceStatus.values()) {
        PropertyInsurancesRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<PropertyInsurance> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("returns empty Optional for coverage amount when null")
    void returnsEmptyCoverageWhenNull() {
      PropertyInsurancesRecord record = createCompleteRecord();
      record.setCoverageAmount(null);
      record.setCoverageAmountCurrency(null);

      Optional<PropertyInsurance> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCoverageAmount()).isEmpty();
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyInsurancesRecord record = createCompleteRecord();
      record.setProvider(null);
      record.setPolicyNumber(null);
      record.setStartDate(null);
      record.setEndDate(null);
      record.setNotes(null);

      Optional<PropertyInsurance> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyInsurance ins = result.get();
      assertThat(ins.getProvider()).isEmpty();
      assertThat(ins.getPolicyNumber()).isEmpty();
      assertThat(ins.getStartDate()).isEmpty();
      assertThat(ins.getEndDate()).isEmpty();
      assertThat(ins.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyInsurancesRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyInsurance> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyInsurancesRecord createCompleteRecord() {
    PropertyInsurancesRecord record = new PropertyInsurancesRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setInsuranceType("BUILDING");
    record.setProvider("Allianz");
    record.setPolicyNumber("POL-2026-001");
    record.setCoverageAmount(new BigDecimal("500000.00"));
    record.setCoverageAmountCurrency("EUR");
    record.setAnnualPremium(new BigDecimal("2400.00"));
    record.setAnnualPremiumCurrency("EUR");
    record.setPaymentFrequency("MONTHLY");
    record.setStartDate(LocalDate.of(2026, 1, 1));
    record.setEndDate(LocalDate.of(2027, 1, 1));
    record.setStatus("ACTIVE");
    record.setNotes("Building insurance");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
