package com.buurman.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.NotificationType;
import com.buurman.domain.UserNotificationTypePreference;
import com.buurman.domain.UserPreferences;
import com.buurman.dto.request.UpdateNotificationTypePreferencesRequest;
import com.buurman.dto.request.UpdateUserPreferencesRequest;
import com.buurman.dto.response.NotificationTypePreferencesResponse;
import com.buurman.dto.response.UserPreferencesResponse;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.FeatureFlags;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

  private final UserPreferencesRepository preferencesRepository;
  private final UserNotificationTypePreferenceRepository notifTypePrefRepository;
  private final FeatureFlagService featureFlagService;

  public UserPreferencesResponse getPreferences(UserPrincipal principal) {
    UserPreferences prefs =
        preferencesRepository.findByUserId(principal.getUserId()).orElseGet(UserPreferences::new);
    return toResponse(prefs);
  }

  @Transactional
  public UserPreferencesResponse updatePreferences(
      UserPrincipal principal, UpdateUserPreferencesRequest request) {
    UserPreferences prefs =
        preferencesRepository
            .findByUserId(principal.getUserId())
            .orElseGet(
                () -> {
                  UserPreferences newPrefs = new UserPreferences();
                  newPrefs.setUserId(principal.getUserId());
                  return newPrefs;
                });

    if (request.theme() != null) {
      prefs.setTheme(request.theme());
    }
    if (request.language() != null) {
      prefs.setLanguage(request.language());
    }
    if (request.timezone() != null) {
      prefs.setTimezone(request.timezone());
    }
    if (request.dateFormat() != null) {
      prefs.setDateFormat(request.dateFormat());
    }
    if (request.currencyFormat() != null) {
      prefs.setCurrencyFormat(request.currencyFormat());
    }
    if (request.emailNotifications() != null) {
      if (request.emailNotifications()
          && !featureFlagService.isEnabled(FeatureFlags.EMAIL_NOTIFICATIONS, principal)) {
        prefs.setEmailNotifications(false);
      } else {
        prefs.setEmailNotifications(request.emailNotifications());
      }
    }
    if (request.smsNotifications() != null) {
      if (request.smsNotifications()
          && !featureFlagService.isEnabled(FeatureFlags.SMS_NOTIFICATIONS, principal)) {
        prefs.setSmsNotifications(false);
      } else {
        prefs.setSmsNotifications(request.smsNotifications());
      }
    }

    prefs = preferencesRepository.save(prefs);
    return toResponse(prefs);
  }

  public NotificationTypePreferencesResponse getNotificationTypePreferences(
      UserPrincipal principal) {
    UserPreferences globalPrefs =
        preferencesRepository.findByUserId(principal.getUserId()).orElseGet(UserPreferences::new);

    boolean smsAvailable = featureFlagService.isEnabled(FeatureFlags.SMS_NOTIFICATIONS, principal);
    boolean emailAvailable =
        featureFlagService.isEnabled(FeatureFlags.EMAIL_NOTIFICATIONS, principal);

    List<UserNotificationTypePreference> saved =
        notifTypePrefRepository.findByUserId(principal.getUserId());
    Map<NotificationType, UserNotificationTypePreference> savedMap =
        saved.stream()
            .collect(toMap(UserNotificationTypePreference::getNotificationType, identity()));

    List<NotificationTypePreferencesResponse.Entry> entries = new ArrayList<>();
    for (NotificationType type : NotificationType.configurableTypes()) {
      UserNotificationTypePreference pref = savedMap.get(type);
      entries.add(
          new NotificationTypePreferencesResponse.Entry(
              type.name(),
              type.getDisplayName(),
              pref != null ? pref.isEmailEnabled() : true,
              pref != null ? pref.isSmsEnabled() : false));
    }

    return new NotificationTypePreferencesResponse(
        globalPrefs.isEmailNotifications(),
        globalPrefs.isSmsNotifications(),
        smsAvailable,
        emailAvailable,
        entries);
  }

  @Transactional
  public NotificationTypePreferencesResponse updateNotificationTypePreferences(
      UserPrincipal principal, UpdateNotificationTypePreferencesRequest request) {

    boolean smsAvailable = featureFlagService.isEnabled(FeatureFlags.SMS_NOTIFICATIONS, principal);
    boolean emailAvailable =
        featureFlagService.isEnabled(FeatureFlags.EMAIL_NOTIFICATIONS, principal);

    List<UserNotificationTypePreference> prefs = new ArrayList<>();
    for (UpdateNotificationTypePreferencesRequest.Entry entry : request.preferences()) {
      NotificationType type = NotificationType.valueOf(entry.notificationType());
      if (!type.isConfigurable()) {
        throw new IllegalArgumentException("Cannot configure system notification type: " + type);
      }
      UserNotificationTypePreference pref = new UserNotificationTypePreference();
      pref.setUserId(principal.getUserId());
      pref.setNotificationType(type);
      pref.setEmailEnabled(emailAvailable && entry.emailEnabled());
      pref.setSmsEnabled(smsAvailable && entry.smsEnabled());
      prefs.add(pref);
    }

    notifTypePrefRepository.saveAll(principal.getUserId(), prefs);
    return getNotificationTypePreferences(principal);
  }

  private UserPreferencesResponse toResponse(UserPreferences prefs) {
    return new UserPreferencesResponse(
        prefs.getTheme(),
        prefs.getLanguage(),
        prefs.getTimezone(),
        prefs.getDateFormat(),
        prefs.getCurrencyFormat(),
        prefs.isEmailNotifications(),
        prefs.isSmsNotifications());
  }
}
