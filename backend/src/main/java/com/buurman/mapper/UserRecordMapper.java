package com.buurman.mapper;

import com.buurman.domain.User;
import com.buurman.jooq.generated.tables.records.UsersRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for converting between JOOQ UsersRecord and User domain object.
 */
@Mapper(componentModel = "spring")
public interface UserRecordMapper {

    /**
     * Convert JOOQ UsersRecord to User domain object.
     */
    @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
    User toDomain(UsersRecord record);

    /**
     * Convert User domain object to JOOQ UsersRecord.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(user.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(user.getUpdatedAt()))")
    UsersRecord toRecord(User user);

    /**
     * Convert list of JOOQ records to list of domain objects.
     */
    List<User> toDomainList(List<UsersRecord> records);

    /**
     * Convert LocalDateTime to Instant.
     */
    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Convert Instant to LocalDateTime.
     */
    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
