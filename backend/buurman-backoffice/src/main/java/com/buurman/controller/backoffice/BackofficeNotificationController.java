package com.buurman.controller.backoffice;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
      Optional<String> teamIdentifier,
      Optional<String> type,
      Optional<String> channel,
      Optional<String> status,
      Optional<String> recipientEmail,
      Optional<String> dateFrom,
      Optional<String> dateTo,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {

    LocalDateTime from = parseDateTime(dateFrom.orElse(null));
    LocalDateTime to = parseDateTime(dateTo.orElse(null));

    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return backofficeNotificationService.listNotifications(
        pageRequest,
        teamIdentifier.map(TeamIdentifier::of).orElse(null),
        type.orElse(null),
        channel.orElse(null),
        status.orElse(null),
        recipientEmail.orElse(null),
        from,
        to);
  }

  @Override
  public BackofficeNotificationResponse getNotification(NotificationIdentifier identifier) {
    return backofficeNotificationService.getNotification(identifier);
  }

  @Override
  public BackofficeNotificationResponse resendNotification(NotificationIdentifier identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeNotificationService.resendNotification(identifier, principal);
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
