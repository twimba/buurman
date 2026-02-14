package com.buurman.controller;

import com.buurman.dto.response.InvitationResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/invitations")
@Tag(name = "Invitations", description = "Team invitation management")
public class InvitationController {

    private final TeamService teamService;

    public InvitationController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Get pending invitations", description = "Get all pending invitations for the authenticated user",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/pending")
    public List<InvitationResponse> getPendingInvitations(@AuthenticationPrincipal UserPrincipal principal) {
        return teamService.getPendingInvitationsForUser(principal);
    }

    @Operation(summary = "View invitation", description = "Get invitation details (public)")
    @GetMapping("/{token}")
    public InvitationResponse getInvitation(@PathVariable String token) {
        return teamService.getInvitation(token);
    }

    @Operation(summary = "Accept invitation", description = "Join team via invitation",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{token}/accept")
    @ResponseStatus(OK)
    public void acceptInvitation(@PathVariable String token,
                                @AuthenticationPrincipal UserPrincipal principal) {
        teamService.acceptInvitation(token, principal);
    }
}
