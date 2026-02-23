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
          "java(Optional.ofNullable(record.getDeletedAt()).map(dt ->"
              + " dt.toInstant(java.time.ZoneOffset.UTC)))")
  @Mapping(target = "title", expression = "java(Optional.ofNullable(record.getTitle()))")
  @Mapping(target = "notes", expression = "java(Optional.ofNullable(record.getNotes()))")
  Document toDomain(DocumentsRecord record);

  @Mapping(target = "uploadedAt", expression = "java(toLocalDateTime(document.getUploadedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(document.getDeletedAt().map(i -> java.time.LocalDateTime.ofInstant(i,"
              + " java.time.ZoneOffset.UTC)).orElse(null))")
  @Mapping(target = "title", expression = "java(document.getTitle().orElse(null))")
  @Mapping(target = "notes", expression = "java(document.getNotes().orElse(null))")
  DocumentsRecord toRecord(Document document);

  List<Document> toDomainList(List<DocumentsRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
