package com.buurman.controller.backoffice;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeNotificationResponse;
import com.buurman.generated.backoffice.api.BackofficeNotificationsApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeNotificationController implements BackofficeNotificationsApi {

  private final BackofficeNotificationService backofficeNotificationService;

  @Override
  public PageResponse<BackofficeNotificationResponse> listNotifications(
      String teamIdentifier,
      String type,
      String channel,
      String status,
      String recipientEmail,
      String dateFrom,
      String dateTo,
      Integer page,
      Integer size,
      String sort,
      String direction) {

    LocalDateTime from = parseDateTime(dateFrom);
    LocalDateTime to = parseDateTime(dateTo);

    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return backofficeNotificationService.listNotifications(
        pageRequest, teamIdentifier != null ? TeamIdentifier.of(teamIdentifier) : null, type, channel, status, recipientEmail, from, to);
  }

  @Override
  public BackofficeNotificationResponse getNotification(String identifier) {
    return backofficeNotificationService.getNotification(NotificationIdentifier.of(identifier));
  }

  @Override
  public BackofficeNotificationResponse resendNotification(String identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeNotificationService.resendNotification(NotificationIdentifier.of(identifier), principal);
  }

  @Override
  public Map<String, Object> getStats() {
    return backofficeNotificationService.getStats();
  }

  private @Nullable LocalDateTime parseDateTime(@Nullable String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeStr, ISO_DATE_TIME);
    } catch (Exception e) {
      try {
        return LocalDateTime.parse(dateTimeStr + "T00:00:00", ISO_DATE_TIME);
      } catch (Exception e2) {
        return null;
      }
    }
  }
}
