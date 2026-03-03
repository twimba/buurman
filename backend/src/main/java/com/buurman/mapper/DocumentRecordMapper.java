package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Document;
import com.buurman.jooq.generated.tables.records.DocumentsRecord;

@Mapper(componentModel = "spring")
public interface DocumentRecordMapper {

  @Mapping(target = "uploadedAt", expression = "java(toInstant(record.getUploadedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(java.util.Optional.ofNullable(record.getDeletedAt()).map(dt ->"
              + " dt.toInstant(java.time.ZoneOffset.UTC)))")
  @Mapping(target = "title", expression = "java(java.util.Optional.ofNullable(record.getTitle()))")
  @Mapping(target = "notes", expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  @Mapping(target = "identifier", expression = "java(java.util.Optional.of(record.getIdentifier()))")
  Document toDomain(DocumentsRecord record);

  @Mapping(target = "uploadedAt", expression = "java(toLocalDateTime(document.getUploadedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(document.getDeletedAt().map(i -> java.time.LocalDateTime.ofInstant(i,"
              + " java.time.ZoneOffset.UTC)).orElse(null))")
  @Mapping(target = "title", expression = "java(document.getTitle().orElse(null))")
  @Mapping(target = "notes", expression = "java(document.getNotes().orElse(null))")
  @Mapping(target = "identifier", expression = "java(document.getIdentifier().orElse(null))")
  DocumentsRecord toRecord(Document document);

  List<Document> toDomainList(List<DocumentsRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
