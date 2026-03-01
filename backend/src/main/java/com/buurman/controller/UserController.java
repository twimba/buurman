package com.buurman.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.PhoneNumberPolicy;
import com.buurman.dto.request.SetDefaultTeamRequest;
import com.buurman.dto.request.SwitchTeamRequest;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.request.VerifyPhoneRequest;
import com.buurman.dto.response.PhoneNumberPolicyResponse;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.generated.api.UsersApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PhoneNumberPolicyService;
import com.buurman.service.PhoneVerificationService;
import com.buurman.service.UserTeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

  private final UserTeamService userTeamService;
  private final PhoneVerificationService phoneVerificationService;
  private final PhoneNumberPolicyService phoneNumberPolicyService;

  @Override
  public UserProfileResponse getUserProfile() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return userTeamService.getCurrentUserProfile(principal);
  }

  @Override
  public UserProfileResponse updateUserProfile(
      @Valid UpdateUserProfileRequest updateUserProfileRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return userTeamService.updateUserProfile(updateUserProfileRequest, principal);
  }

  @Override
  public UserProfileResponse verifyPhone(@Valid VerifyPhoneRequest verifyPhoneRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return phoneVerificationService.verifyPhone(principal.getUserId(), verifyPhoneRequest.code());
  }

  @Override
  public void resendPhoneVerification() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    phoneVerificationService.resendVerificationCode(principal.getUserId());
  }

  @Override
  public UserProfileResponse cancelPhoneVerification() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return phoneVerificationService.cancelVerification(principal.getUserId());
  }

  @Override
  public PhoneNumberPolicyResponse getPhonePolicy() {
    PhoneNumberPolicy policy = phoneNumberPolicyService.getPolicy();
    Map<String, List<String>> matrix = policy.getPolicyMatrix().orElse(Map.of());
    return new PhoneNumberPolicyResponse(matrix);
  }

  @Override
  public List<UserTeamResponse> getUserTeams() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return userTeamService.getUserTeams(principal);
  }

  @Override
  public UserTeamResponse switchTeam(@Valid SwitchTeamRequest switchTeamRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return userTeamService.switchTeam(switchTeamRequest.teamIdentifier(), principal);
  }

  @Override
  public UserTeamResponse setDefaultTeam(@Valid SetDefaultTeamRequest setDefaultTeamRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return userTeamService.setDefaultTeam(setDefaultTeamRequest.teamIdentifier(), principal);
  }

  @Override
  public void leaveTeam(String teamIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    userTeamService.leaveTeam(teamIdentifier, principal);
  }
}
