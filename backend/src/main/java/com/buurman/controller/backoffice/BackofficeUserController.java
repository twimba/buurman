package com.buurman.controller.backoffice;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/users")
@Tag(name = "Backoffice - Users", description = "Platform-wide user management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeUserController {

  private final BackofficeUserService backofficeUserService;

  @Operation(
      summary = "List users",
      description = "Get all users with optional search and pagination")
  @GetMapping
  public PageResponse<BackofficeUserResponse> listUsers(
      @RequestParam(required = false) @Nullable String search,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam(required = false) @Nullable String sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return backofficeUserService.listUsers(pageRequest, search);
  }

  @Operation(summary = "Get user", description = "Get user details by identifier")
  @GetMapping("/{identifier}")
  public BackofficeUserResponse getUser(@PathVariable String identifier) {
    return backofficeUserService.getUser(identifier);
  }

  @Operation(
      summary = "Disable user",
      description = "Disable a user account in both database and Keycloak")
  @PostMapping("/{identifier}/disable")
  @ResponseStatus(NO_CONTENT)
  public void disableUser(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    backofficeUserService.disableUser(identifier, principal);
  }

  @Operation(summary = "Enable user", description = "Re-enable a disabled user account")
  @PostMapping("/{identifier}/enable")
  @ResponseStatus(NO_CONTENT)
  public void enableUser(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    backofficeUserService.enableUser(identifier, principal);
  }

  @Operation(
      summary = "Reset password",
      description = "Send a password reset email to the user via Keycloak")
  @PostMapping("/{identifier}/reset-password")
  @ResponseStatus(NO_CONTENT)
  public void resetPassword(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    backofficeUserService.resetPassword(identifier, principal);
  }
}
