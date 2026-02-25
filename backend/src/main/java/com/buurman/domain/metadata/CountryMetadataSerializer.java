package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CountryMetadataSerializer {

  private final ObjectMapper objectMapper;

  public String serialize(ContractCountryMetadata metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
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
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Failed to deserialize country metadata for " + countryCode, e);
    }
  }

  private static final int MAX_METADATA_JSON_SIZE = 65_536; // 64 KB

  /**
   * Deserializes a raw Map/Object (from a request DTO) into a typed metadata record by
   * round-tripping through JSON.
   */
  public @Nullable ContractCountryMetadata deserializeFromMap(
      @Nullable Object rawMetadata, @Nullable String countryCode) {
    if (rawMetadata == null || countryCode == null) {
      return null;
    }
    try {
      byte[] jsonBytes = objectMapper.writeValueAsBytes(rawMetadata);
      if (jsonBytes.length > MAX_METADATA_JSON_SIZE) {
        throw new IllegalArgumentException(
            "Country metadata exceeds maximum size of " + MAX_METADATA_JSON_SIZE + " bytes");
      }
      String json = new String(jsonBytes, StandardCharsets.UTF_8);
      return deserialize(json, countryCode);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Failed to deserialize country metadata map for " + countryCode, e);
    }
  }
}
