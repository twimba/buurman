package com.buurman.controller;

import com.buurman.dto.request.UpdateTeamNotificationPreferencesRequest;
import com.buurman.dto.request.UpdateUserPreferencesRequest;
import com.buurman.dto.response.UserPreferencesResponse;
import com.buurman.dto.response.UserTeamNotificationPreferencesResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/preferences")
@Tag(name = "User Preferences", description = "User and team notification preferences")
@SecurityRequirement(name = "bearer-jwt")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    public UserPreferencesController(UserPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @Operation(summary = "Get user preferences", description = "Get the current user's global preferences")
    @GetMapping
    public UserPreferencesResponse getPreferences(@AuthenticationPrincipal UserPrincipal principal) {
        return preferencesService.getPreferences(principal);
    }

    @Operation(summary = "Update user preferences", description = "Update the current user's global preferences")
    @PatchMapping
    public UserPreferencesResponse updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return preferencesService.updatePreferences(principal, request);
    }

    @Operation(summary = "Get team notification preferences",
               description = "Get notification preferences for a specific team")
    @GetMapping("/teams/{teamId}")
    public UserTeamNotificationPreferencesResponse getTeamNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID teamId) {
        return preferencesService.getTeamNotificationPreferences(principal, teamId);
    }

    @Operation(summary = "Update team notification preferences",
               description = "Update notification preferences for a specific team")
    @PatchMapping("/teams/{teamId}")
    public UserTeamNotificationPreferencesResponse updateTeamNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateTeamNotificationPreferencesRequest request) {
        return preferencesService.updateTeamNotificationPreferences(principal, teamId, request);
    }
}
