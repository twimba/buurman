package com.buurman.dto.response;

import java.util.List;
import java.util.Optional;

public record CountryMetadataSchemaResponse(
    String countryCode,
    String countryName,
    boolean hasDedicatedSchema,
    List<FieldSchema> fields,
    List<GroupSchema> groups) {

  public record FieldSchema(
      String name,
      String label,
      FieldType type,
      boolean required,
      List<EnumValue> enumValues,
      ValidationSchema validation,
      String group,
      Optional<String> helpText,
      Optional<String> unit) {}

  public record EnumValue(String value, String label) {}

  public record ValidationSchema(
      Optional<Integer> min, Optional<Integer> max, Optional<String> pattern) {}

  public record GroupSchema(String key, String label) {}

  public enum FieldType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    ENUM
  }
}
