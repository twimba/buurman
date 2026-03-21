package com.buurman.domain.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.util.MoneyAmount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@DisplayName("CountryMetadataSerializer")
class CountryMetadataSerializerTest {

  private final ObjectMapper apiObjectMapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .addModule(new Jdk8Module())
          .build();

  private final CountryMetadataSerializer serializer =
      new CountryMetadataSerializer(apiObjectMapper);

  @Nested
  @DisplayName("serialize")
  class Serialize {

    @Test
    @DisplayName("serializes NL metadata with MoneyAmount as minor units")
    void serializesNlMetadataWithMinorUnits() {
      NlContractMetadata metadata =
          new NlContractMetadata(
              "social",
              142,
              true,
              false,
              true,
              true,
              false,
              false,
              false,
              MoneyAmount.of(new BigDecimal("150.00"), "EUR"),
              null,
              MoneyAmount.of(new BigDecimal("879.66"), "EUR"),
              "A+");

      String json = serializer.serialize(metadata);

      assertThat(json).contains("\"sectorClassification\":\"social\"");
      assertThat(json).contains("\"wwsPoints\":142");
      assertThat(json).contains("\"huurcommissieEligible\":true");
      assertThat(json).contains("\"energyLabel\":\"A+\"");
      // MoneyAmount stored as minor units via MoneyAmountJsonbModule
      assertThat(json).contains("\"value\":15000");
      assertThat(json).contains("\"value\":87966");
    }

    @Test
    @DisplayName("serializes GenericContractMetadata")
    void serializesGenericMetadata() {
      GenericContractMetadata metadata =
          new GenericContractMetadata("B", "CERT-123", "REG-456", 3, true, "Some notes");

      String json = serializer.serialize(metadata);

      assertThat(json).contains("\"energyCertificateRating\":\"B\"");
      assertThat(json).contains("\"maxDepositMonths\":3");
      assertThat(json).contains("\"notes\":\"Some notes\"");
    }

    @Test
    @DisplayName("omits null fields (NON_ABSENT inclusion)")
    void omitsNullFields() {
      GenericContractMetadata metadata =
          new GenericContractMetadata("A", null, null, null, null, null);

      String json = serializer.serialize(metadata);

      assertThat(json).contains("\"energyCertificateRating\":\"A\"");
      assertThat(json).doesNotContain("energyCertificateNumber");
      assertThat(json).doesNotContain("contractRegistrationNumber");
      assertThat(json).doesNotContain("maxDepositMonths");
    }
  }

  @Nested
  @DisplayName("deserialize")
  class Deserialize {

