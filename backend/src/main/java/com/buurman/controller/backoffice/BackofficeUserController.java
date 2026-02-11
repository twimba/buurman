package com.buurman.controller.backoffice;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backoffice/users")
@Tag(name = "Backoffice - Users", description = "Platform-wide user management")
@SecurityRequirement(name = "bearer-jwt")
public class BackofficeUserController {

    private final BackofficeUserService backofficeUserService;

    public BackofficeUserController(BackofficeUserService backofficeUserService) {
        this.backofficeUserService = backofficeUserService;
    }

    @Operation(summary = "List users", description = "Get all users with optional search and pagination")
    @GetMapping
    public PageResponse<BackofficeUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        return backofficeUserService.listUsers(pageRequest, search);
    }

    @Operation(summary = "Get user", description = "Get user details by identifier")
    @GetMapping("/{identifier}")
    public BackofficeUserResponse getUser(@PathVariable String identifier) {
        return backofficeUserService.getUser(identifier);
    }

    @Operation(summary = "Disable user", description = "Disable a user account in both database and Keycloak")
    @PostMapping("/{identifier}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableUser(
            @PathVariable String identifier,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        backofficeUserService.disableUser(identifier, principal);
    }

    @Operation(summary = "Enable user", description = "Re-enable a disabled user account")
    @PostMapping("/{identifier}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableUser(
            @PathVariable String identifier,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        backofficeUserService.enableUser(identifier, principal);
    }

    @Operation(summary = "Reset password", description = "Send a password reset email to the user via Keycloak")
    @PostMapping("/{identifier}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable String identifier,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        backofficeUserService.resetPassword(identifier, principal);
    }
}
