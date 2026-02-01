package com.buurman.mapper;

import com.buurman.domain.TeamInvitation;
import com.buurman.jooq.generated.tables.records.TeamInvitationsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for converting between JOOQ TeamInvitationsRecord and TeamInvitation domain object.
 */
@Mapper(componentModel = "spring")
public interface TeamInvitationRecordMapper {

    /**
     * Convert JOOQ TeamInvitationsRecord to TeamInvitation domain object.
     */
    @Mapping(target = "expiresAt", expression = "java(toInstant(record.getExpiresAt()))")
    @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
    @Mapping(target = "acceptedAt", expression = "java(toInstant(record.getAcceptedAt()))")
    TeamInvitation toDomain(TeamInvitationsRecord record);

    /**
     * Convert TeamInvitation domain object to JOOQ TeamInvitationsRecord.
     */
    @Mapping(target = "expiresAt", expression = "java(toLocalDateTime(invitation.getExpiresAt()))")
    @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(invitation.getInvitedAt()))")
    @Mapping(target = "acceptedAt", expression = "java(toLocalDateTime(invitation.getAcceptedAt()))")
    TeamInvitationsRecord toRecord(TeamInvitation invitation);

    /**
     * Convert list of JOOQ records to list of domain objects.
     */
    List<TeamInvitation> toDomainList(List<TeamInvitationsRecord> records);

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
