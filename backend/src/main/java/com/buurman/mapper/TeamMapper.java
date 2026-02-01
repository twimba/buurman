package com.buurman.mapper;

import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    TeamResponse toResponse(Team team, long memberCount);

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "userId", source = "member.userId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "name", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "isCurrentUser", expression = "java(member.getUserId().equals(currentUserId))")
    TeamMemberResponse toMemberResponse(TeamMember member, User user, UUID currentUserId);

    @Mapping(target = "invitationId", source = "invitation.id")
    @Mapping(target = "teamId", source = "invitation.teamId")
    @Mapping(target = "invitationUrl", expression = "java(\"http://localhost:5173/invitations/\" + invitation.getToken())")
    InvitationResponse toInvitationResponse(TeamInvitation invitation);
}
