package com.buurman.service;

import com.buurman.domain.UserPreferences;
import com.buurman.domain.UserTeamNotificationPreferences;
import com.buurman.dto.request.UpdateTeamNotificationPreferencesRequest;
import com.buurman.dto.request.UpdateUserPreferencesRequest;
import com.buurman.dto.response.UserPreferencesResponse;
import com.buurman.dto.response.UserTeamNotificationPreferencesResponse;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserTeamNotificationPreferencesRepository;
import com.buurman.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserTeamNotificationPreferencesRepository teamNotifRepository;
    private final TeamPermissionService permissionService;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository,
                                  UserTeamNotificationPreferencesRepository teamNotifRepository,
                                  TeamPermissionService permissionService) {
        this.preferencesRepository = preferencesRepository;
        this.teamNotifRepository = teamNotifRepository;
        this.permissionService = permissionService;
    }

    public UserPreferencesResponse getPreferences(UserPrincipal principal) {
        UserPreferences prefs = preferencesRepository.findByUserId(principal.getUserId())
            .orElseGet(UserPreferences::new); // Return defaults if not found

        return toResponse(prefs);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UserPrincipal principal,
                                                     UpdateUserPreferencesRequest request) {
        UserPreferences prefs = preferencesRepository.findByUserId(principal.getUserId())
            .orElseGet(() -> {
                UserPreferences newPrefs = new UserPreferences();
                newPrefs.setUserId(principal.getUserId());
                return newPrefs;
            });

        // Update only non-null fields
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
            prefs.setEmailNotifications(request.emailNotifications());
        }
        if (request.inAppNotifications() != null) {
            prefs.setInAppNotifications(request.inAppNotifications());
        }

        prefs = preferencesRepository.save(prefs);
        return toResponse(prefs);
    }

    public UserTeamNotificationPreferencesResponse getTeamNotificationPreferences(
            UserPrincipal principal, UUID teamId) {
        permissionService.validateTeamAccess(principal, teamId);

        UserTeamNotificationPreferences prefs = teamNotifRepository
            .findByUserIdAndTeamId(principal.getUserId(), teamId)
            .orElseGet(UserTeamNotificationPreferences::new); // Return defaults if not found

        return toTeamNotifResponse(teamId, prefs);
    }

    @Transactional
    public UserTeamNotificationPreferencesResponse updateTeamNotificationPreferences(
            UserPrincipal principal, UUID teamId,
            UpdateTeamNotificationPreferencesRequest request) {
        permissionService.validateTeamAccess(principal, teamId);

        UserTeamNotificationPreferences prefs = teamNotifRepository
            .findByUserIdAndTeamId(principal.getUserId(), teamId)
            .orElseGet(() -> {
                UserTeamNotificationPreferences newPrefs = new UserTeamNotificationPreferences();
                newPrefs.setUserId(principal.getUserId());
                newPrefs.setTeamId(teamId);
                return newPrefs;
            });

        // Update only non-null fields
        if (request.paymentReminders() != null) {
            prefs.setPaymentReminders(request.paymentReminders());
        }
        if (request.contractExpiryAlerts() != null) {
            prefs.setContractExpiryAlerts(request.contractExpiryAlerts());
        }
        if (request.newMemberNotifications() != null) {
            prefs.setNewMemberNotifications(request.newMemberNotifications());
        }
        if (request.weeklySummary() != null) {
            prefs.setWeeklySummary(request.weeklySummary());
        }

        prefs = teamNotifRepository.save(prefs);
        return toTeamNotifResponse(teamId, prefs);
    }

    private UserPreferencesResponse toResponse(UserPreferences prefs) {
        return new UserPreferencesResponse(
            prefs.getTheme(),
            prefs.getLanguage(),
            prefs.getTimezone(),
            prefs.getDateFormat(),
            prefs.getCurrencyFormat(),
            prefs.isEmailNotifications(),
            prefs.isInAppNotifications()
        );
    }

    private UserTeamNotificationPreferencesResponse toTeamNotifResponse(
            UUID teamId, UserTeamNotificationPreferences prefs) {
        return new UserTeamNotificationPreferencesResponse(
            teamId,
            prefs.isPaymentReminders(),
            prefs.isContractExpiryAlerts(),
            prefs.isNewMemberNotifications(),
            prefs.isWeeklySummary()
        );
    }
}
