package com.buurman.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface TeamMapper {

  @Mapping(target = "identifier", source = "team.identifier")
  @Mapping(target = "teamName", source = "team.name")
  TeamResponse toResponse(Team team, long memberCount);

  @Mapping(target = "userIdentifier", source = "user.identifier")
  @Mapping(target = "email", source = "user.email")
  @Mapping(target = "name", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
  @Mapping(target = "isOwner", source = "member.owner")
  @Mapping(target = "isCurrentUser", expression = "java(member.getUserId().equals(currentUserId))")
  TeamMemberResponse toMemberResponse(TeamMember member, User user, UUID currentUserId);

  @Mapping(target = "teamIdentifier", source = "teamIdentifier")
  @Mapping(target = "teamName", source = "teamName")
  @Mapping(target = "inviterName", source = "inviterName")
  @Mapping(target = "invitationUrl", expression = "java(invitationBaseUrl + invitation.getToken())")
  @Mapping(
      target = "isExpired",
      expression = "java(invitation.getExpiresAt().isBefore(java.time.Instant.now()))")
  @Mapping(target = "isAccepted", expression = "java(invitation.getAcceptedAt().isPresent())")
  InvitationResponse toInvitationResponse(
      TeamInvitation invitation,
      String teamIdentifier,
      String teamName,
      String inviterName,
      String invitationBaseUrl);
}
