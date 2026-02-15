package com.buurman.mapper;

import com.buurman.domain.TeamInvitation;
import com.buurman.jooq.generated.tables.records.TeamInvitationsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;

@Mapper(componentModel = "spring")
public interface TeamInvitationRecordMapper {

    @Mapping(target = "expiresAt", expression = "java(toInstant(record.getExpiresAt()))")
    @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
    @Mapping(target = "acceptedAt", expression = "java(toInstant(record.getAcceptedAt()))")
    TeamInvitation toDomain(TeamInvitationsRecord record);

    @Mapping(target = "expiresAt", expression = "java(toLocalDateTime(invitation.getExpiresAt()))")
    @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(invitation.getInvitedAt()))")
    @Mapping(target = "acceptedAt", expression = "java(toLocalDateTime(invitation.getAcceptedAt()))")
    @Mapping(target = "deletedAt", ignore = true)
    TeamInvitationsRecord toRecord(TeamInvitation invitation);

    List<TeamInvitation> toDomainList(List<TeamInvitationsRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
    }
}
