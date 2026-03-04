package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Photo;
import com.buurman.jooq.generated.tables.records.PhotosRecord;

@Mapper(componentModel = "spring")
public interface PhotoRecordMapper {

  @Mapping(target = "uploadedAt", expression = "java(toInstant(record.getUploadedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(java.util.Optional.ofNullable(record.getDeletedAt()).map(dt ->"
              + " dt.toInstant(java.time.ZoneOffset.UTC)))")
  @Mapping(
      target = "isMainPhoto",
      expression = "java(record.getIsMainPhoto() != null ? record.getIsMainPhoto() : false)")
  @Mapping(
      target = "thumbnailFileKey",
      expression = "java(java.util.Optional.ofNullable(record.getThumbnailFileKey()))")
  @Mapping(target = "title", expression = "java(java.util.Optional.ofNullable(record.getTitle()))")
  @Mapping(target = "notes", expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  Photo toDomain(PhotosRecord record);

  @Mapping(target = "uploadedAt", expression = "java(toLocalDateTime(photo.getUploadedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(photo.getDeletedAt().map(i -> java.time.LocalDateTime.ofInstant(i,"
              + " java.time.ZoneOffset.UTC)).orElse(null))")
  @Mapping(
      target = "thumbnailFileKey",
      expression = "java(photo.getThumbnailFileKey().orElse(null))")
  @Mapping(target = "title", expression = "java(photo.getTitle().orElse(null))")
  @Mapping(target = "notes", expression = "java(photo.getNotes().orElse(null))")
  @Mapping(target = "identifier", expression = "java(photo.getIdentifier().orElse(null))")
  PhotosRecord toRecord(Photo photo);

  List<Photo> toDomainList(List<PhotosRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
