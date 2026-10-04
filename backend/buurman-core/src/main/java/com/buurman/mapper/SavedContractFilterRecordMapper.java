package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.jooq.JSONB;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.SavedContractFilter;
import com.buurman.jooq.generated.tables.records.SavedContractFiltersRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * No existing {@code RecordMapper} in this codebase is a MapStruct abstract class with an injected
 * collaborator (see {@code DocumentRecordMapper} for the interface + default-method precedent this
 * follows); the {@code ObjectMapper} needed for JSONB↔Map conversion is instead passed explicitly
 * into the default method by the repository, which already has one injected.
 */
@Mapper(componentModel = "spring")
public interface SavedContractFilterRecordMapper {

  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(target = "criteria", ignore = true)
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  SavedContractFilter toDomainWithoutCriteria(SavedContractFiltersRecord record);

  default SavedContractFilter toDomain(
      SavedContractFiltersRecord record, ObjectMapper objectMapper) {
    SavedContractFilter filter = toDomainWithoutCriteria(record);
    filter.setCriteria(toCriteriaMap(record.getCriteria(), objectMapper));
    return filter;
  }

  default Map<String, Object> toCriteriaMap(JSONB criteria, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(criteria.data(), new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize saved filter criteria", e);
    }
  }

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
