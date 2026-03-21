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

import com.buurman.domain.PropertyAcquisition;
import com.buurman.domain.PropertyAcquisition.AcquisitionType;
import com.buurman.domain.PropertyAcquisition.DepreciationMethod;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.PropertyAcquisitionsRecord;

@DisplayName("PropertyAcquisitionRecordMapper")
class PropertyAcquisitionRecordMapperTest {

  private final PropertyAcquisitionRecordMapper mapper = new PropertyAcquisitionRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("PAC01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<PropertyAcquisition> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      PropertyAcquisitionsRecord record = createCompleteRecord();

      Optional<PropertyAcquisition> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyAcquisition acq = result.get();
      assertThat(acq.getId()).isEqualTo(ID);
      assertThat(acq.getIdentifier()).contains(IDENTIFIER);
      assertThat(acq.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(acq.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(acq.getAcquisitionType()).isEqualTo(AcquisitionType.PURCHASE);
      assertThat(acq.getAcquisitionDate()).contains(LocalDate.of(2024, 6, 1));
      assertThat(acq.getPurchasePrice()).isPresent();
      assertThat(acq.getPurchasePrice().get().value())
          .isEqualByComparingTo(new BigDecimal("350000.00"));
      assertThat(acq.getClosingCosts()).isPresent();
      assertThat(acq.getClosingCosts().get().value())
          .isEqualByComparingTo(new BigDecimal("15000.00"));
      assertThat(acq.getRenovationCosts()).isPresent();
      assertThat(acq.getRenovationCosts().get().value())
          .isEqualByComparingTo(new BigDecimal("25000.00"));
      assertThat(acq.getLandValue()).isPresent();
      assertThat(acq.getLandValue().get().value())
          .isEqualByComparingTo(new BigDecimal("100000.00"));
      assertThat(acq.getDepreciationMethod()).contains(DepreciationMethod.STRAIGHT_LINE);
      assertThat(acq.getDepreciationYears()).contains(30);
      assertThat(acq.getNotes()).contains("Primary acquisition");
      assertThat(acq.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all AcquisitionType enum values")
    void mapsAllAcquisitionTypes() {
      for (AcquisitionType type : AcquisitionType.values()) {
        PropertyAcquisitionsRecord record = createCompleteRecord();
        record.setAcquisitionType(type.name());

        Optional<PropertyAcquisition> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getAcquisitionType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all DepreciationMethod enum values")
    void mapsAllDepreciationMethods() {
      for (DepreciationMethod method : DepreciationMethod.values()) {
        PropertyAcquisitionsRecord record = createCompleteRecord();
        record.setDepreciationMethod(method.name());

        Optional<PropertyAcquisition> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getDepreciationMethod()).contains(method);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      PropertyAcquisitionsRecord record = createCompleteRecord();
      record.setAcquisitionDate(null);
      record.setPurchasePrice(null);
      record.setPurchasePriceCurrency(null);
      record.setClosingCosts(null);
      record.setClosingCostsCurrency(null);
      record.setRenovationCosts(null);
      record.setRenovationCostsCurrency(null);
      record.setLandValue(null);
      record.setLandValueCurrency(null);
      record.setDepreciationMethod(null);
      record.setDepreciationYears(null);
      record.setNotes(null);

      Optional<PropertyAcquisition> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      PropertyAcquisition acq = result.get();
      assertThat(acq.getAcquisitionDate()).isEmpty();
      assertThat(acq.getPurchasePrice()).isEmpty();
      assertThat(acq.getClosingCosts()).isEmpty();
      assertThat(acq.getRenovationCosts()).isEmpty();
      assertThat(acq.getLandValue()).isEmpty();
      assertThat(acq.getDepreciationMethod()).isEmpty();
      assertThat(acq.getDepreciationYears()).isEmpty();
      assertThat(acq.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      PropertyAcquisitionsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<PropertyAcquisition> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private PropertyAcquisitionsRecord createCompleteRecord() {
    PropertyAcquisitionsRecord record = new PropertyAcquisitionsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setPropertyId(PROPERTY_ID);
    record.setTeamId(TEAM_ID);
    record.setAcquisitionType("PURCHASE");
    record.setAcquisitionDate(LocalDate.of(2024, 6, 1));
    record.setPurchasePrice(new BigDecimal("350000.00"));
    record.setPurchasePriceCurrency("EUR");
    record.setClosingCosts(new BigDecimal("15000.00"));
    record.setClosingCostsCurrency("EUR");
    record.setRenovationCosts(new BigDecimal("25000.00"));
    record.setRenovationCostsCurrency("EUR");
    record.setLandValue(new BigDecimal("100000.00"));
    record.setLandValueCurrency("EUR");
    record.setDepreciationMethod("STRAIGHT_LINE");
    record.setDepreciationYears(30);
    record.setNotes("Primary acquisition");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