    @Test
    @DisplayName("deserializes NL metadata from JSONB with minor-unit MoneyAmount")
    @SuppressWarnings("NullAway")
    void deserializesNlMetadata() {
      String json =
          """
          {"sectorClassification":"social","wwsPoints":142,\
          "totalServiceCostsAmount":{"value":15000,"currency":"EUR"},\
          "energyLabel":"A+"}""";

      ContractCountryMetadata result = serializer.deserialize(json, "NL");

      assertThat(result).isNotNull().isInstanceOf(NlContractMetadata.class);
      NlContractMetadata nl = (NlContractMetadata) result;
      assertThat(nl.sectorClassification()).isEqualTo("social");
      assertThat(nl.wwsPoints()).isEqualTo(142);
      assertThat(nl.energyLabel()).isEqualTo("A+");
      assertThat(nl.totalServiceCostsAmount()).isNotNull();
      assertThat(Objects.requireNonNull(nl.totalServiceCostsAmount()).value())
          .isEqualByComparingTo("150.00");
      assertThat(nl.totalServiceCostsAmount().currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("deserializes GenericContractMetadata for unknown country code")
    @SuppressWarnings("NullAway")
    void deserializesGenericMetadata() {
      String json = """
          {"energyCertificateRating":"C","maxDepositMonths":2}""";

      ContractCountryMetadata result = serializer.deserialize(json, "ZZ");

      assertThat(result).isNotNull().isInstanceOf(GenericContractMetadata.class);
      GenericContractMetadata generic = (GenericContractMetadata) result;
      assertThat(generic.energyCertificateRating()).isEqualTo("C");
      assertThat(generic.maxDepositMonths()).isEqualTo(2);
    }

    @Test
    @DisplayName("returns null for null JSON")
    void returnsNullForNullJson() {
      assertThat(serializer.deserialize(null, "NL")).isNull();
    }

    @Test
    @DisplayName("returns null for blank JSON")
    void returnsNullForBlankJson() {
      assertThat(serializer.deserialize("   ", "NL")).isNull();
    }

    @Test
    @DisplayName("returns null for null countryCode")
    void returnsNullForNullCountryCode() {
      assertThat(serializer.deserialize("{}", null)).isNull();
    }

    @Test
    @DisplayName("throws on malformed JSON")
    void throwsOnMalformedJson() {
      assertThatThrownBy(() -> serializer.deserialize("{invalid", "NL"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed to deserialize country metadata for NL");
    }

    @Test
    @DisplayName("handles lowercase country code")
    @SuppressWarnings("NullAway")
    void handlesLowercaseCountryCode() {
      String json = """
          {"sectorClassification":"free"}""";

      ContractCountryMetadata result = serializer.deserialize(json, "nl");

      assertThat(result).isNotNull().isInstanceOf(NlContractMetadata.class);
    }
  }

  @Nested
  @DisplayName("deserializeFromMap")
  class DeserializeFromMap {

    @Test
    @DisplayName("round-trips a map with major-unit MoneyAmount to typed metadata")
    @SuppressWarnings("NullAway")
    void roundTripsMapToTypedMetadata() {
      Map<String, Object> rawMap =
          Map.of(
              "sectorClassification", "social",
              "wwsPoints", 142,
              "energyLabel", "A+");

      ContractCountryMetadata result = serializer.deserializeFromMap(rawMap, "NL");

      assertThat(result).isNotNull().isInstanceOf(NlContractMetadata.class);
      NlContractMetadata nl = (NlContractMetadata) result;
      assertThat(nl.sectorClassification()).isEqualTo("social");
      assertThat(nl.wwsPoints()).isEqualTo(142);
      assertThat(nl.energyLabel()).isEqualTo("A+");
    }

    @Test
    @DisplayName("returns null for null rawMetadata")
    void returnsNullForNullRawMetadata() {
      assertThat(serializer.deserializeFromMap(null, "NL")).isNull();
    }

    @Test
    @DisplayName("returns null for null countryCode")
    void returnsNullForNullCountryCode() {
      assertThat(serializer.deserializeFromMap(Map.of(), null)).isNull();
    }

    @Test
    @DisplayName("falls back to GenericContractMetadata for unknown country")
    @SuppressWarnings("NullAway")
    void fallsBackToGenericForUnknownCountry() {
      Map<String, Object> rawMap = Map.of("energyCertificateRating", "B", "maxDepositMonths", 3);

      ContractCountryMetadata result = serializer.deserializeFromMap(rawMap, "ZZ");

      assertThat(result).isNotNull().isInstanceOf(GenericContractMetadata.class);
      GenericContractMetadata generic = (GenericContractMetadata) result;
      assertThat(generic.energyCertificateRating()).isEqualTo("B");
      assertThat(generic.maxDepositMonths()).isEqualTo(3);
    }

    @Test
    @DisplayName("throws on oversized metadata payload")
    void throwsOnOversizedPayload() {
      String largeValue = "x".repeat(70_000);
      Map<String, Object> rawMap = Map.of("notes", largeValue);

      assertThatThrownBy(() -> serializer.deserializeFromMap(rawMap, "NL"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exceeds maximum size");
    }
  }

  @Nested
  @DisplayName("serialize → deserialize round-trip")
  class RoundTrip {

    @Test
    @DisplayName("NL metadata survives serialize/deserialize with MoneyAmount precision")
    @SuppressWarnings("NullAway")
    void nlRoundTrip() {
      NlContractMetadata original =
          new NlContractMetadata(
              "free",
              200,
              false,
              true,
              true,
              false,
              true,
              true,
              false,
              MoneyAmount.of(new BigDecimal("99.95"), "EUR"),
              null,
              null,
              "C");

      String json = serializer.serialize(original);
      ContractCountryMetadata restored = serializer.deserialize(json, "NL");

      assertThat(restored).isNotNull().isInstanceOf(NlContractMetadata.class);
      NlContractMetadata nl = (NlContractMetadata) restored;
      assertThat(nl.sectorClassification()).isEqualTo("free");
      assertThat(nl.wwsPoints()).isEqualTo(200);
      assertThat(nl.allInRent()).isTrue();
      assertThat(Objects.requireNonNull(nl.totalServiceCostsAmount()).value())
          .isEqualByComparingTo("99.95");
      assertThat(nl.totalServiceCostsAmount().currency()).isEqualTo("EUR");
      assertThat(nl.liberalizationThreshold()).isNull();
      assertThat(nl.energyLabel()).isEqualTo("C");
    }

    @Test
    @DisplayName("Generic metadata round-trips correctly")
    @SuppressWarnings("NullAway")
    void genericRoundTrip() {
      GenericContractMetadata original =
          new GenericContractMetadata("A", "CERT-001", "REG-999", 6, true, "test notes");

      String json = serializer.serialize(original);
      ContractCountryMetadata restored = serializer.deserialize(json, "ZZ");

      assertThat(restored).isNotNull().isInstanceOf(GenericContractMetadata.class);
      GenericContractMetadata generic = (GenericContractMetadata) restored;
      assertThat(generic.energyCertificateRating()).isEqualTo("A");
      assertThat(generic.energyCertificateNumber()).isEqualTo("CERT-001");
      assertThat(generic.contractRegistrationNumber()).isEqualTo("REG-999");
      assertThat(generic.maxDepositMonths()).isEqualTo(6);
      assertThat(generic.rentIndexationApplicable()).isTrue();
      assertThat(generic.notes()).isEqualTo("test notes");
    }
  }
}
