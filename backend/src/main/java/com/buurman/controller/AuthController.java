package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.RegisterRequest;
import com.buurman.dto.request.UpdateProfileRequest;
import com.buurman.dto.request.VerifyEmailRequest;
import com.buurman.dto.response.UserResponse;
import com.buurman.generated.api.AuthenticationApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

  private final AuthService authService;

  @Override
  public UserResponse register(@Valid RegisterRequest registerRequest) {
    return authService.register(registerRequest);
  }

  @Override
  public UserResponse getCurrentUser() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return authService.getCurrentUser(principal.getUserId());
  }

  @Override
  public UserResponse updateProfile(@Valid UpdateProfileRequest updateProfileRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return authService.updateProfile(principal.getUserId(), updateProfileRequest);
  }

  @Override
  public UserResponse verifyEmail(@Valid VerifyEmailRequest verifyEmailRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return authService.verifyEmail(principal.getUserId(), verifyEmailRequest.code());
  }

  @Override
  public void resendVerification() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    authService.resendVerificationCode(principal.getUserId());
  }
}
