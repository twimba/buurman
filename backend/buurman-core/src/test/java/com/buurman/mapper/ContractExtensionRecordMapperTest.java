package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractExtension.ExtensionStatus;
import com.buurman.domain.ContractExtension.RentAdjustmentType;
import com.buurman.domain.ContractExtension.TriggerType;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.ContractExtensionsRecord;

@DisplayName("ContractExtensionRecordMapper")
class ContractExtensionRecordMapperTest {

  private final ContractExtensionRecordMapper mapper = new ContractExtensionRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID RENT_PERIOD_ID = UUID.randomUUID();
  private static final UUID ACTIVATED_BY = UUID.randomUUID();
  private static final UUID CONFIRMED_BY = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("CEX01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      ContractExtensionsRecord record = createCompleteRecord();

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getId()).isEqualTo(ID);
      assertThat(ext.getIdentifier()).contains(IDENTIFIER);
      assertThat(ext.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(ext.getContractId()).isEqualTo(CONTRACT_ID);
      assertThat(ext.getExtensionNumber()).isEqualTo(1);
      assertThat(ext.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
      assertThat(ext.getNewEndDate()).contains(LocalDate.of(2027, 12, 31));
      assertThat(ext.getRentAdjustmentType()).isEqualTo(RentAdjustmentType.FIXED_PERCENTAGE);
      assertThat(ext.getRentAdjustmentValue()).contains(new BigDecimal("3.50"));
      assertThat(ext.getStatus()).isEqualTo(ExtensionStatus.ACTIVE);
      assertThat(ext.getTriggerType()).isEqualTo(TriggerType.AUTO);
      assertThat(ext.getRentPeriodId()).contains(RENT_PERIOD_ID);
      assertThat(ext.getNotes()).contains("Annual extension");
      assertThat(ext.getDeclinedReason()).isEmpty();
      assertThat(ext.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(ext.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(ext.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("converts EUR minor units to major units correctly")
    void convertsEurMinorUnitsToMajor() {
      ContractExtensionsRecord record = createCompleteRecord();
      record.setPreviousRentAmount(120000L);
      record.setPreviousRentCurrency("EUR");
      record.setNewRentAmount(123600L);
      record.setNewRentCurrency("EUR");

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getPreviousRentAmount().value())
          .isEqualByComparingTo(new BigDecimal("1200.00"));
      assertThat(ext.getPreviousRentAmount().currency()).isEqualTo("EUR");
      assertThat(ext.getNewRentAmount().value()).isEqualByComparingTo(new BigDecimal("1236.00"));
      assertThat(ext.getNewRentAmount().currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("converts JPY minor units to major units (0 decimal places)")
    void convertsJpyMinorUnits() {
      ContractExtensionsRecord record = createCompleteRecord();
      record.setPreviousRentAmount(100000L);
      record.setPreviousRentCurrency("JPY");
      record.setNewRentAmount(103000L);
      record.setNewRentCurrency("JPY");

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getPreviousRentAmount().value())
          .isEqualByComparingTo(new BigDecimal("100000"));
      assertThat(ext.getNewRentAmount().value()).isEqualByComparingTo(new BigDecimal("103000"));
    }

    @Test
    @DisplayName("maps all ExtensionStatus enum values")
    void mapsAllExtensionStatuses() {
      for (ExtensionStatus status : ExtensionStatus.values()) {
        ContractExtensionsRecord record = createCompleteRecord();
        record.setStatus(status.name());

        ContractExtension ext = mapper.toDomain(record);

        assertThat(ext.getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("maps all RentAdjustmentType enum values")
    void mapsAllRentAdjustmentTypes() {
      for (RentAdjustmentType type : RentAdjustmentType.values()) {
        ContractExtensionsRecord record = createCompleteRecord();
        record.setRentAdjustmentType(type.name());

        ContractExtension ext = mapper.toDomain(record);

        assertThat(ext.getRentAdjustmentType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all TriggerType enum values")
    void mapsAllTriggerTypes() {
      for (TriggerType type : TriggerType.values()) {
        ContractExtensionsRecord record = createCompleteRecord();
        record.setTriggerType(type.name());

        ContractExtension ext = mapper.toDomain(record);

        assertThat(ext.getTriggerType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      ContractExtensionsRecord record = createCompleteRecord();
      record.setNewEndDate(null);
      record.setRentAdjustmentValue(null);
      record.setRentPeriodId(null);
      record.setNotes(null);
      record.setDeclinedReason(null);
      record.setActivatedAt(null);
      record.setActivatedBy(null);
      record.setConfirmedAt(null);
      record.setConfirmedBy(null);
      record.setSupersededAt(null);

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getNewEndDate()).isEmpty();
      assertThat(ext.getRentAdjustmentValue()).isEmpty();
      assertThat(ext.getRentPeriodId()).isEmpty();
      assertThat(ext.getNotes()).isEmpty();
      assertThat(ext.getDeclinedReason()).isEmpty();
      assertThat(ext.getActivatedAt()).isEmpty();
      assertThat(ext.getActivatedBy()).isEmpty();
      assertThat(ext.getConfirmedAt()).isEmpty();
      assertThat(ext.getConfirmedBy()).isEmpty();
      assertThat(ext.getSupersededAt()).isEmpty();
    }

    @Test
    @DisplayName("maps timestamp Optional fields when present")
    void mapsTimestampOptionals() {
      ContractExtensionsRecord record = createCompleteRecord();
      LocalDateTime activatedAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0);
      LocalDateTime confirmedAt = LocalDateTime.of(2026, 4, 2, 11, 0, 0);
      LocalDateTime supersededAt = LocalDateTime.of(2027, 1, 1, 0, 0, 0);
      record.setActivatedAt(activatedAt);
      record.setActivatedBy(ACTIVATED_BY);
      record.setConfirmedAt(confirmedAt);
      record.setConfirmedBy(CONFIRMED_BY);
      record.setSupersededAt(supersededAt);

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getActivatedAt()).contains(activatedAt.toInstant(ZoneOffset.UTC));
      assertThat(ext.getActivatedBy()).contains(ACTIVATED_BY);
      assertThat(ext.getConfirmedAt()).contains(confirmedAt.toInstant(ZoneOffset.UTC));
      assertThat(ext.getConfirmedBy()).contains(CONFIRMED_BY);
      assertThat(ext.getSupersededAt()).contains(supersededAt.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      ContractExtensionsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      ContractExtension ext = mapper.toDomain(record);

      assertThat(ext.getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private ContractExtensionsRecord createCompleteRecord() {
    ContractExtensionsRecord record = new ContractExtensionsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setContractId(CONTRACT_ID);
    record.setExtensionNumber(1);
    record.setPreviousEndDate(LocalDate.of(2026, 12, 31));
    record.setNewEndDate(LocalDate.of(2027, 12, 31));
    record.setPreviousRentAmount(120000L);
    record.setPreviousRentCurrency("EUR");
    record.setNewRentAmount(123600L);
    record.setNewRentCurrency("EUR");
    record.setRentAdjustmentType("FIXED_PERCENTAGE");
    record.setRentAdjustmentValue(new BigDecimal("3.50"));
    record.setStatus("ACTIVE");
    record.setTriggerType("AUTO");
    record.setRentPeriodId(RENT_PERIOD_ID);
    record.setNotes("Annual extension");
    record.setDeclinedReason(null);
    record.setActivatedAt(null);
    record.setActivatedBy(null);
    record.setConfirmedAt(null);
    record.setConfirmedBy(null);
    record.setSupersededAt(null);
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
