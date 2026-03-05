package com.buurman.domain.metadata;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class CountryMetadataSerializer {

  /** Spring-managed ObjectMapper for API-facing operations (major units). */
  private final ObjectMapper apiObjectMapper;

  /**
   * Dedicated ObjectMapper for JSONB storage. Registers {@link MoneyAmountJsonbModule} to
   * serialize/deserialize {@link com.buurman.util.MoneyAmount} as minor units.
   */
  private final ObjectMapper jsonbObjectMapper;

  public CountryMetadataSerializer(ObjectMapper apiObjectMapper) {
    this.apiObjectMapper = apiObjectMapper;
    this.jsonbObjectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addModule(new Jdk8Module())
            .addModule(new MoneyAmountJsonbModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    this.jsonbObjectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
  }

  public String serialize(ContractCountryMetadata metadata) {
    try {
      return jsonbObjectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize country metadata", e);
    }
  }

  public @Nullable ContractCountryMetadata deserialize(
      @Nullable String json, @Nullable String countryCode) {
    if (json == null || json.isBlank() || countryCode == null) {
      return null;
    }
    Class<? extends ContractCountryMetadata> clazz =
        CountryMetadataRegistry.getSchemaClass(countryCode);
    try {
      return jsonbObjectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Failed to deserialize country metadata for " + countryCode, e);
    }
  }

  private static final int MAX_METADATA_JSON_SIZE = 65_536; // 64 KB

  /**
   * Deserializes a raw Map/Object (from a request DTO) into a typed metadata record. The input
   * arrives from the API in major units; this method round-trips through JSON via the JSONB
   * ObjectMapper to produce the correctly typed record.
   */
  public @Nullable ContractCountryMetadata deserializeFromMap(
      @Nullable Object rawMetadata, @Nullable String countryCode) {
    if (rawMetadata == null || countryCode == null) {
      return null;
    }
    try {
      // Round-trip: API ObjectMapper writes the map as JSON (major-unit MoneyAmount),
      // then JSONB ObjectMapper reads it back as a typed record.
      // Since the API input contains {value: <major>, currency: "..."}, we use the API
      // ObjectMapper to write and the API ObjectMapper to read (major units from frontend).
      byte[] jsonBytes = apiObjectMapper.writeValueAsBytes(rawMetadata);
      if (jsonBytes.length > MAX_METADATA_JSON_SIZE) {
        throw new IllegalArgumentException(
            "Country metadata exceeds maximum size of " + MAX_METADATA_JSON_SIZE + " bytes");
      }
      String json = new String(jsonBytes, StandardCharsets.UTF_8);
      // Use API ObjectMapper for reading too — the input is in major units (from frontend),
      // which matches MoneyAmount's default record deserialization.
      Class<? extends ContractCountryMetadata> clazz =
          CountryMetadataRegistry.getSchemaClass(countryCode);
      return apiObjectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Failed to deserialize country metadata map for " + countryCode, e);
    }
  }
}
