package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.User;
import com.buurman.jooq.generated.tables.records.UsersRecord;

@Mapper(componentModel = "spring")
public interface UserRecordMapper {

  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "emailVerifiedAt", expression = "java(toInstant(record.getEmailVerifiedAt()))")
  @Mapping(target = "phoneVerifiedAt", expression = "java(toInstant(record.getPhoneVerifiedAt()))")
  @Mapping(target = "disabledAt", expression = "java(toInstant(record.getDisabledAt()))")
  User toDomain(UsersRecord record);

  @Mapping(target = "createdAt", expression = "java(toLocalDateTime(user.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(user.getUpdatedAt()))")
  @Mapping(
      target = "emailVerifiedAt",
      expression = "java(toLocalDateTime(user.getEmailVerifiedAt()))")
  @Mapping(
      target = "phoneVerifiedAt",
      expression = "java(toLocalDateTime(user.getPhoneVerifiedAt()))")
  @Mapping(target = "disabledAt", expression = "java(toLocalDateTime(user.getDisabledAt()))")
  @Mapping(target = "deletedAt", ignore = true)
  UsersRecord toRecord(User user);

  List<User> toDomainList(List<UsersRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
