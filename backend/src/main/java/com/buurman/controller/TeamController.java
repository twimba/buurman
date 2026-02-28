package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.TransferOwnershipRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamPreferencesResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/teams")
@Tag(name = "Teams", description = "Team management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  @Operation(summary = "Get current team", description = "Get current user's team details")
  @GetMapping("/current")
  public TeamResponse getCurrentTeam(@AuthenticationPrincipal UserPrincipal principal) {
    return teamService.getCurrentTeam(principal);
  }

  @Operation(summary = "List team members", description = "Get all members of the team")
  @GetMapping("/{teamIdentifier}/members")
  public List<TeamMemberResponse> getTeamMembers(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.getTeamMembers(teamIdentifier, principal);
  }

  @Operation(summary = "Create invitation", description = "Invite new member to team (Admin only)")
  @PostMapping("/{teamIdentifier}/invitations")
  @ResponseStatus(CREATED)
  public InvitationResponse createInvitation(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Valid @RequestBody CreateInvitationRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.createInvitation(teamIdentifier, request, principal);
  }

  @Operation(
      summary = "Get pending invitations",
      description = "Get all pending invitations for the team (Admin only)")
  @GetMapping("/{teamIdentifier}/invitations/pending")
  public List<InvitationResponse> getTeamPendingInvitations(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.getTeamPendingInvitations(teamIdentifier, principal);
  }

  @Operation(
      summary = "Resend invitation",
      description = "Resend an invitation email with a new token (Admin only)")
  @PostMapping("/{teamIdentifier}/invitations/{token}/resend")
  public InvitationResponse resendInvitation(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Parameter(description = "Invitation token") @PathVariable String token,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.resendInvitation(teamIdentifier, token, principal);
  }

  @Operation(summary = "Remove team member", description = "Remove member from team (Admin only)")
  @DeleteMapping("/{teamIdentifier}/members/{userIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void removeMember(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Parameter(description = "User ULID identifier") @PathVariable String userIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    teamService.removeMember(teamIdentifier, userIdentifier, principal);
  }

  @Operation(summary = "Update member role", description = "Change member's role (Admin only)")
  @PutMapping("/{teamIdentifier}/members/{userIdentifier}/role")
  public TeamMemberResponse updateMemberRole(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Parameter(description = "User ULID identifier") @PathVariable String userIdentifier,
      @Valid @RequestBody UpdateMemberRoleRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.updateMemberRole(teamIdentifier, userIdentifier, request, principal);
  }

  @Operation(summary = "Update team", description = "Update team name (Admin only)")
  @PutMapping("/{teamIdentifier}")
  public TeamResponse updateTeam(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Valid @RequestBody UpdateTeamRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.updateTeam(teamIdentifier, request, principal);
  }

  @Operation(
      summary = "Update team settings",
      description = "Update team configuration settings (Admin only)")
  @PutMapping("/{teamIdentifier}/settings")
  public TeamPreferencesResponse updateTeamSettings(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Valid @RequestBody UpdateTeamSettingsRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.updateTeamPreferences(teamIdentifier, request, principal);
  }

  @Operation(summary = "Get team settings", description = "Get team configuration settings")
  @GetMapping("/{teamIdentifier}/settings")
  public TeamPreferencesResponse getTeamSettings(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.getTeamPreferences(teamIdentifier, principal);
  }

  @Operation(
      summary = "Transfer ownership",
      description = "Transfer team ownership to another member (Owner only)")
  @PostMapping("/{teamIdentifier}/transfer-ownership")
  public TeamMemberResponse transferOwnership(
      @Parameter(description = "Team ULID identifier") @PathVariable String teamIdentifier,
      @Valid @RequestBody TransferOwnershipRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return teamService.transferOwnership(teamIdentifier, request.newOwnerIdentifier(), principal);
  }
}
