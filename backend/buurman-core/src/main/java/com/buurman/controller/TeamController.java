package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.TransferOwnershipRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamPreferencesResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.generated.api.TeamsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TeamController implements TeamsApi {

  private final TeamService teamService;

  @Override
  public TeamResponse getCurrentTeam() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.getCurrentTeam(principal);
  }

  @Override
  public List<TeamMemberResponse> getTeamMembers(TeamIdentifier teamIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.getTeamMembers(teamIdentifier, principal);
  }

  @Override
  public InvitationResponse createInvitation(
      TeamIdentifier teamIdentifier, CreateInvitationRequest createInvitationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.createInvitation(teamIdentifier, createInvitationRequest, principal);
  }

  @Override
  public List<InvitationResponse> getTeamPendingInvitations(TeamIdentifier teamIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.getTeamPendingInvitations(teamIdentifier, principal);
  }

  @Override
  public InvitationResponse resendInvitation(TeamIdentifier teamIdentifier, String token) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.resendInvitation(teamIdentifier, token, principal);
  }

  @Override
  public void removeMember(TeamIdentifier teamIdentifier, UserIdentifier userIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    teamService.removeMember(teamIdentifier, userIdentifier, principal);
  }

  @Override
  public TeamMemberResponse updateMemberRole(
      TeamIdentifier teamIdentifier,
      UserIdentifier userIdentifier,
      UpdateMemberRoleRequest updateMemberRoleRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.updateMemberRole(
        teamIdentifier, userIdentifier, updateMemberRoleRequest, principal);
  }

  @Override
  public TeamResponse updateTeam(
      TeamIdentifier teamIdentifier, UpdateTeamRequest updateTeamRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.updateTeam(teamIdentifier, updateTeamRequest, principal);
  }

  @Override
  public TeamPreferencesResponse updateTeamSettings(
      TeamIdentifier teamIdentifier, UpdateTeamSettingsRequest updateTeamSettingsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.updateTeamPreferences(teamIdentifier, updateTeamSettingsRequest, principal);
  }

  @Override
  public TeamPreferencesResponse getTeamSettings(TeamIdentifier teamIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.getTeamPreferences(teamIdentifier, principal);
  }

  @Override
  public TeamMemberResponse transferOwnership(
      TeamIdentifier teamIdentifier, TransferOwnershipRequest transferOwnershipRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.transferOwnership(
        teamIdentifier, transferOwnershipRequest.newOwnerIdentifier(), principal);
  }
}
