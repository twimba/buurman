package com.buurman.controller;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.PhoneNumberPolicy;
import com.buurman.dto.request.SetDefaultTeamRequest;
import com.buurman.dto.request.SwitchTeamRequest;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.request.VerifyPhoneRequest;
import com.buurman.dto.response.PhoneNumberPolicyResponse;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PhoneNumberPolicyService;
import com.buurman.service.PhoneVerificationService;
import com.buurman.service.UserTeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User team management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class UserController {

  private final UserTeamService userTeamService;
  private final PhoneVerificationService phoneVerificationService;
  private final PhoneNumberPolicyService phoneNumberPolicyService;

  @Operation(
      summary = "Get current user profile",
      description = "Get the current user's profile information")
  @GetMapping("/me")
  public UserProfileResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
    return userTeamService.getCurrentUserProfile(principal);
  }

  @Operation(
      summary = "Update user profile",
      description = "Update the current user's profile information")
  @PutMapping("/me")
  public UserProfileResponse updateProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateUserProfileRequest request) {
    return userTeamService.updateUserProfile(request, principal);
  }

  @Operation(summary = "Verify phone number", description = "Verify phone number with SMS code")
  @PostMapping("/me/phone/verify")
  public UserProfileResponse verifyPhone(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody VerifyPhoneRequest request) {
    return phoneVerificationService.verifyPhone(principal.getUserId(), request.code());
  }

  @Operation(
      summary = "Resend phone verification code",
      description = "Resend SMS verification code (rate limited)")
  @PostMapping("/me/phone/resend-verification")
  @ResponseStatus(NO_CONTENT)
  public void resendPhoneVerification(@AuthenticationPrincipal UserPrincipal principal) {
    phoneVerificationService.resendVerificationCode(principal.getUserId());
  }

  @Operation(
      summary = "Cancel phone verification",
      description = "Cancel pending phone verification to allow changing the number")
  @PostMapping("/me/phone/cancel-verification")
  public UserProfileResponse cancelPhoneVerification(
      @AuthenticationPrincipal UserPrincipal principal) {
    return phoneVerificationService.cancelVerification(principal.getUserId());
  }

  @Operation(
      summary = "Get phone number policy",
      description = "Get the current phone number policy for client-side validation")
  @GetMapping("/phone-policy")
  public PhoneNumberPolicyResponse getPhonePolicy() {
    PhoneNumberPolicy policy = phoneNumberPolicyService.getPolicy();
    Map<String, List<String>> matrix =
        policy.getPolicyMatrix() != null ? policy.getPolicyMatrix() : Map.of();
    return new PhoneNumberPolicyResponse(matrix);
  }

  @Operation(
      summary = "Get user's teams",
      description = "List all teams the current user belongs to")
  @GetMapping("/me/teams")
  public List<UserTeamResponse> getUserTeams(@AuthenticationPrincipal UserPrincipal principal) {
    return userTeamService.getUserTeams(principal);
  }

  @Operation(
      summary = "Switch active team",
      description = "Change the user's currently active team")
  @PostMapping("/switch-team")
  public UserTeamResponse switchTeam(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody SwitchTeamRequest request) {
    return userTeamService.switchTeam(request.teamIdentifier(), principal);
  }

  @Operation(summary = "Set default team", description = "Set the user's default team for login")
  @PutMapping("/default-team")
  public UserTeamResponse setDefaultTeam(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody SetDefaultTeamRequest request) {
    return userTeamService.setDefaultTeam(request.teamIdentifier(), principal);
  }

  @Operation(summary = "Leave team", description = "Leave a team (cannot leave owned teams)")
  @PostMapping("/teams/{teamIdentifier}/leave")
  @ResponseStatus(NO_CONTENT)
  public void leaveTeam(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable String teamIdentifier) {
    userTeamService.leaveTeam(teamIdentifier, principal);
  }
}
