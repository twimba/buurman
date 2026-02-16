package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Photo;
import com.buurman.jooq.generated.tables.records.PhotosRecord;

@Mapper(componentModel = "spring")
public interface PhotoRecordMapper {

  @Mapping(target = "uploadedAt", expression = "java(toInstant(record.getUploadedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toInstant(record.getDeletedAt()))")
  Photo toDomain(PhotosRecord record);

  @Mapping(target = "uploadedAt", expression = "java(toLocalDateTime(photo.getUploadedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toLocalDateTime(photo.getDeletedAt()))")
  PhotosRecord toRecord(Photo photo);

  List<Photo> toDomainList(List<PhotosRecord> records);

  default Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default LocalDateTime toLocalDateTime(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
