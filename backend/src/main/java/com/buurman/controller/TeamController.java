package com.buurman.controller;

import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
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
    @GetMapping("/{teamId}/members")
    public List<TeamMemberResponse> getTeamMembers(@PathVariable UUID teamId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getTeamMembers(teamId, principal);
    }

    @Operation(summary = "Create invitation", description = "Invite new member to team (Admin only)")
    @PostMapping("/{teamId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public InvitationResponse createInvitation(@PathVariable UUID teamId,
                                               @Valid @RequestBody CreateInvitationRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.createInvitation(teamId, request, principal);
    }

    @Operation(summary = "Remove team member", description = "Remove member from team (Admin only)")
    @DeleteMapping("/{teamId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void removeMember(@PathVariable UUID teamId,
                            @PathVariable UUID memberId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        teamService.removeMember(teamId, memberId, principal);
    }

    @Operation(summary = "Update member role", description = "Change member's role (Admin only)")
    @PutMapping("/{teamId}/members/{memberId}/role")
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public TeamMemberResponse updateMemberRole(@PathVariable UUID teamId,
                                              @PathVariable UUID memberId,
                                              @Valid @RequestBody UpdateMemberRoleRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return teamService.updateMemberRole(teamId, memberId, request, principal);
    }
}
