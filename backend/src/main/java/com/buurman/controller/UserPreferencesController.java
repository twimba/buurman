package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.UpdateNotificationTypePreferencesRequest;
import com.buurman.dto.request.UpdateUserPreferencesRequest;
import com.buurman.dto.response.NotificationTypePreferencesResponse;
import com.buurman.dto.response.UserPreferencesResponse;
import com.buurman.generated.api.UserPreferencesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.UserPreferencesService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserPreferencesController implements UserPreferencesApi {

  private final UserPreferencesService preferencesService;

  @Override
  public UserPreferencesResponse getPreferences() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return preferencesService.getPreferences(principal);
  }

  @Override
  public UserPreferencesResponse updatePreferences(
      @Valid UpdateUserPreferencesRequest updateUserPreferencesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return preferencesService.updatePreferences(principal, updateUserPreferencesRequest);
  }

  @Override
  public NotificationTypePreferencesResponse getNotificationTypePreferences() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return preferencesService.getNotificationTypePreferences(principal);
  }

  @Override
  public NotificationTypePreferencesResponse updateNotificationTypePreferences(
      @Valid UpdateNotificationTypePreferencesRequest updateNotificationTypePreferencesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return preferencesService.updateNotificationTypePreferences(
        principal, updateNotificationTypePreferencesRequest);
  }
}
