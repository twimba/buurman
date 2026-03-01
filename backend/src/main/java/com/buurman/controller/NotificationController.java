package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

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
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    PaginatedResult<NotificationResponse> result =
        centerService.getNotifications(
            principal, type, channel, status, recipientEmail, dateFrom, dateTo, pageRequest);

    return PageResponse.of(result.items(), page, size, result.totalElements());
  }

  @Override
  public NotificationResponse getNotification(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.getNotification(principal, identifier);
  }

  @Override
  public NotificationStatsResponse getStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.getStats(principal);
  }

  @Override
  public NotificationResponse resendNotification(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.resendNotification(principal, identifier);
  }

  @Override
  public NotificationResponse refreshStatus(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return centerService.refreshNotificationStatus(principal, identifier);
  }
}
