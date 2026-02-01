package com.buurman.controller;

import com.buurman.dto.response.InvitationResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@Tag(name = "Invitations", description = "Team invitation management")
public class InvitationController {

    private final TeamService teamService;

    public InvitationController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "View invitation", description = "Get invitation details (public)")
    @GetMapping("/{token}")
    public InvitationResponse getInvitation(@PathVariable String token) {
        return teamService.getInvitation(token);
    }

    @Operation(summary = "Accept invitation", description = "Join team via invitation",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{token}/accept")
    @ResponseStatus(HttpStatus.OK)
    public void acceptInvitation(@PathVariable String token,
                                @AuthenticationPrincipal UserPrincipal principal) {
        teamService.acceptInvitation(token, principal);
    }
}
