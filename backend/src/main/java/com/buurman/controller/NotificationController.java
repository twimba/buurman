package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.NotificationResponse;
import com.buurman.dto.response.NotificationStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.generated.api.NotificationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationCenterService;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationsApi {

  private final NotificationCenterService centerService;

  @SuppressWarnings("unchecked")
  @Override
  public PageResponse getNotifications(
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
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    PaginatedResult<NotificationResponse> result =
        centerService.getNotifications(
            principal,
            type.orElse(null),
            channel.orElse(null),
            status.orElse(null),
            recipientEmail.orElse(null),
            dateFrom.orElse(null),
            dateTo.orElse(null),
            pageRequest);

    return PageResponse.of(
        result.items(), pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Override
  public NotificationResponse getNotification(NotificationIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.getNotification(principal, identifier);
  }

  @Override
  public NotificationStatsResponse getStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.getStats(principal);
  }

  @Override
  public NotificationResponse resendNotification(NotificationIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.resendNotification(principal, identifier);
  }

  @Override
  public NotificationResponse refreshStatus(NotificationIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.refreshNotificationStatus(principal, identifier);
  }
}
