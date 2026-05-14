package com.buurman.service;

import static com.buurman.util.FeatureFlags.EMAIL_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.SMS_NOTIFICATIONS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

  private final UserPreferencesRepository preferencesRepository;
  private final UserNotificationTypePreferenceRepository notifTypePrefRepository;
  private final UserRepository userRepository;
  private final FeatureFlagService featureFlagService;

  public UserPreferencesResponse getPreferences(UserPrincipal principal) {
    UserPreferences prefs =
        preferencesRepository.findByUserId(principal.getUserId()).orElseGet(UserPreferences::new);
    return toResponse(prefs);
  }

  @Transactional
  public UserPreferencesResponse updatePreferences(
      UserPrincipal principal, UpdateUserPreferencesRequest request) {
    final UserPreferences prefs =
        preferencesRepository
            .findByUserId(principal.getUserId())
            .orElseGet(
                () -> {
                  UserPreferences newPrefs = new UserPreferences();
                  newPrefs.setUserId(principal.getUserId());
                  return newPrefs;
                });

    request.theme().ifPresent(prefs::setTheme);
    request.language().ifPresent(prefs::setLanguage);
    request.timezone().ifPresent(prefs::setTimezone);
    request.dateFormat().ifPresent(prefs::setDateFormat);
    request.currencyFormat().ifPresent(cf -> prefs.setCurrencyFormat(Optional.of(cf)));
    request
        .emailNotifications()
        .ifPresent(
            enabled -> {
              if (enabled && !featureFlagService.isEnabled(EMAIL_NOTIFICATIONS, principal)) {
                prefs.setEmailNotifications(false);
              } else {
                prefs.setEmailNotifications(enabled);
              }
            });
    request
        .smsNotifications()
        .ifPresent(
            enabled -> {
              boolean hasVerifiedPhone =
                  userRepository
                      .getById(principal.getUserId())
                      .getPhoneVerifiedAt()
                      .isPresent();
              if (enabled
                  && (!featureFlagService.isEnabled(SMS_NOTIFICATIONS, principal)
                      || !hasVerifiedPhone)) {
                prefs.setSmsNotifications(false);
              } else {
                prefs.setSmsNotifications(enabled);
              }
            });

    UserPreferences savedPrefs = preferencesRepository.save(prefs);
    return toResponse(savedPrefs);
  }

  public NotificationTypePreferencesResponse getNotificationTypePreferences(
      UserPrincipal principal) {
    UserPreferences globalPrefs =
        preferencesRepository.findByUserId(principal.getUserId()).orElseGet(UserPreferences::new);

    boolean smsAvailable =
        featureFlagService.isEnabled(SMS_NOTIFICATIONS, principal)
            && userRepository.getById(principal.getUserId()).getPhoneVerifiedAt().isPresent();
    boolean emailAvailable = featureFlagService.isEnabled(EMAIL_NOTIFICATIONS, principal);

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
              pref == null || pref.isEmailEnabled(),
              pref != null && pref.isSmsEnabled()));
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

    boolean smsAvailable =
        featureFlagService.isEnabled(SMS_NOTIFICATIONS, principal)
            && userRepository.getById(principal.getUserId()).getPhoneVerifiedAt().isPresent();
    boolean emailAvailable = featureFlagService.isEnabled(EMAIL_NOTIFICATIONS, principal);

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
        Optional.of(prefs.getTheme()),
        Optional.of(prefs.getLanguage()),
        Optional.of(prefs.getTimezone()),
        Optional.of(prefs.getDateFormat()),
        prefs.getCurrencyFormat(),
        prefs.isEmailNotifications(),
        prefs.isSmsNotifications());
  }
}
