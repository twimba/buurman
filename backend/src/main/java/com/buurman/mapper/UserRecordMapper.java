package com.buurman.mapper;

import com.buurman.domain.User;
import com.buurman.jooq.generated.tables.records.UsersRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserRecordMapper {

    @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
    User toDomain(UsersRecord record);

    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(user.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(user.getUpdatedAt()))")
    @Mapping(target = "deletedAt", ignore = true)
    UsersRecord toRecord(User user);

    List<User> toDomainList(List<UsersRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
