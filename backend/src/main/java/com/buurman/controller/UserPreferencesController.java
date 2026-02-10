package com.buurman.controller;

import com.buurman.dto.request.UpdateNotificationTypePreferencesRequest;
import com.buurman.dto.request.UpdateUserPreferencesRequest;
import com.buurman.dto.response.NotificationTypePreferencesResponse;
import com.buurman.dto.response.UserPreferencesResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/preferences")
@Tag(name = "User Preferences", description = "User preferences and notification channel settings")
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

    @Operation(summary = "Get notification type preferences",
               description = "Get per-type notification channel preferences with global toggles")
    @GetMapping("/notifications")
    public NotificationTypePreferencesResponse getNotificationTypePreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return preferencesService.getNotificationTypePreferences(principal);
    }

    @Operation(summary = "Update notification type preferences",
               description = "Update per-type notification channel preferences")
    @PutMapping("/notifications")
    public NotificationTypePreferencesResponse updateNotificationTypePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateNotificationTypePreferencesRequest request) {
        return preferencesService.updateNotificationTypePreferences(principal, request);
    }
}
