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

import com.buurman.domain.PropertyTax;
import com.buurman.domain.PropertyTax.TaxStatus;
import com.buurman.domain.PropertyTax.TaxType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyTaxesRecord;

@DisplayName("PropertyTaxRecordMapper")
class PropertyTaxRecordMapperTest {

  private final PropertyTaxRecordMapper mapper = new PropertyTaxRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PTX01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyTax> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyTaxesRecord record = createCompleteRecord();

      Optional<PropertyTax> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyTax tax = result.get();
      assertThat(tax.getId()).isEqualTo(ID);
      assertThat(tax.getIdentifier()).contains(IDENTIFIER);
      assertThat(tax.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(tax.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(tax.getTaxType()).isEqualTo(TaxType.PROPERTY);
      assertThat(tax.getAuthority()).contains("Municipality of Amsterdam");
      assertThat(tax.getAnnualAmount().value()).isEqualByComparingTo(new BigDecimal("1200.00"));
      assertThat(tax.getAnnualAmount().currency()).isEqualTo("EUR");
      assertThat(tax.getPaymentFrequency()).isEqualTo("MONTHLY");
      assertThat(tax.getDueMonths()).contains("1,4,7,10");
      assertThat(tax.getTaxYear()).contains(2026);
      assertThat(tax.getStartDate()).contains(LocalDate.of(2026, 1, 1));
      assertThat(tax.getEndDate()).contains(LocalDate.of(2026, 12, 31));
      assertThat(tax.getStatus()).isEqualTo(TaxStatus.ACTIVE);
      assertThat(tax.getNotes()).contains("Annual property tax");
      assertThat(tax.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(tax.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(tax.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all TaxType enum values")
    void mapsAllTaxTypes() {
      for (TaxType type : TaxType.values()) {
        PropertyTaxesRecord record = createCompleteRecord();
        record.setTaxType(type.name());

        Optional<PropertyTax> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getTaxType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all TaxStatus enum values")
    void mapsAllTaxStatuses() {
      for (TaxStatus status : TaxStatus.values()) {
        PropertyTaxesRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<PropertyTax> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyTaxesRecord record = createCompleteRecord();
      record.setAuthority(null);
      record.setDueMonths(null);
      record.setTaxYear(null);
      record.setStartDate(null);
      record.setEndDate(null);
      record.setNotes(null);

      Optional<PropertyTax> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyTax tax = result.get();
      assertThat(tax.getAuthority()).isEmpty();
      assertThat(tax.getDueMonths()).isEmpty();
      assertThat(tax.getTaxYear()).isEmpty();
      assertThat(tax.getStartDate()).isEmpty();
      assertThat(tax.getEndDate()).isEmpty();
      assertThat(tax.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyTaxesRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyTax> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt())
          .contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyTaxesRecord createCompleteRecord() {
    PropertyTaxesRecord record = new PropertyTaxesRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setTaxType("PROPERTY");
    record.setAuthority("Municipality of Amsterdam");
    record.setAnnualAmount(new BigDecimal("1200.00"));
    record.setCurrency("EUR");
    record.setPaymentFrequency("MONTHLY");
    record.setDueMonths("1,4,7,10");
    record.setTaxYear(2026);
    record.setStartDate(LocalDate.of(2026, 1, 1));
    record.setEndDate(LocalDate.of(2026, 12, 31));
    record.setStatus("ACTIVE");
    record.setNotes("Annual property tax");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
