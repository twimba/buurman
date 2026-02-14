package com.buurman.controller;

import com.buurman.domain.TeamSettings;
import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.TransferOwnershipRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/teams")
@Tag(name = "Teams", description = "Team management")
@SecurityRequirement(name = "bearer-jwt")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Get current team", description = "Get current user's team details")
    @GetMapping("/current")
    public TeamResponse getCurrentTeam(@AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getCurrentTeam(principal);
    }

    @Operation(summary = "List team members", description = "Get all members of the team")
    @GetMapping("/{teamIdentifier}/members")
    public List<TeamMemberResponse> getTeamMembers(@PathVariable String teamIdentifier,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getTeamMembers(teamIdentifier, principal);
    }

    @Operation(summary = "Create invitation", description = "Invite new member to team (Admin only)")
    @PostMapping("/{teamIdentifier}/invitations")
    @ResponseStatus(CREATED)
    public InvitationResponse createInvitation(@PathVariable String teamIdentifier,
                                               @Valid @RequestBody CreateInvitationRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.createInvitation(teamIdentifier, request, principal);
    }

    @Operation(summary = "Get pending invitations", description = "Get all pending invitations for the team (Admin only)")
    @GetMapping("/{teamIdentifier}/invitations/pending")
    public List<InvitationResponse> getTeamPendingInvitations(@PathVariable String teamIdentifier,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getTeamPendingInvitations(teamIdentifier, principal);
    }

    @Operation(summary = "Resend invitation", description = "Resend an invitation email with a new token (Admin only)")
    @PostMapping("/{teamIdentifier}/invitations/{token}/resend")
    public InvitationResponse resendInvitation(@PathVariable String teamIdentifier,
                                               @PathVariable String token,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.resendInvitation(teamIdentifier, token, principal);
    }

    @Operation(summary = "Remove team member", description = "Remove member from team (Admin only)")
    @DeleteMapping("/{teamIdentifier}/members/{userIdentifier}")
    @ResponseStatus(NO_CONTENT)
    public void removeMember(@PathVariable String teamIdentifier,
                            @PathVariable String userIdentifier,
                            @AuthenticationPrincipal UserPrincipal principal) {
        teamService.removeMember(teamIdentifier, userIdentifier, principal);
    }

    @Operation(summary = "Update member role", description = "Change member's role (Admin only)")
    @PutMapping("/{teamIdentifier}/members/{userIdentifier}/role")
    public TeamMemberResponse updateMemberRole(@PathVariable String teamIdentifier,
                                              @PathVariable String userIdentifier,
                                              @Valid @RequestBody UpdateMemberRoleRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.updateMemberRole(teamIdentifier, userIdentifier, request, principal);
    }

    @Operation(summary = "Update team", description = "Update team name (Admin only)")
    @PutMapping("/{teamIdentifier}")
    public TeamResponse updateTeam(@PathVariable String teamIdentifier,
                                   @Valid @RequestBody UpdateTeamRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.updateTeam(teamIdentifier, request, principal);
    }

    @Operation(summary = "Update team settings", description = "Update team configuration settings (Admin only)")
    @PutMapping("/{teamIdentifier}/settings")
    public TeamResponse updateTeamSettings(@PathVariable String teamIdentifier,
                                          @Valid @RequestBody UpdateTeamSettingsRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.updateTeamSettings(teamIdentifier, request, principal);
    }

    @Operation(summary = "Get team settings", description = "Get team configuration settings")
    @GetMapping("/{teamIdentifier}/settings")
    public TeamSettings getTeamSettings(@PathVariable String teamIdentifier,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getTeamSettings(teamIdentifier, principal);
    }

    @Operation(summary = "Transfer ownership", description = "Transfer team ownership to another member (Owner only)")
    @PostMapping("/{teamIdentifier}/transfer-ownership")
    public TeamMemberResponse transferOwnership(@PathVariable String teamIdentifier,
                                                @Valid @RequestBody TransferOwnershipRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.transferOwnership(teamIdentifier, request.newOwnerIdentifier(), principal);
    }
}
