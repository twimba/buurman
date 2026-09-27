package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.domain.Contract.ContractStatus;
import com.buurman.domain.Contract.ContractType;
import com.buurman.domain.Contract.LandlordType;
import com.buurman.domain.Contract.PaymentFrequency;
import com.buurman.domain.Contract.RenewalMode;
import com.buurman.domain.ContractExtension.RentAdjustmentType;
import com.buurman.domain.Sid;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.domain.metadata.NlContractMetadata;
import com.buurman.jooq.generated.tables.records.ContractsRecord;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractRecordMapper")
class ContractRecordMapperTest {

  @Mock private CountryMetadataSerializer countryMetadataSerializer;
  @InjectMocks private ContractRecordMapper mapper;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID UNIT_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("CON01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<Contract> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      ContractsRecord record = createCompleteRecord();

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Contract contract = result.get();
      assertThat(contract.getId()).isEqualTo(ID);
      assertThat(contract.getIdentifier()).contains(IDENTIFIER);
      assertThat(contract.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(contract.getPropertyId()).isEqualTo(PROPERTY_ID);
      // Distinct from PROPERTY_ID: catches a setUnitId(record.getPropertyId()) copy-paste bug
      // (BUUR-106 follow-up register, section F, item 8).
      assertThat(contract.getUnitId()).isEqualTo(UNIT_ID);
      assertThat(contract.getContractType()).isEqualTo(ContractType.FIXED_TERM);
      assertThat(contract.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
      assertThat(contract.getEndDate()).contains(LocalDate.of(2027, 1, 1));
      assertThat(contract.getSignedDate()).contains(LocalDate.of(2025, 12, 15));
      assertThat(contract.getRentAmount().value()).isEqualByComparingTo(new BigDecimal("1200.00"));
      assertThat(contract.getRentAmount().currency()).isEqualTo("EUR");
      assertThat(contract.getPaymentFrequency()).isEqualTo(PaymentFrequency.MONTHLY);
      assertThat(contract.getPaymentDueDay()).contains(1);
      assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
      assertThat(contract.getTerminationNoticeDays()).isEqualTo(60);
      assertThat(contract.getLateFeePercentage()).contains(new BigDecimal("5.00"));
      assertThat(contract.getTermsAndConditions()).contains("Standard terms");
      assertThat(contract.getNotes()).contains("Test note");
      assertThat(contract.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(contract.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(contract.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(contract.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(contract.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all ContractType values")
    void mapsAllContractTypeValues() {
      for (ContractType type : ContractType.values()) {
        ContractsRecord record = createCompleteRecord();
        record.setContractType(type.name());

        Optional<Contract> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getContractType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all PaymentFrequency values")
    void mapsAllPaymentFrequencyValues() {
      for (PaymentFrequency frequency : PaymentFrequency.values()) {
        ContractsRecord record = createCompleteRecord();
        record.setPaymentFrequency(frequency.name());

        Optional<Contract> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentFrequency()).isEqualTo(frequency);
      }
    }

    @Test
    @DisplayName("maps all ContractStatus values")
    void mapsAllContractStatusValues() {
      for (ContractStatus status : ContractStatus.values()) {
        ContractsRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<Contract> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("maps all RenewalMode values")
    void mapsAllRenewalModeValues() {
      for (RenewalMode mode : RenewalMode.values()) {
        ContractsRecord record = createCompleteRecord();
        record.setRenewalMode(mode.name());

        Optional<Contract> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getRenewalMode()).isEqualTo(mode);
      }
    }

    @Test
    @DisplayName("maps all RentAdjustmentType values")
    void mapsAllRentAdjustmentTypeValues() {
      for (RentAdjustmentType type : RentAdjustmentType.values()) {
        ContractsRecord record = createCompleteRecord();
        record.setRentAdjustmentType(type.name());

        Optional<Contract> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getRentAdjustmentType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("defaults terminationNoticeDays to 30 when null")
    void defaultsTerminationNoticeDays() {
      ContractsRecord record = createCompleteRecord();
      record.setTerminationNoticeDays(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getTerminationNoticeDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("wraps nullable fields in Optional.empty when null")
    void wrapsNullableFieldsAsEmpty() {
      ContractsRecord record = createCompleteRecord();
      record.setEndDate(null);
      record.setSignedDate(null);
      record.setPaymentDueDay(null);
      record.setLateFeePercentage(null);
      record.setTermsAndConditions(null);
      record.setNotes(null);
      record.setRenewalTermMonths(null);
      record.setMaxRenewals(null);
      record.setRentAdjustmentValue(null);
      record.setLandlordType(null);
      record.setRegionCode(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Contract contract = result.get();
      assertThat(contract.getEndDate()).isEmpty();
      assertThat(contract.getSignedDate()).isEmpty();
      assertThat(contract.getPaymentDueDay()).isEmpty();
      assertThat(contract.getLateFeePercentage()).isEmpty();
      assertThat(contract.getTermsAndConditions()).isEmpty();
      assertThat(contract.getNotes()).isEmpty();
      assertThat(contract.getRenewalTermMonths()).isEmpty();
      assertThat(contract.getMaxRenewals()).isEmpty();
      assertThat(contract.getRentAdjustmentValue()).isEmpty();
      assertThat(contract.getLandlordType()).isEmpty();
      assertThat(contract.getRegionCode()).isEmpty();
    }

    @Test
    @DisplayName("maps landlordType string to enum via Optional")
    void mapsLandlordType() {
      ContractsRecord record = createCompleteRecord();
      record.setLandlordType("NATURAL_PERSON");

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getLandlordType()).contains(LandlordType.NATURAL_PERSON);
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      ContractsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("maps renewal configuration fields")
    void mapsRenewalFields() {
      ContractsRecord record = createCompleteRecord();
      record.setRenewalMode("AUTOMATIC");
      record.setRenewalTermMonths(12);
      record.setMaxRenewals(3);
      record.setLandlordNoticeDays(90);
      record.setTenantNoticeDays(60);
      record.setRequiresTenantConfirmation(true);
      record.setRentAdjustmentType("FIXED_AMOUNT");
      record.setRentAdjustmentValue(new BigDecimal("50.00"));

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Contract contract = result.get();
      assertThat(contract.getRenewalMode()).isEqualTo(RenewalMode.AUTOMATIC);
      assertThat(contract.getRenewalTermMonths()).contains(12);
      assertThat(contract.getMaxRenewals()).contains(3);
      assertThat(contract.getLandlordNoticeDays()).isEqualTo(90);
      assertThat(contract.getTenantNoticeDays()).isEqualTo(60);
      assertThat(contract.getRequiresTenantConfirmation()).isTrue();
      assertThat(contract.getRentAdjustmentType()).isEqualTo(RentAdjustmentType.FIXED_AMOUNT);
      assertThat(contract.getRentAdjustmentValue()).contains(new BigDecimal("50.00"));
    }
  }

  @Nested
  @DisplayName("country metadata deserialization")
  class CountryMetadataMapping {

    @Test
    @DisplayName("skips metadata deserialization when country_metadata is null")
    void skipsSerializerWhenMetadataNull() {
      // createCompleteRecord() sets countryCode="NL" but countryMetadata=null.
      // The mapper guard: if (metadataJsonb != null) skips deserialization.
      ContractsRecord record = createCompleteRecord();

      Optional<Contract> result = mapper.toDomain(record);

      verify(countryMetadataSerializer, never()).deserialize(anyString(), anyString());
      assertThat(result).isPresent();
      assertThat(result.get().getCountryMetadata()).isEmpty();
    }

    @Test
    @DisplayName("handles null countryCode without NPE")
    void handlesNullCountryCodeWithoutNpe() {
      ContractsRecord record = createCompleteRecord();
      record.setCountryCode(null);
      record.setCountryMetadata(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCountryCode()).isEmpty();
      assertThat(result.get().getCountryMetadata()).isEmpty();
      verify(countryMetadataSerializer, never()).deserialize(anyString(), anyString());
    }

    @Test
    @DisplayName("sets countryCode Optional from record")
    void setsCountryCodeOptional() {
      ContractsRecord record = createCompleteRecord();

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCountryCode()).contains("NL");
    }

    @Test
    @DisplayName("deserializes metadata via serializer when country_code and metadata are set")
    void deserializesMetadataWhenCountryCodePresent() {
      // This test verifies the serializer is called with the correct arguments
      // when the record has both country_code and country_metadata set.
      // The DSL.field() accessor on a typed record should resolve the column value
      // if the column exists in the record's schema.
      ContractsRecord record = createCompleteRecord();
      record.setCountryCode("NL");
      record.setCountryMetadata(JSONB.jsonb("{\"sectorClassification\":\"FREE\"}"));

      NlContractMetadata nlMetadata =
          new NlContractMetadata(
              "FREE", 200, true, false, true, true, true, false, false, null, true, null, "A");
      when(countryMetadataSerializer.deserialize("{\"sectorClassification\":\"FREE\"}", "NL"))
          .thenReturn(nlMetadata);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCountryCode()).contains("NL");
      assertThat(result.get().getCountryMetadata()).contains(nlMetadata);
    }

    @Test
    @DisplayName("sets empty country metadata when serializer returns null")
    void setsEmptyMetadataWhenSerializerReturnsNull() {
      ContractsRecord record = createCompleteRecord();
      record.setCountryCode("XX");
      record.setCountryMetadata(JSONB.jsonb("{}"));

      when(countryMetadataSerializer.deserialize("{}", "XX")).thenReturn(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCountryCode()).contains("XX");
      // contract.setCountryMetadata(Optional.ofNullable(null)) => Optional.empty()
      assertThat(result.get().getCountryMetadata()).isEmpty();
    }
  }

  @Nested
  @DisplayName("deposit and security deposit mapping")
  class MoneyAmountMapping {

    @Test
    @DisplayName("maps deposit amount when present")
    void mapsDepositAmount() {
      ContractsRecord record = createCompleteRecord();
      record.setDepositAmount(new BigDecimal("2400.00"));
      record.setDepositAmountCurrency("EUR");

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDepositAmount()).isPresent();
      assertThat(result.get().getDepositAmount().get().value())
          .isEqualByComparingTo(new BigDecimal("2400.00"));
      assertThat(result.get().getDepositAmount().get().currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("returns empty Optional when deposit amount is null")
    void returnsEmptyWhenDepositNull() {
      ContractsRecord record = createCompleteRecord();
      record.setDepositAmount(null);
      record.setDepositAmountCurrency(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDepositAmount()).isEmpty();
    }

    @Test
    @DisplayName("returns empty Optional when deposit has amount but null currency")
    void returnsEmptyWhenDepositAmountPresentButCurrencyNull() {
      ContractsRecord record = createCompleteRecord();
      record.setDepositAmount(new BigDecimal("2400.00"));
      record.setDepositAmountCurrency(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDepositAmount()).isEmpty();
    }

    @Test
    @DisplayName("returns empty Optional when deposit has null amount but present currency")
    void returnsEmptyWhenDepositAmountNullButCurrencyPresent() {
      ContractsRecord record = createCompleteRecord();
      record.setDepositAmount(null);
      record.setDepositAmountCurrency("EUR");

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDepositAmount()).isEmpty();
    }

    @Test
    @DisplayName("returns empty Optional when security deposit is null")
    void returnsEmptyWhenSecurityDepositNull() {
      ContractsRecord record = createCompleteRecord();
      record.setSecurityDeposit(null);
      record.setSecurityDepositCurrency(null);

      Optional<Contract> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getSecurityDeposit()).isEmpty();
    }
  }

  /**
   * Creates a ContractsRecord with all required fields populated. Nullable fields default to
   * non-null values so individual tests can selectively null them out.
   */
  private ContractsRecord createCompleteRecord() {
    ContractsRecord record = new ContractsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setPropertyId(PROPERTY_ID);
    record.setUnitId(UNIT_ID);
    record.setContractType("FIXED_TERM");
    record.setStartDate(LocalDate.of(2026, 1, 1));
    record.setEndDate(LocalDate.of(2027, 1, 1));
    record.setSignedDate(LocalDate.of(2025, 12, 15));
    record.setRentAmount(new BigDecimal("1200.00"));
    record.setRentAmountCurrency("EUR");
    record.setDepositAmount(null);
    record.setDepositAmountCurrency(null);
    record.setSecurityDeposit(null);
    record.setSecurityDepositCurrency(null);
    record.setPaymentFrequency("MONTHLY");
    record.setPaymentDueDay(1);
    record.setTerminationNoticeDays(60);
    record.setLateFeePercentage(new BigDecimal("5.00"));
    record.setStatus("ACTIVE");
    record.setTermsAndConditions("Standard terms");
    record.setNotes("Test note");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    record.setRenewalMode("NONE");
    record.setRenewalTermMonths(null);
    record.setMaxRenewals(null);
    record.setLandlordNoticeDays(30);
    record.setTenantNoticeDays(30);
    record.setRequiresTenantConfirmation(false);
    record.setRentAdjustmentType("NONE");
    record.setRentAdjustmentValue(null);
    record.setLandlordType(null);
    record.setRegionCode(null);
    record.setCountryCode("NL");
    record.setCountryMetadata(null);
    return record;
  }
}
