package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.NotificationResponse;
import com.buurman.dto.response.NotificationStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationCenterService;
import com.buurman.util.PaginationHelper.PaginatedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification center and management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationCenterService centerService;

  @Operation(
      summary = "List notifications",
      description = "Paginated list of all team notifications (Admin only)")
  @GetMapping
  public PageResponse<NotificationResponse> getNotifications(
      @Parameter(description = "Filter by notification type") @RequestParam
          Optional<NotificationType> type,
      @Parameter(description = "Filter by channel") @RequestParam
          Optional<NotificationChannel> channel,
      @Parameter(description = "Filter by status") @RequestParam
          Optional<NotificationStatus> status,
      @Parameter(description = "Filter by recipient email") @RequestParam
          Optional<String> recipientEmail,
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          Optional<String> dateFrom,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31") @RequestParam
          Optional<String> dateTo,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          int size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {

    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    PaginatedResult<NotificationResponse> result =
        centerService.getNotifications(
            principal,
            type.map(NotificationType::name).orElse(null),
            channel.map(NotificationChannel::name).orElse(null),
            status.map(NotificationStatus::name).orElse(null),
            recipientEmail.orElse(null),
            dateFrom.orElse(null),
            dateTo.orElse(null),
            pageRequest);

    return PageResponse.of(result.items(), page, size, result.totalElements());
  }

  @Operation(summary = "Get notification details")
  @GetMapping("/{identifier}")
  public NotificationResponse getNotification(
      @Parameter(description = "Notification ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return centerService.getNotification(principal, identifier);
  }

  @Operation(summary = "Get notification statistics")
  @GetMapping("/stats")
  public NotificationStatsResponse getStats(@AuthenticationPrincipal UserPrincipal principal) {
    return centerService.getStats(principal);
  }

  @Operation(
      summary = "Resend notification",
      description = "Resend a failed notification (Admin only)")
  @PostMapping("/{identifier}/resend")
  @ResponseStatus(CREATED)
  public NotificationResponse resendNotification(
      @Parameter(description = "Notification ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return centerService.resendNotification(principal, identifier);
  }

  @Operation(
      summary = "Refresh notification status",
      description = "Fetch latest delivery status from provider (Admin only)")
  @PostMapping("/{identifier}/refresh-status")
  public NotificationResponse refreshStatus(
      @Parameter(description = "Notification ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return centerService.refreshNotificationStatus(principal, identifier);
  }
}
