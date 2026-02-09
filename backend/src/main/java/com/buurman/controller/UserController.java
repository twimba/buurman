package com.buurman.controller;

import com.buurman.dto.request.SetDefaultTeamRequest;
import com.buurman.dto.request.SwitchTeamRequest;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.UserTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User team management")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserTeamService userTeamService;

    public UserController(UserTeamService userTeamService) {
        this.userTeamService = userTeamService;
    }

    @Operation(summary = "Get current user profile", description = "Get the current user's profile information")
    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return userTeamService.getCurrentUserProfile(principal);
    }

    @Operation(summary = "Update user profile", description = "Update the current user's profile information")
    @PutMapping("/me")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        return userTeamService.updateUserProfile(request, principal);
    }

    @Operation(summary = "Get user's teams", description = "List all teams the current user belongs to")
    @GetMapping("/me/teams")
    public List<UserTeamResponse> getUserTeams(@AuthenticationPrincipal UserPrincipal principal) {
        return userTeamService.getUserTeams(principal);
    }

    @Operation(summary = "Switch active team", description = "Change the user's currently active team")
    @PostMapping("/switch-team")
    public UserTeamResponse switchTeam(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody SwitchTeamRequest request) {
        return userTeamService.switchTeam(request.teamIdentifier(), principal);
    }

    @Operation(summary = "Set default team", description = "Set the user's default team for login")
    @PutMapping("/default-team")
    public UserTeamResponse setDefaultTeam(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody SetDefaultTeamRequest request) {
        return userTeamService.setDefaultTeam(request.teamIdentifier(), principal);
    }

    @Operation(summary = "Leave team", description = "Leave a team (cannot leave owned teams)")
    @PostMapping("/teams/{teamIdentifier}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveTeam(@AuthenticationPrincipal UserPrincipal principal,
                          @PathVariable String teamIdentifier) {
        userTeamService.leaveTeam(teamIdentifier, principal);
    }
}
